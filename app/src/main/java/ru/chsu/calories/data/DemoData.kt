package ru.chsu.calories.data

import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Выдуманная история питания за последние 30 дней, чтобы экраны и статистика не были пустыми.
 * Seed фиксированный — при каждом запуске получаются одни и те же данные.
 */
object DemoData {

    private val templates: Map<Meal, List<List<Pair<String, Int>>>> = mapOf(
        Meal.BREAKFAST to listOf(
            listOf("p3" to 250, "p19" to 200, "p28" to 120),
            listOf("p16" to 110, "p7" to 40, "p38" to 200),
            listOf("p17" to 180, "p27" to 150)
        ),
        Meal.LUNCH to listOf(
            listOf("p0" to 200, "p10" to 150, "p23" to 100),
            listOf("p30" to 300, "p8" to 60, "p11" to 120),
            listOf("p31" to 220, "p33" to 150),
            listOf("p5" to 180, "p15" to 130, "p24" to 100)
        ),
        Meal.DINNER to listOf(
            listOf("p9" to 250, "p14" to 180, "p25" to 100),
            listOf("p6" to 200, "p13" to 120),
            listOf("p32" to 280, "p20" to 150)
        ),
        Meal.SNACK to listOf(
            listOf("p27" to 180),
            listOf("p34" to 40, "p39" to 250),
            listOf("p35" to 30, "p38" to 200),
            listOf("p20" to 150, "p28" to 120)
        )
    )

    fun history(today: LocalDate): Map<LocalDate, DayLog> {
        val rnd = Random(7)
        val result = mutableMapOf<LocalDate, DayLog>()
        for (i in 29 downTo 1) {
            val date = today.minusDays(i.toLong())
            val meals = Meal.entries.associateWith { meal ->
                if (meal == Meal.SNACK && rnd.nextDouble() < 0.35) {
                    emptyList()
                } else {
                    templates.getValue(meal).random(rnd).map { (id, g) ->
                        val grams = (g * (1.35 + rnd.nextDouble() * 0.6) / 10).roundToInt() * 10
                        FoodEntry(id, grams)
                    }
                }
            }
            result[date] = DayLog(meals, waterGlasses = 2 + rnd.nextInt(6))
        }
        result[today] = DayLog(
            meals = mapOf(
                Meal.BREAKFAST to listOf(FoodEntry("p3", 250), FoodEntry("p28", 120)),
                Meal.LUNCH to listOf(FoodEntry("p0", 200), FoodEntry("p10", 150), FoodEntry("p23", 100)),
                Meal.DINNER to emptyList(),
                Meal.SNACK to emptyList()
            ),
            waterGlasses = 3
        )
        return result
    }
}
