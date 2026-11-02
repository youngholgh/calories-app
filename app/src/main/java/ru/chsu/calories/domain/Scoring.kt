package ru.chsu.calories.domain

import ru.chsu.calories.data.DayResult
import ru.chsu.calories.data.Participant
import ru.chsu.calories.data.Standing
import java.time.LocalDate
import kotlin.math.abs

/**
 * Очки в соревновании. Каждого сравниваем с его СОБСТВЕННОЙ нормой,
 * поэтому у человека на похудении и у человека на наборе массы равные шансы.
 *  - день в норме (±10%)          → 10 очков
 *  - выпито 8 стаканов (2 л) воды → 3 очка
 *  - серия от 3 дней в норме подряд → +5 очков за каждый такой день
 */
object Scoring {
    const val NORM_POINTS = 10
    const val WATER_POINTS = 3
    const val STREAK_BONUS = 5
    const val STREAK_FROM = 3
    const val WATER_GOAL = 8

    fun inNorm(kcal: Double, norm: Int): Boolean = kcal > 0 && abs(kcal - norm) <= norm * 0.1

    fun dayPoints(d: DayResult?, norm: Int): Int {
        if (d == null) return 0
        var p = 0
        if (inNorm(d.kcal, norm)) p += NORM_POINTS
        if (d.waterGlasses >= WATER_GOAL) p += WATER_POINTS
        return p
    }

    fun standing(p: Participant, dates: List<LocalDate>): Standing {
        var points = 0
        var run = 0
        var inNormCount = 0
        dates.forEach { date ->
            val d = p.days[date]
            points += dayPoints(d, p.normKcal)
            if (d != null && inNorm(d.kcal, p.normKcal)) {
                inNormCount++
                run++
                if (run >= STREAK_FROM) points += STREAK_BONUS
            } else {
                run = 0
            }
        }
        return Standing(
            participant = p,
            points = points,
            daysInNorm = inNormCount,
            streak = currentStreak(p, dates),
            todayPoints = dates.lastOrNull()?.let { dayPoints(p.days[it], p.normKcal) } ?: 0
        )
    }

    /** Серия дней в норме подряд. Сегодняшний день, если он ещё не в норме, серию не обрывает. */
    private fun currentStreak(p: Participant, dates: List<LocalDate>): Int {
        var list = dates
        val last = dates.lastOrNull() ?: return 0
        val lastDay = p.days[last]
        if (lastDay == null || !inNorm(lastDay.kcal, p.normKcal)) list = dates.dropLast(1)
        var streak = 0
        for (date in list.asReversed()) {
            val d = p.days[date]
            if (d != null && inNorm(d.kcal, p.normKcal)) streak++ else break
        }
        return streak
    }

    fun rank(standings: List<Standing>): List<Standing> =
        standings.sortedWith(compareByDescending<Standing> { it.points }.thenByDescending { it.daysInNorm })
}
