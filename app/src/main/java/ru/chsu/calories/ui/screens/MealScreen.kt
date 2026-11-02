package ru.chsu.calories.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.chsu.calories.data.Meal
import ru.chsu.calories.ui.AppViewModel
import ru.chsu.calories.ui.components.OutlinedBox
import ru.chsu.calories.ui.components.ScreenHeader
import ru.chsu.calories.ui.components.fmt

@Composable
fun MealScreen(
    vm: AppViewModel,
    meal: Meal,
    onBack: () -> Unit,
    onEdit: (index: Int) -> Unit,
    onAdd: () -> Unit
) {
    val list = vm.day().meals[meal].orEmpty()
    val sum = vm.total(list)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(meal.title, backLabel = "Дневник", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "${vm.selectedDate.short()} · нажмите на продукт, чтобы изменить массу",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (list.isEmpty()) {
                Text(
                    "В этом приёме пищи пока ничего нет",
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            list.forEachIndexed { index, entry ->
                val p = vm.product(entry.productId)
                val n = vm.nutrients(entry)
                OutlinedBox {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { onEdit(index) }
                                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp)
                        ) {
                            Text(p.name, fontWeight = FontWeight.Medium)
                            Text(
                                "${entry.grams} г · Б ${n.protein.fmt()} Ж ${n.fat.fmt()} У ${n.carbs.fmt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("${n.kcal.fmt()} ккал", fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
                        IconButton(onClick = { vm.removeEntry(meal, index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Всего за ${meal.title.lowercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${sum.kcal.fmt()} ккал", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Б ${sum.protein.fmt()} г · Ж ${sum.fat.fmt()} г · У ${sum.carbs.fmt()} г",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(40.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text("Добавить продукт", style = MaterialTheme.typography.labelLarge)
        }
    }
}
