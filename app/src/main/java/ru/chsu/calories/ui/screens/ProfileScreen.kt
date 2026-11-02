package ru.chsu.calories.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.chsu.calories.data.Activity
import ru.chsu.calories.data.Goal
import ru.chsu.calories.data.Profile
import ru.chsu.calories.data.Sex
import ru.chsu.calories.domain.Norm
import ru.chsu.calories.ui.AppViewModel
import ru.chsu.calories.ui.components.ChoiceChips
import ru.chsu.calories.ui.components.Hints
import ru.chsu.calories.ui.components.InfoHint
import ru.chsu.calories.ui.components.ScreenHeader
import ru.chsu.calories.ui.components.SimpleDropdown
import ru.chsu.calories.ui.components.fmt

/** Экран «Профиль» (рис. 3 в презентации). При регистрации открывается как шаг онбординга. */
@Composable
fun ProfileScreen(vm: AppViewModel, onboarding: Boolean, onFinish: () -> Unit, onLogout: () -> Unit) {
    val p = vm.profile

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(if (onboarding) "Расскажите о себе" else "Профиль") {
            if (onboarding) {
                Text("шаг 2 из 2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (onboarding) "Эти данные нужны, чтобы рассчитать вашу суточную норму калорий."
                else vm.userEmail ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LabeledRow("Пол") {
                ChoiceChips(listOf(Sex.MALE to "муж.", Sex.FEMALE to "жен."), p.sex) { vm.updateProfile(vm.profile.copy(sex = it)) }
            }
            NumberRow("Возраст", p.age, 14..100) { vm.updateProfile(vm.profile.copy(age = it)) }
            NumberRow("Рост, см", p.heightCm, 120..230) { vm.updateProfile(vm.profile.copy(heightCm = it)) }
            NumberRow("Вес, кг", p.weightKg, 35..250) { vm.updateProfile(vm.profile.copy(weightKg = it)) }

            Text("Активность", style = MaterialTheme.typography.labelLarge)
            SimpleDropdown(Activity.entries, p.activity, { "${it.title} — ${it.hint}" }) {
                vm.updateProfile(vm.profile.copy(activity = it))
            }
            // заголовок блока «Цель» с подсказкой «i» справа
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Цель", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                InfoHint(Hints.GOAL_TITLE, Hints.GOAL_TEXT)
            }
            ChoiceChips(Goal.entries.map { it to it.title }, p.goal) { vm.updateProfile(vm.profile.copy(goal = it)) }

            NormCard(p, vm.norm)

            if (onboarding) {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Начать вести дневник")
                }
            } else {
                OutlinedButton(onClick = { vm.resetDiary() }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Заполнить дневник демо-данными заново")
                }
                TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                    Text("Выйти из аккаунта", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(100.dp))
        Row(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun NumberRow(label: String, value: Int, range: IntRange, onValid: (Int) -> Unit) {
    var text by rememberSaveable(label) { mutableStateOf(value.toString()) }
    val number = text.toIntOrNull()
    val ok = number != null && number in range
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(100.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { t ->
                val clean = t.filter { it.isDigit() }.take(3)
                text = clean
                clean.toIntOrNull()?.takeIf { it in range }?.let(onValid)
            },
            singleLine = true,
            isError = !ok,
            supportingText = if (ok) null else ({ Text("от ${range.first} до ${range.last}") }),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NormCard(p: Profile, norm: Norm) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Суточная норма",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                InfoHint(
                    "Формула Миффлина–Сан Жеора",
                    "Белки, жиры и углеводы делятся как 20% / 30% / 50% калорий. " +
                        "1 г белка или углеводов — 4 ккал, 1 г жира — 9 ккал. " +
                        "Сейчас выбрана цель «${p.goal.title}»: ${p.goal.note}."
                )
            }
            Text("${norm.kcal} ккал", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Б ${norm.protein} г · Ж ${norm.fat} г · У ${norm.carbs} г", style = MaterialTheme.typography.bodyMedium)
            Text(
                "BMR = 10×${p.weightKg} + 6,25×${p.heightCm} − 5×${p.age} ${if (p.sex == Sex.MALE) "+ 5" else "− 161"} = ${norm.bmr.fmt()}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StepRow("Базовый обмен (BMR)", norm.bmr.fmt())
            StepRow("× активность ${p.activity.factor.toString().replace('.', ',')}", norm.tdee.fmt())
            StepRow("цель: ${p.goal.title} (${p.goal.note})", norm.kcal.toString())
        }
    }
}

@Composable
private fun StepRow(label: String, value: String) {
    Row {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
