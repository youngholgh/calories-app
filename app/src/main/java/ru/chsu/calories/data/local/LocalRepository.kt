package ru.chsu.calories.data.local

import ru.chsu.calories.data.Activity
import ru.chsu.calories.data.Competition
import ru.chsu.calories.data.DayLog
import ru.chsu.calories.data.FoodEntry
import ru.chsu.calories.data.Goal
import ru.chsu.calories.data.Meal
import ru.chsu.calories.data.Product
import ru.chsu.calories.data.Profile
import ru.chsu.calories.data.Sex
import java.time.LocalDate

/** Всё, что лежит в базе, одним объектом — читается при запуске приложения. */
data class SavedState(
    val email: String?,
    val profile: Profile?,
    val days: Map<LocalDate, DayLog>,
    val customProducts: List<Product>,
    val competition: Competition?
)

/**
 * Перевод между моделями приложения и таблицами Room.
 * ViewModel работает только с этим классом и не знает про сущности базы.
 */
class LocalRepository(private val dao: CaloriesDao) {

    suspend fun load(): SavedState {
        val p = dao.profile()
        val entries = dao.allEntries()
        val water = dao.allWater()

        val dates = (entries.map { it.day } + water.map { it.day }).toSet()
        val days = dates.associate { day ->
            val forDay = entries.filter { it.day == day }
            val meals = Meal.entries.associateWith { meal ->
                forDay.filter { it.meal == meal.name }
                    .sortedBy { it.position }
                    .map { FoodEntry(it.productId, it.grams) }
            }
            LocalDate.parse(day) to DayLog(meals, water.firstOrNull { it.day == day }?.glasses ?: 0)
        }

        return SavedState(
            email = p?.email,
            profile = p?.let {
                Profile(
                    sex = Sex.valueOf(it.sex),
                    age = it.age,
                    heightCm = it.heightCm,
                    weightKg = it.weightKg,
                    activity = Activity.valueOf(it.activity),
                    goal = Goal.valueOf(it.goal)
                )
            },
            days = days,
            customProducts = dao.customProducts().map {
                Product(it.id, it.name, it.kcal, it.protein, it.fat, it.carbs, it.category, custom = true)
            },
            competition = dao.competition()?.let { Competition(it.name, it.code, LocalDate.parse(it.startDay)) }
        )
    }

    suspend fun saveProfile(email: String?, p: Profile) {
        dao.saveProfile(
            ProfileEntity(
                email = email,
                sex = p.sex.name,
                age = p.age,
                heightCm = p.heightCm,
                weightKg = p.weightKg,
                activity = p.activity.name,
                goal = p.goal.name
            )
        )
    }

    suspend fun saveDay(date: LocalDate, log: DayLog) {
        val day = date.toString()
        dao.replaceDay(day, toEntities(day, log), WaterEntity(day, log.waterGlasses))
    }

    suspend fun replaceDiary(days: Map<LocalDate, DayLog>) {
        dao.replaceDiary(
            entries = days.flatMap { (date, log) -> toEntities(date.toString(), log) },
            water = days.map { (date, log) -> WaterEntity(date.toString(), log.waterGlasses) }
        )
    }

    suspend fun saveCustomProduct(p: Product) {
        dao.saveCustomProduct(CustomProductEntity(p.id, p.name, p.kcal, p.protein, p.fat, p.carbs, p.category))
    }

    suspend fun saveCompetition(c: Competition?) {
        if (c == null) dao.deleteCompetition()
        else dao.saveCompetition(CompetitionEntity(name = c.name, code = c.code, startDay = c.startDay.toString()))
    }

    private fun toEntities(day: String, log: DayLog): List<DiaryEntryEntity> =
        log.meals.flatMap { (meal, list) ->
            list.mapIndexed { i, e ->
                DiaryEntryEntity(day = day, meal = meal.name, position = i, productId = e.productId, grams = e.grams)
            }
        }
}
