package ru.chsu.calories.domain

import ru.chsu.calories.data.Profile
import ru.chsu.calories.data.Sex
import kotlin.math.roundToInt

data class Norm(
    val bmr: Double,
    val tdee: Double,
    val kcal: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int
)

/**
 * Расчёт суточной нормы по формуле Миффлина–Сан Жеора.
 * BMR = 10 × вес + 6,25 × рост − 5 × возраст + 5 (муж.) / − 161 (жен.)
 * Норма = BMR × коэффициент активности × коэффициент цели (округляем до 10 ккал).
 * БЖУ делим как 20% / 30% / 50% калорий (1 г белка и углеводов = 4 ккал, жира = 9 ккал).
 */
object NormCalculator {

    fun bmr(p: Profile): Double =
        10.0 * p.weightKg + 6.25 * p.heightCm - 5.0 * p.age + (if (p.sex == Sex.MALE) 5 else -161)

    fun calculate(p: Profile): Norm {
        val bmr = bmr(p)
        val tdee = bmr * p.activity.factor
        val kcal = (tdee * p.goal.factor / 10).roundToInt() * 10
        return Norm(
            bmr = bmr,
            tdee = tdee,
            kcal = kcal,
            protein = (kcal * 0.2 / 4).roundToInt(),
            fat = (kcal * 0.3 / 9).roundToInt(),
            carbs = (kcal * 0.5 / 4).roundToInt()
        )
    }
}
