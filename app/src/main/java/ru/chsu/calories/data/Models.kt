package ru.chsu.calories.data

enum class Sex { MALE, FEMALE }

enum class Activity(val title: String, val hint: String, val factor: Double) {
    MINIMAL("минимальная", "сидячая работа", 1.2),
    LOW("низкая", "1–3 тренировки в неделю", 1.375),
    MEDIUM("средняя", "3–5 тренировок в неделю", 1.55),
    HIGH("высокая", "6–7 тренировок в неделю", 1.725),
    VERY_HIGH("очень высокая", "физическая работа + спорт", 1.9)
}

enum class Goal(val title: String, val factor: Double, val note: String) {
    LOSE("похудение", 0.85, "−15%"),
    KEEP("поддержание", 1.0, "без изменений"),
    GAIN("набор", 1.15, "+15%")
}

enum class Meal(val title: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SNACK("Перекус")
}

data class Profile(
    val sex: Sex = Sex.MALE,
    val age: Int = 20,
    val heightCm: Int = 180,
    val weightKg: Int = 75,
    val activity: Activity = Activity.MEDIUM,
    val goal: Goal = Goal.KEEP
)

/** Продукт из справочника. Все значения указаны на 100 г. */
data class Product(
    val id: String,
    val name: String,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val category: Int,
    val custom: Boolean = false
)

/** Запись в дневнике: какой продукт и сколько грамм. */
data class FoodEntry(val productId: String, val grams: Int)

data class DayLog(
    val meals: Map<Meal, List<FoodEntry>> = Meal.entries.associateWith { emptyList<FoodEntry>() },
    val waterGlasses: Int = 0
)

data class Nutrients(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0
) {
    operator fun plus(o: Nutrients) =
        Nutrients(kcal + o.kcal, protein + o.protein, fat + o.fat, carbs + o.carbs)

    companion object {
        /** Калорийность записи = калорийность на 100 г × масса / 100 */
        fun of(p: Product, grams: Int): Nutrients {
            val m = grams / 100.0
            return Nutrients(p.kcal * m, p.protein * m, p.fat * m, p.carbs * m)
        }
    }
}
