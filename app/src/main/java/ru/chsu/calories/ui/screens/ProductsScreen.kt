@file:OptIn(ExperimentalMaterial3Api::class)

package ru.chsu.calories.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.chsu.calories.data.ProductCatalog
import ru.chsu.calories.ui.AppViewModel
import ru.chsu.calories.ui.components.ProductRow
import ru.chsu.calories.ui.components.ScreenHeader

private const val ALL = -1
private const val MINE = -2

@Composable
fun ProductsScreen(vm: AppViewModel, onPick: (productId: String) -> Unit, onCustom: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableIntStateOf(ALL) }

    // здесь выбирать новый продукт не нужно — просто сбрасываем флаг
    LaunchedEffect(vm.lastCustomProductId) { vm.lastCustomProductId = null }

    val source = when (category) {
        ALL -> vm.allProducts
        MINE -> vm.customProducts
        else -> vm.allProducts.filter { it.category == category }
    }
    val list = vm.search(query, source)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Продукты") {
                Text("${vm.allProducts.size} шт.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск по справочнику") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            val chips = listOf(ALL to "Все") + ProductCatalog.categories.mapIndexed { i, c -> i to c } + listOf(MINE to "Мои")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chips) { (value, label) ->
                    FilterChip(selected = category == value, onClick = { category = value }, label = { Text(label) })
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (list.isEmpty()) {
                    item {
                        Text(
                            if (category == MINE) "Вы ещё не добавили свои продукты" else "Ничего не найдено",
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(list, key = { it.id }) { p ->
                    ProductRow(p, showMacros = true) { onPick(p.id) }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onCustom,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Свой продукт") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}
