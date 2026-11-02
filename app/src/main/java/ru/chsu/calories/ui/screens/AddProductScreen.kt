@file:OptIn(ExperimentalMaterial3Api::class)

package ru.chsu.calories.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.chsu.calories.data.Meal
import ru.chsu.calories.data.Nutrients
import ru.chsu.calories.ui.AppViewModel
import ru.chsu.calories.ui.components.ChoiceChips
import ru.chsu.calories.ui.components.ProductRow
import ru.chsu.calories.ui.components.ScreenHeader
import ru.chsu.calories.ui.components.fmt

/**
 * Экран «Добавление продукта» (рис. 2 в презентации).
 * Открывается из дневника (с поиском), из справочника (продукт уже выбран, можно выбрать приём пищи)
 * или для изменения массы уже добавленной записи.
 */
@Composable
fun AddProductScreen(
    vm: AppViewModel,
    initialMeal: Meal,
    presetProductId: String?,
    editIndex: Int,
    fromProducts: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onCustom: () -> Unit
) {
    val editing = editIndex >= 0
    val editEntry = if (editing) vm.day().meals[initialMeal].orEmpty().getOrNull(editIndex) else null

    var meal by rememberSaveable { mutableStateOf(initialMeal) }
    var selectedId by rememberSaveable { mutableStateOf(editEntry?.productId ?: presetProductId) }
    var query by rememberSaveable { mutableStateOf("") }
    var gramsText by rememberSaveable {
        mutableStateOf((editEntry?.grams ?: if (fromProducts) 100 else 150).toString())
    }

    // вернулись с экрана «Свой продукт» — сразу выбираем новый продукт
    LaunchedEffect(vm.lastCustomProductId) {
        val id = vm.lastCustomProductId ?: return@LaunchedEffect
        selectedId = id
        query = vm.product(id).name
        vm.lastCustomProductId = null
    }

    val grams = gramsText.toIntOrNull() ?: 0
    val showSearch = !editing && !fromProducts
    val selected = selectedId?.let { vm.product(it) }
    val backLabel = when {
        editing -> initialMeal.title
        fromProducts -> "Продукты"
        else -> initialMeal.title
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (editing) "Изменить" else "Добавить", backLabel = backLabel, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (fromProducts) {
                Text("Куда добавить", style = MaterialTheme.typography.labelLarge)
                ChoiceChips(Meal.entries.map { it to it.title }, meal) { meal = it }
            }

            if (showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Поиск, например «гречка»") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val results = vm.search(query).take(if (query.isBlank()) 5 else 8)
                if (results.isEmpty()) {
                    Text(
                        "Ничего не найдено по запросу «$query»",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                results.forEach { p ->
                    ProductRow(p, selected = p.id == selectedId) { selectedId = p.id }
                }
                TextButton(onClick = onCustom, contentPadding = PaddingValues(0.dp)) {
                    Text("Нет нужного продукта? Добавить свой")
                }
            } else if (selected != null) {
                ProductRow(selected, selected = true) {}
            }

            Text("Масса, г", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { gramsText = (grams - 10).coerceAtLeast(0).toString() },
                    modifier = Modifier.size(52.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { t -> gramsText = t.filter { it.isDigit() }.take(4) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { gramsText = (grams + 10).coerceAtMost(3000).toString() },
                    modifier = Modifier.size(52.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(50, 100, 150, 200, 250, 300).forEach { g ->
                    AssistChip(onClick = { gramsText = g.toString() }, label = { Text("$g г") })
                }
            }

            // итог — пересчитывается при каждом изменении
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    if (selected == null) {
                        Text("Выберите продукт из списка", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Итого: 0 ккал", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    } else {
                        val n = Nutrients.of(selected, grams)
                        Text("${selected.name}, $grams г", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Итого: ${n.kcal.fmt()} ккал", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Б ${n.protein.fmt()} г · Ж ${n.fat.fmt()} г · У ${n.carbs.fmt()} г",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Button(
            onClick = {
                val id = selectedId ?: return@Button
                if (editing) vm.updateEntry(initialMeal, editIndex, grams) else vm.addEntry(meal, id, grams)
                onDone()
            },
            enabled = selectedId != null && grams in 1..3000,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(40.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) { Text(if (editing) "Сохранить" else "Добавить", style = MaterialTheme.typography.labelLarge) }
    }
}
