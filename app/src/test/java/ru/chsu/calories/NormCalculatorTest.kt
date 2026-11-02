package ru.chsu.calories

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.chsu.calories.data.Activity
import ru.chsu.calories.data.Goal
import ru.chsu.calories.data.Nutrients
import ru.chsu.calories.data.Product
import ru.chsu.calories.data.Profile
import ru.chsu.calories.data.Sex
import ru.chsu.calories.domain.NormCalculator

class NormCalculatorTest {

    @Test
    fun bmrForMan() {
        // 10*75 + 6.25*180 - 5*20 + 5 = 1780
        val p = Profile(Sex.MALE, 20, 180, 75, Activity.MEDIUM, Goal.KEEP)
        assertEquals(1780.0, NormCalculator.bmr(p), 0.001)
    }

    @Test
    fun bmrForWoman() {
        // 10*60 + 6.25*165 - 5*25 - 161 = 1345.25
        val p = Profile(Sex.FEMALE, 25, 165, 60, Activity.MINIMAL, Goal.KEEP)
        assertEquals(1345.25, NormCalculator.bmr(p), 0.001)
    }

    @Test
    fun normFromPresentationExample() {
        // пример с макета «Профиль»: муж., 20 лет, 180 см, 75 кг, средняя активность → 2760 ккал
        val p = Profile(Sex.MALE, 20, 180, 75, Activity.MEDIUM, Goal.KEEP)
        assertEquals(2760, NormCalculator.calculate(p).kcal)
    }

    @Test
    fun goalChangesNorm() {
        val base = Profile(Sex.MALE, 20, 180, 75, Activity.MEDIUM, Goal.KEEP)
        val lose = NormCalculator.calculate(base.copy(goal = Goal.LOSE)).kcal
        val gain = NormCalculator.calculate(base.copy(goal = Goal.GAIN)).kcal
        assertEquals(2350, lose)
        assertEquals(3170, gain)
    }

    @Test
    fun entryCalories() {
        // пример с макета «Добавление продукта»: гречка варёная 110 ккал/100 г, 150 г → 165 ккал
        val buckwheat = Product("p0", "Гречка варёная", 110.0, 4.2, 1.1, 21.3, 0)
        assertEquals(165.0, Nutrients.of(buckwheat, 150).kcal, 0.001)
    }
}
