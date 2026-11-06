package by.grinich.hscontrol.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import by.grinich.hscontrol.utils.MonthUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    userId: String,
    month: String,
    onBack: () -> Unit
) {
    val vm: SalesHistoryViewModel = viewModel()
    val state by vm.state.collectAsState()
    val models by vm.models.collectAsState()

    var period by remember { mutableStateOf("month") }
    var fromDate by remember { mutableStateOf("$month-01") }
    var toDate by remember { mutableStateOf("$month-31") }
    var specificDate by remember { mutableStateOf(MonthUtils.todayIso()) }
    var modelFilter by remember { mutableStateOf("all") }
    var periodOpen by remember { mutableStateOf(false) }
    var modelOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.loadModels()
        vm.load(userId, period, fromDate, toDate, specificDate, modelFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История продаж") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Период
            Box {
                OutlinedButton(
                    onClick = { periodOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Период: ${periodLabel(period)}")
                        Text("▼", fontSize = 12.sp)
                    }
                }
                DropdownMenu(
                    expanded = periodOpen,
                    onDismissRequest = { periodOpen = false }
                ) {
                    listOf(
                        "today" to "Сегодня",
                        "month" to "Текущий месяц",
                        "custom" to "Произвольный диапазон",
                        "specific" to "Конкретная дата"
                    ).forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                period = key
                                periodOpen = false
                                vm.load(userId, key, fromDate, toDate, specificDate, modelFilter)
                            }
                        )
                    }
                }
            }

            // Поля диапазона
            if (period == "custom") {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fromDate,
                        onValueChange = { fromDate = it },
                        label = { Text("С") },
                        placeholder = { Text("ГГГГ-ММ-ДД") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = toDate,
                        onValueChange = { toDate = it },
                        label = { Text("По") },
                        placeholder = { Text("ГГГГ-ММ-ДД") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (period == "specific") {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = specificDate,
                    onValueChange = { specificDate = it },
                    label = { Text("Дата") },
                    placeholder = { Text("ГГГГ-ММ-ДД") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))

            // Фильтр по модели
            Box {
                OutlinedButton(
                    onClick = { modelOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Модель: ${if (modelFilter == "all") "Все" else modelFilter}")
                        Text("▼", fontSize = 12.sp)
                    }
                }
                DropdownMenu(
                    expanded = modelOpen,
                    onDismissRequest = { modelOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Все модели") },
                        onClick = {
                            modelFilter = "all"
                            modelOpen = false
                            vm.load(userId, period, fromDate, toDate, specificDate, "all")
                        }
                    )
                    models.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.modelName) },
                            onClick = {
                                modelFilter = m.modelName
                                modelOpen = false
                                vm.load(userId, period, fromDate, toDate, specificDate, m.modelName)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.load(userId, period, fromDate, toDate, specificDate, modelFilter) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Показать")
            }

            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is HistoryState.Loading -> LoadingState()
                is HistoryState.Error -> ErrorState(s.message) {
                    vm.load(userId, period, fromDate, toDate, specificDate, modelFilter)
                }
                is HistoryState.Success -> HistoryContent(s)
            }
        }
    }
}

@Composable
private fun HistoryContent(data: HistoryState.Success) {
    if (data.totalHonor == 0 && data.totalCompetitors == 0) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Нет данных за выбранный период",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Сводка",
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Honor продано:", "${data.totalHonor} шт.")
                    InfoRow("Конкуренты:", "${data.totalCompetitors} шт.")
                    InfoRow("Всего:", "${data.totalAll} шт.")
                    InfoRow("Дней с продажами:", "${data.daysCount}")
                    InfoRow("Среднее в день:",
                        "${String.format("%.1f", data.avgPerDay)} шт.")
                }
            }
        }

        item { SectionTitle("По дням", "📅") }

        itemsIndexed(data.daily, key = { _, it -> it.date }) { _, day ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(MonthUtils.dateDisplay(day.date),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp)
                        Text("${day.totalForDay} шт.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    day.models.forEach { (model, qty) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(model, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$qty шт.", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item { SectionTitle("По моделям Honor", "📱") }

        itemsIndexed(data.modelStats, key = { _, it -> it.model }) { _, stat ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stat.model, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("${stat.quantity} шт. (${String.format("%.1f", stat.percent)}%)",
                            fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (stat.percent / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun periodLabel(key: String): String = when (key) {
    "today" -> "Сегодня"
    "month" -> "Текущий месяц"
    "custom" -> "Произвольный"
    "specific" -> "Конкретная дата"
    else -> key
}