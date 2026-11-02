package ru.chsu.calories.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.chsu.calories.data.Meal
import ru.chsu.calories.ui.AppViewModel
import ru.chsu.calories.ui.components.Hints
import ru.chsu.calories.ui.components.InfoHint
import ru.chsu.calories.ui.components.MacroBox
import ru.chsu.calories.ui.components.OutlinedBox
import ru.chsu.calories.ui.components.ScreenHeader
import ru.chsu.calories.ui.components.fmt
import ru.chsu.calories.ui.theme.CarbsColor
import ru.chsu.calories.ui.theme.FatColor
import ru.chsu.calories.ui.theme.OkColor
import ru.chsu.calories.ui.theme.ProteinColor
import ru.chsu.calories.ui.theme.WarnColor
import ru.chsu.calories.ui.theme.WaterColor
import java.time.LocalDate

private val MONTHS = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
private val WEEKDAYS = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")

fun LocalDate.short(): String = "$dayOfMonth ${MONTHS[monthValue - 1]}"
fun LocalDate.weekdayShort(): String = WEEKDAYS[dayOfWeek.value - 1]

@Composable
fun DiaryScreen(vm: AppViewModel, onOpenMeal: (Meal) -> Unit, onAdd: (Meal) -> Unit) {
    val date = vm.selectedDate
    val day = vm.day(date)
    val total = vm.dayTotal(date)
    val norm = vm.norm
    val left = norm.kcal - total.kcal
    val isToday = date == vm.today

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Дневник") {
            SyncChip(vm.online, vm.syncQueue) { vm.toggleOnline() }
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // переключение дней
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.shiftDate(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий день")
                }
                Text(
                    (if (isToday) "Сегодня, " else "${date.weekdayShort()}, ") + date.short(),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(onClick = { vm.shiftDate(1) }, enabled = !isToday) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий день")
                }
            }

            // итог за день
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // цель и подсказка «i» в правом верхнем углу карточки
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Цель: ${vm.profile.goal.title} · ${vm.profile.goal.note}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        InfoHint(
                            Hints.GOAL_TITLE,
                            "Ваша норма ${norm.kcal} ккал: базовый обмен ${norm.bmr.fmt()} × активность " +
                                "${vm.profile.activity.factor.toString().replace('.', ',')} × цель «${vm.profile.goal.title}» " +
                                "(${vm.profile.goal.note}). Цель меняется в профиле."
                        )
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(total.kcal.fmt(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            " / ${norm.kcal} ккал",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (left >= 0) "осталось ${left.fmt()}" else "перебор ${(-left).fmt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (left >= 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (total.kcal / norm.kcal).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (left < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroBox("Белки", total.protein, norm.protein, ProteinColor, Modifier.weight(1f))
                        MacroBox("Жиры", total.fat, norm.fat, FatColor, Modifier.weight(1f))
                        MacroBox("Углеводы", total.carbs, norm.carbs, CarbsColor, Modifier.weight(1f))
                    }
                }
            }

            // приёмы пищи
            Meal.entries.forEach { meal ->
                val list = day.meals[meal].orEmpty()
                OutlinedBox(onClick = { onOpenMeal(meal) }) {
                    Row(
                        Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(meal.title, fontWeight = FontWeight.Medium)
                            Text(
                                if (list.isEmpty()) "пока пусто" else list.joinToString { vm.product(it.productId).name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(vm.total(list).kcal.fmt(), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(6.dp))
                        // компактная кнопка «+» (30 dp вместо стандартных 40)
                        FilledTonalIconButton(onClick = { onAdd(meal) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = "Добавить в ${meal.title}", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // вода
            OutlinedBox {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Text("Вода", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(
                            "${day.waterGlasses * 250} / 2000 мл",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(8) { i -> Glass(filled = i < day.waterGlasses) { vm.toggleWater(i) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Glass(filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    Box(
        modifier = Modifier
            .size(width = 30.dp, height = 36.dp)
            .clip(shape)
            .border(BorderStroke(1.5.dp, WaterColor), shape)
            .clickable { onClick() },
        contentAlignment = Alignment.BottomCenter
    ) {
        if (filled) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .background(WaterColor.copy(alpha = 0.75f))
            )
        }
    }
}

@Composable
private fun SyncChip(online: Boolean, queue: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (online) OkColor else WarnColor)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (online) "синхронизировано" else "офлайн · $queue в очереди",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
