package ru.chsu.calories.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.chsu.calories.data.Competition
import ru.chsu.calories.data.Standing
import ru.chsu.calories.domain.Scoring
import ru.chsu.calories.ui.AppViewModel
import ru.chsu.calories.ui.components.InfoHint
import ru.chsu.calories.ui.components.ScreenHeader
import ru.chsu.calories.ui.components.fmt
import ru.chsu.calories.ui.theme.OkColor
import ru.chsu.calories.ui.theme.WarnColor

/**
 * Соревновательный режим: группа друзей по коду, рейтинг за текущую неделю.
 * Каждый участник сравнивается со своей нормой, поэтому цели могут быть разными.
 */
@Composable
fun CompetitionScreen(vm: AppViewModel) {
    val c = vm.competition
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Соревнование")
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (c == null) NoCompetition(vm) else Leaderboard(vm, c)
        }
    }
}

@Composable
private fun NoCompetition(vm: AppViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Text(
        "Соревнуйтесь с друзьями: кто чаще укладывается в свою норму, пьёт достаточно воды и держит серию дней подряд.",
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    RulesCard()

    Text("Создать группу", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = name, onValueChange = { name = it },
        label = { Text("Название") }, placeholder = { Text("Неделя без срывов") },
        singleLine = true, modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = { vm.createCompetition(name) }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
        Text("Создать и получить код")
    }

    Text("Или вступить по коду друга", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    OutlinedTextField(
        value = code,
        onValueChange = { code = it; error = null },
        label = { Text("Код группы") }, placeholder = { Text("KAL-1234") },
        singleLine = true, isError = error != null,
        supportingText = if (error != null) ({ Text(error ?: "") }) else null,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedButton(
        onClick = { error = vm.joinCompetition(code) },
        modifier = Modifier.fillMaxWidth().height(44.dp)
    ) { Text("Вступить") }
    Text(
        "Пока сервер не подключён, соперники в группе — демо-участники. Любой код вида KAL-1234 подойдёт.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RulesCard() {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Как начисляются очки", fontWeight = FontWeight.Medium)
            Text("+${Scoring.NORM_POINTS} — день в своей норме (±10%)", style = MaterialTheme.typography.bodySmall)
            Text("+${Scoring.WATER_POINTS} — выпито ${Scoring.WATER_GOAL} стаканов воды", style = MaterialTheme.typography.bodySmall)
            Text("+${Scoring.STREAK_BONUS} — за каждый день серии от ${Scoring.STREAK_FROM} дней подряд", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Leaderboard(vm: AppViewModel, c: Competition) {
    val clipboard = LocalClipboardManager.current
    val dates = vm.weekDates()
    val standings = vm.standings()
    val maxPoints = (standings.maxOfOrNull { it.points } ?: 0).coerceAtLeast(1)
    val myPlace = standings.indexOfFirst { it.participant.isMe } + 1
    val me = standings.firstOrNull { it.participant.isMe }

    // шапка группы с кодом приглашения
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                InfoHint(
                    "Как начисляются очки",
                    "+${Scoring.NORM_POINTS} за день в своей норме (±10%), " +
                        "+${Scoring.WATER_POINTS} за ${Scoring.WATER_GOAL} стаканов воды, " +
                        "+${Scoring.STREAK_BONUS} за каждый день серии от ${Scoring.STREAK_FROM} дней. " +
                        "Каждого сравниваем с его собственной нормой, поэтому цели у участников могут быть разными."
                )
            }
            Text(
                "Неделя ${dates.first().short()} — ${dates.first().plusDays(6).short()} · итоги в воскресенье",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Код для друзей: ", style = MaterialTheme.typography.bodyMedium)
                Text(c.code, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(c.code))
                    vm.show("Код ${c.code} скопирован")
                }) { Text("Копировать") }
            }
        }
    }

    // моё положение и что сделать сегодня
    if (me != null) {
        val left = vm.norm.kcal - vm.dayTotal(vm.today).kcal
        val todayHint = when {
            Scoring.inNorm(vm.dayTotal(vm.today).kcal, vm.norm.kcal) -> "Сегодня вы в норме — день засчитан (+${Scoring.NORM_POINTS})"
            left > 0 -> "До зоны нормы сегодня осталось ${(left - vm.norm.kcal * 0.1).coerceAtLeast(0.0).fmt()} ккал"
            else -> "Сегодня перебор на ${(-left).fmt()} ккал — очки за день не начислятся"
        }
        Text("Вы на $myPlace месте из ${standings.size}", style = MaterialTheme.typography.titleMedium)
        Text(todayHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    standings.forEachIndexed { index, s -> StandingRow(index + 1, s, maxPoints, dates.size) }

    OutlinedButton(onClick = { vm.leaveCompetition() }, modifier = Modifier.fillMaxWidth()) {
        Text("Выйти из соревнования", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun StandingRow(place: Int, s: Standing, maxPoints: Int, daysCount: Int) {
    val mine = s.participant.isMe
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (mine) 1.5.dp else 1.dp,
            if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // кружок с местом: первые три места выделены
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (place <= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$place",
                        fontWeight = FontWeight.Bold,
                        color = if (place <= 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.participant.name, fontWeight = FontWeight.Medium)
                    Text(
                        "норма ${s.participant.normKcal} ккал · в норме ${s.daysInNorm} из $daysCount" +
                            (if (s.streak >= 2) " · серия ${s.streak}" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.points}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (s.todayPoints > 0) "+${s.todayPoints} сегодня" else "очков",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (s.todayPoints > 0) OkColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LinearProgressIndicator(
                progress = { s.points.toFloat() / maxPoints },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (mine) MaterialTheme.colorScheme.primary else WarnColor.copy(alpha = 0.7f),
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
