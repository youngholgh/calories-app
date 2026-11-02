package ru.chsu.calories.data

import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/** Группа, в которой соревнуется пользователь. Код группы раздаётся друзьям. */
data class Competition(val name: String, val code: String, val startDay: LocalDate)

/** Итог дня участника: сколько съедено и сколько выпито стаканов воды. */
data class DayResult(val kcal: Double, val waterGlasses: Int)

data class Participant(
    val id: String,
    val name: String,
    val isMe: Boolean,
    val normKcal: Int,
    val days: Map<LocalDate, DayResult>
)

/** Место участника в таблице за неделю. */
data class Standing(
    val participant: Participant,
    val points: Int,
    val daysInNorm: Int,
    val streak: Int,
    val todayPoints: Int
)

/**
 * Соперники для демо-режима. Пока сервера нет, участники группы генерируются по коду:
 * у одного и того же кода всегда одни и те же люди и одни и те же результаты.
 * Когда появится сервер, этот объект заменится запросом GET /competitions/{code}.
 */
object DemoRivals {

    private val names = listOf("Роман", "Артём", "Максим", "Аня", "Катя", "Илья", "Соня", "Денис")

    fun generate(code: String, dates: List<LocalDate>, today: LocalDate): List<Participant> {
        val rnd = Random(code.hashCode())
        val picked = names.shuffled(rnd).take(4)
        return picked.mapIndexed { i, name ->
            val norm = (2000 + rnd.nextInt(10) * 100)
            // у каждого свой «характер»: кто-то стабильно в норме, кто-то часто переедает
            val discipline = 0.55 + rnd.nextDouble() * 0.4
            val days = dates.associateWith { date ->
                val inProgress = date == today
                val factor = when {
                    inProgress -> 0.35 + rnd.nextDouble() * 0.5           // день ещё идёт
                    rnd.nextDouble() < discipline -> 0.92 + rnd.nextDouble() * 0.16
                    else -> if (rnd.nextBoolean()) 1.12 + rnd.nextDouble() * 0.25 else 0.7 + rnd.nextDouble() * 0.15
                }
                DayResult(
                    kcal = (norm * factor).roundToInt().toDouble(),
                    waterGlasses = if (inProgress) rnd.nextInt(2, 6) else rnd.nextInt(3, 9)
                )
            }
            Participant("r$i", name, isMe = false, normKcal = norm, days = days)
        }
    }
}
