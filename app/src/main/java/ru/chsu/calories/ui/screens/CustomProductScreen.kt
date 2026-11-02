package ru.chsu.calories.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.chsu.calories.data.ProductCatalog
import ru.chsu.calories.ui.AppViewModel
import ru.chsu.calories.ui.components.ScreenHeader
import ru.chsu.calories.ui.components.SimpleDropdown
import ru.chsu.calories.ui.components.fmt
import ru.chsu.calories.ui.theme.WarnColor
import kotlin.math.abs

private fun String.toNumber(): Double? = replace(',', '.').toDoubleOrNull()

@Composable
fun CustomProductScreen(vm: AppViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableIntStateOf(0) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val k = kcal.toNumber()
    val p = protein.toNumber()
    val f = fat.toNumber()
    val c = carbs.toNumber()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Свой продукт", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Значения указываются на 100 г продукта — их можно найти на упаковке.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it; error = null },
                label = { Text("Название") }, placeholder = { Text("Сырники домашние") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            NumberInput("Калорийность, ккал", kcal, Modifier.fillMaxWidth()) { kcal = it; error = null }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInput("Белки, г", protein, Modifier.weight(1f)) { protein = it; error = null }
                NumberInput("Жиры, г", fat, Modifier.weight(1f)) { fat = it; error = null }
                NumberInput("Углев., г", carbs, Modifier.weight(1f)) { carbs = it; error = null }
            }
            Text("Категория", style = MaterialTheme.typography.labelLarge)
            SimpleDropdown(ProductCatalog.categories.indices.toList(), category, { ProductCatalog.categories[it] }) { category = it }

            // проверка: 1 г белка/углеводов ≈ 4 ккал, 1 г жира ≈ 9 ккал
            if (k != null && p != null && f != null && c != null) {
                val estimate = 4 * p + 9 * f + 4 * c
                if (abs(estimate - k) > k * 0.15 + 10) {
                    Text("По БЖУ получается около ${estimate.fmt()} ккал, а указано ${k.fmt()}. Проверьте значения.", color = WarnColor, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("По БЖУ ≈ ${estimate.fmt()} ккал — сходится с указанным.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Button(
            onClick = {
                if (name.isBlank()) {
                    error = "Укажите название продукта"
                    return@Button
                }
                if (k == null || p == null || f == null || c == null || k < 0 || p < 0 || f < 0 || c < 0) {
                    error = "Заполните калорийность и БЖУ числами (можно 0)"
                    return@Button
                }
                if (k > 900 || p + f + c > 100) {
                    error = "На 100 г не может быть больше 900 ккал и 100 г БЖУ"
                    return@Button
                }
                vm.addCustomProduct(name.trim(), k, p, f, c, category)
                onSaved()
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp)
        ) { Text("Сохранить продукт") }
    }
}

@Composable
private fun NumberInput(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { t -> onChange(t.filter { it.isDigit() || it == ',' || it == '.' }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}
