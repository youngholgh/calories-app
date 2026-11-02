package ru.chsu.calories.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.chsu.calories.data.Meal
import ru.chsu.calories.ui.AppViewModel
import ru.chsu.calories.ui.components.ChoiceChips
import ru.chsu.calories.ui.components.OutlinedBox
import ru.chsu.calories.ui.components.ScreenHeader
import ru.chsu.calories.ui.components.StatBox
import ru.chsu.calories.ui.components.fmt
import ru.chsu.calories.ui.theme.CarbsColor
import ru.chsu.calories.ui.theme.FatColor
import ru.chsu.calories.ui.theme.OkColor
import ru.chsu.calories.ui.theme.ProteinColor
import ru.chsu.calories.ui.theme.WarnColor
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max

private enum class Period(val title: String) { DAY("День"), WEEK("Неделя"), MONTH("Месяц") }

@Composable
fun StatsScreen(vm: AppViewModel, onOpenDay: (LocalDate) -> Unit) {
    var period by rememberSaveable { mutableStateOf(Period.WEEK) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Статистика")
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChoiceChips(Period.entries.map { it to it.title }, period) { period = it }
            when (period) {
                Period.DAY -> DayStats(vm)
                Period.WEEK -> RangeStats(vm, 7, onOpenDay)
                Period.MONTH -> RangeStats(vm, 30, onOpenDay)
            }
        }
    }
}

@Composable
private fun RangeStats(vm: AppViewModel, n: Int, onOpenDay: (LocalDate) -> Unit) {
    val norm = vm.norm.kcal
    val end = vm.selectedDate
    val dates = (n - 1 downTo 0).map { end.minusDays(it.toLong()) }
    val values = dates.map { vm.dayTotal(it).kcal }
    val filled = values.filter { it > 0 }
    val avg = if (filled.isEmpty()) 0.0 else filled.average()
    val inNorm = filled.count { abs(it - norm) <= norm * 0.1 }
    val bestIndex = values.indices.filter { values[it] > 0 }.minByOrNull { abs(values[it] - norm) }

    Text(
        "${dates.first().short()} — ${dates.last().short()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    BarChart(
        values = values,
        norm = norm,
        labels = dates.mapIndexed { i, d ->
            if (n == 7) d.weekdayShort() else if (i % 5 == 4 || i == n - 1) d.dayOfMonth.toString() else ""
        }
    )
    Text(
        "Пунктир — норма $norm ккал, оранжевым — дни с перебором больше 10%",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatBox("В среднем", "${avg.fmt()} ккал", Modifier.weight(1f))
        StatBox("Дней в норме ±10%", "$inNorm из ${filled.size}", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val diff = avg - norm
        StatBox(
            "Отклонение от нормы",
            (if (diff > 0) "+" else "−") + abs(diff).fmt(),
            Modifier.weight(1f),
            valueColor = if (diff > 0) WarnColor else OkColor
        )
        StatBox("Ближе всего к норме", bestIndex?.let { dates[it].short() } ?: "—", Modifier.weight(1f))
    }
    Text("По дням (нажмите, чтобы открыть)", style = MaterialTheme.typography.labelLarge)
    dates.indices.reversed().take(if (n == 7) 7 else 10).forEach { i ->
        OutlinedBox(onClick = { onOpenDay(dates[i]) }) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text("${dates[i].weekdayShort()}, ${dates[i].short()}", modifier = Modifier.weight(1f))
                Text("${values[i].fmt()} ккал", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun BarChart(values: List<Double>, norm: Int, labels: List<String>) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val normColor = MaterialTheme.colorScheme.error
    val maxValue = max(norm * 1.25, values.maxOrNull() ?: 0.0).toFloat()

    Column {
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val w = size.width
            val h = size.height
            val slot = w / values.size
            // сетка через каждые 1000 ккал
            var line = 1000f
            while (line < maxValue) {
                val y = h * (1f - line / maxValue)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 2f)
                line += 1000f
            }
            values.forEachIndexed { i, v ->
                val barH = h * (v.toFloat() / maxValue)
                drawRoundRect(
                    color = if (v > norm * 1.1) WarnColor else barColor,
                    topLeft = Offset(i * slot + slot * 0.15f, h - barH),
                    size = Size(slot * 0.7f, barH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
            val ny = h * (1f - norm / maxValue)
            drawLine(
                normColor, Offset(0f, ny), Offset(w, ny), strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun DayStats(vm: AppViewModel) {
    val date = vm.selectedDate
    val day = vm.day(date)
    val total = vm.dayTotal(date)
    val norm = vm.norm
    val mealKcal = Meal.entries.associateWith { vm.total(day.meals[it].orEmpty()).kcal }
    val maxMeal = max(1.0, mealKcal.values.maxOrNull() ?: 0.0)

    Text(
        "${date.short()} · ${total.kcal.fmt()} из ${norm.kcal} ккал",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Meal.entries.forEach { meal ->
        val k = mealKcal.getValue(meal)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text(meal.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text("${k.fmt()} ккал", style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(
                progress = { (k / maxMeal).toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }

    Text("Соотношение БЖУ по калориям", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
    val kp = total.protein * 4
    val kf = total.fat * 9
    val kc = total.carbs * 4
    val sum = kp + kf + kc
    if (sum <= 0) {
        Text("За этот день ещё ничего не записано", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
    ) {
        if (kp > 0) Box(Modifier.weight(kp.toFloat()).fillMaxHeight().background(ProteinColor))
        if (kf > 0) Box(Modifier.weight(kf.toFloat()).fillMaxHeight().background(FatColor))
        if (kc > 0) Box(Modifier.weight(kc.toFloat()).fillMaxHeight().background(CarbsColor))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        StatBox("Белки", "${(kp / sum * 100).fmt()}%", Modifier.weight(1f), valueColor = ProteinColor, hint = "цель 20%")
        StatBox("Жиры", "${(kf / sum * 100).fmt()}%", Modifier.weight(1f), valueColor = FatColor, hint = "цель 30%")
        StatBox("Углев.", "${(kc / sum * 100).fmt()}%", Modifier.weight(1f), valueColor = CarbsColor, hint = "цель 50%")
    }
    StatBox("Вода", "${day.waterGlasses * 250} мл", Modifier.fillMaxWidth())
}
