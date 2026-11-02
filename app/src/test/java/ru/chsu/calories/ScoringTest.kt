package ru.chsu.calories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.chsu.calories.data.DayResult
import ru.chsu.calories.data.Participant
import ru.chsu.calories.domain.Scoring
import java.time.LocalDate

class ScoringTest {

    private val monday = LocalDate.of(2026, 10, 26)
    private val week = (0L..6L).map { monday.plusDays(it) }

    private fun player(vararg kcal: Int, water: Int = 0) = Participant(
        id = "t", name = "Тест", isMe = false, normKcal = 2000,
        days = week.zip(kcal.toList()).associate { (d, k) -> d to DayResult(k.toDouble(), water) }
    )

    @Test
    fun normZoneIsTenPercent() {
        assertTrue(Scoring.inNorm(1800.0, 2000))   // −10% — ещё норма
        assertTrue(Scoring.inNorm(2200.0, 2000))   // +10% — ещё норма
        assertFalse(Scoring.inNorm(2250.0, 2000))
        assertFalse(Scoring.inNorm(0.0, 2000))     // пустой день не засчитывается
    }

    @Test
    fun waterGivesExtraPoints() {
        assertEquals(13, Scoring.dayPoints(DayResult(2000.0, 8), 2000))
        assertEquals(10, Scoring.dayPoints(DayResult(2000.0, 7), 2000))
    }

    @Test
    fun streakBonusFromThirdDay() {
        // 4 дня в норме подряд: 4 × 10 + бонус за 3-й и 4-й день (2 × 5) = 50
        val s = Scoring.standing(player(2000, 2000, 2000, 2000), week.take(4))
        assertEquals(50, s.points)
        assertEquals(4, s.streak)
    }

    @Test
    fun overeatingBreaksStreak() {
        val s = Scoring.standing(player(2000, 2000, 2600, 2000), week.take(4))
        assertEquals(30, s.points)
        assertEquals(1, s.streak)
        assertEquals(3, s.daysInNorm)
    }
}
