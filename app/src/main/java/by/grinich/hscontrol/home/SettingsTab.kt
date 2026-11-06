package by.grinich.hscontrol.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import by.grinich.hscontrol.BuildConfig
import by.grinich.hscontrol.auth.UserProfile
import by.grinich.hscontrol.utils.MonthUtils
import kotlinx.coroutines.launch

@Composable
fun SettingsTab(
    profile: UserProfile,
    month: String,
    darkMode: Boolean?,
    onDarkModeChange: (Boolean?) -> Unit,
    isReadOnly: Boolean = false
) {
    val vm: SettingsViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(profile.id, month) {
        vm.load(profile.id, month, profile.role)
    }

    when (val s = state) {
        is SettingsState.Loading -> LoadingState()
        is SettingsState.Error -> ErrorState(s.message) { vm.refresh() }
        is SettingsState.Success -> SettingsContent(s, vm, darkMode, onDarkModeChange, isReadOnly)
    }
}

@Composable
private fun SettingsContent(
    data: SettingsState.Success,
    vm: SettingsViewModel,
    darkMode: Boolean?,
    onDarkModeChange: (Boolean?) -> Unit,
    isReadOnly: Boolean
) {
    var channel by remember { mutableStateOf(data.settings.channel) }
    var competitors by remember { mutableStateOf(data.settings.competitorsCount.toString()) }
    var baseSalary by remember { mutableStateOf(data.settings.baseSalary.toString()) }
    var mobilePay by remember { mutableStateOf(data.settings.mobilePayment.toString()) }
    var targetBonus by remember { mutableStateOf(data.settings.targetBonus?.toString() ?: "") }
    var overBonus by remember { mutableStateOf(data.settings.overBonus?.toString() ?: "") }
    var testBonus by remember { mutableStateOf(data.settings.testBonus.toString()) }

    LaunchedEffect(
        data.settings.channel,
        data.settings.competitorsCount,
        data.settings.baseSalary,
        data.settings.mobilePayment,
        data.settings.targetBonus,
        data.settings.overBonus,
        data.settings.testBonus
    ) {
        channel = data.settings.channel
        competitors = data.settings.competitorsCount.toString()
        baseSalary = data.settings.baseSalary.toString()
        mobilePay = data.settings.mobilePayment.toString()
        targetBonus = data.settings.targetBonus?.toString() ?: ""
        overBonus = data.settings.overBonus?.toString() ?: ""
        testBonus = data.settings.testBonus.toString()
    }

    fun saveCurrent() {
        val comp = competitors.toIntOrNull() ?: return
        if (comp < 0 || comp > 6) return
        val bs = baseSalary.replace(",", ".").toDoubleOrNull() ?: 500.0
        val mp = mobilePay.replace(",", ".").toDoubleOrNull() ?: 10.0
        val tb = testBonus.replace(",", ".").toDoubleOrNull() ?: 50.0
        val tbonus = if (targetBonus.isBlank()) null
        else targetBonus.replace(",", ".").toDoubleOrNull()
        val obonus = if (overBonus.isBlank()) null
        else overBonus.replace(",", ".").toDoubleOrNull()

        vm.autoSave(
            channel = channel,
            competitors = comp,
            baseSalary = bs,
            mobilePayment = mp,
            targetBonus = tbonus,
            overBonus = obonus,
            testBonus = tb
        )
    }

    var channelOpen by remember { mutableStateOf(false) }
    var showAddModelDialog by remember { mutableStateOf(false) }
    var saleToDelete by remember { mutableStateOf<String?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val canEditModels = data.role == "developer" && !isReadOnly

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ============ DEVELOPER: УПРАВЛЕНИЕ ФУНКЦИЯМИ ============
        if (!isReadOnly && data.role == "developer") {
            item(key = "developer_flags") {
                DeveloperFlagsCard()
            }
        }

        // ============ ТЕМА ============
        if (!isReadOnly) {
            item(key = "theme") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Оформление",
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = darkMode == null,
                                onClick = { onDarkModeChange(null) },
                                label = { Text("Авто") }
                            )
                            FilterChip(
                                selected = darkMode == false,
                                onClick = { onDarkModeChange(false) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LightMode, null,
                                            modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Светлая")
                                    }
                                }
                            )
                            FilterChip(
                                selected = darkMode == true,
                                onClick = { onDarkModeChange(true) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DarkMode, null,
                                            modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Тёмная")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // ============ НАСТРОЙКИ МЕСЯЦА ============
        item(key = "settings_month") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Настройки месяца",
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        if (!isReadOnly) {
                            Text("Автосохранение",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    Box {
                        OutlinedButton(
                            onClick = { if (!isReadOnly) channelOpen = true },
                            enabled = !isReadOnly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Канал: $channel")
                                if (!isReadOnly) Text("▼", fontSize = 12.sp)
                            }
                        }
                        if (!isReadOnly) {
                            DropdownMenu(
                                expanded = channelOpen,
                                onDismissRequest = { channelOpen = false }
                            ) {
                                listOf("5 Element", "A1", "MTS", "Electrosila").forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c) },
                                        onClick = {
                                            channel = c
                                            channelOpen = false
                                            saveCurrent()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = competitors,
                        onValueChange = {
                            if (!isReadOnly) {
                                competitors = it.filter { ch -> ch.isDigit() }.take(1)
                                saveCurrent()
                            }
                        },
                        label = { Text("Конкурентов (0–6)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !isReadOnly,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = testBonus,
                        onValueChange = {
                            if (!isReadOnly) {
                                testBonus = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                saveCurrent()
                            }
                        },
                        label = { Text("Бонус за тест, BYN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = !isReadOnly,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (data.version == 3) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = baseSalary,
                            onValueChange = {
                                if (!isReadOnly) {
                                    baseSalary = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                    saveCurrent()
                                }
                            },
                            label = { Text("Оклад, BYN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = !isReadOnly,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = mobilePay,
                            onValueChange = {
                                if (!isReadOnly) {
                                    mobilePay = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                    saveCurrent()
                                }
                            },
                            label = { Text("Мобильная связь, BYN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = !isReadOnly,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = targetBonus,
                            onValueChange = {
                                if (!isReadOnly) {
                                    targetBonus = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                    saveCurrent()
                                }
                            },
                            label = { Text("Выплата за целевую долю, BYN") },
                            placeholder = { Text("Пусто = табличное") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = !isReadOnly,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = overBonus,
                            onValueChange = {
                                if (!isReadOnly) {
                                    overBonus = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                    saveCurrent()
                                }
                            },
                            label = { Text("Бонус за 1% перевып., BYN") },
                            placeholder = { Text("Пусто = табличное") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = !isReadOnly,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    errorMsg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            }
        }

        // ============ МОДЕЛИ HONOR ============
        item(key = "honor_models") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Модели Honor (${data.honorModels.size})",
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        if (canEditModels) {
                            IconButton(onClick = { showAddModelDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить")
                            }
                        }
                    }
                    if (!canEditModels) {
                        Text("Редактирование недоступно",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))

                    data.honorModels.forEachIndexed { index, model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                model.modelName,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (canEditModels) {
                                IconButton(
                                    onClick = { vm.moveModelUp(model.id) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("▲", fontSize = 14.sp)
                                }
                                IconButton(
                                    onClick = { vm.moveModelDown(model.id) },
                                    enabled = index < data.honorModels.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("▼", fontSize = 14.sp)
                                }
                                IconButton(
                                    onClick = { vm.deleteHonorModel(model.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, "Удалить",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        // ============ ПРОДАЖИ ============
        item(key = "sales_header") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Продажи за месяц (${data.sales.size})",
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (!isReadOnly && data.sales.isNotEmpty()) {
                        TextButton(onClick = { showClearAllConfirm = true }) {
                            Text("Очистить", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (data.sales.isEmpty()) {
            item(key = "sales_empty") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Нет продаж за этот месяц",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            val salesByDate = data.sales
                .groupBy { it.saleDate }
                .toSortedMap(compareByDescending { it })

            salesByDate.forEach { (date, salesOfDay) ->
                item(key = "date_header_$date") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                MonthUtils.dateDisplay(date),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "${salesOfDay.sumOf { it.quantity }} шт.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                itemsIndexed(
                    salesOfDay,
                    key = { idx, sale -> sale.id ?: "sale_${date}_$idx" }
                ) { _, sale ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (sale.brand == "Honor") sale.model ?: sale.brand else sale.brand,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    sale.brand,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${sale.quantity} шт.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            if (!isReadOnly) {
                                IconButton(
                                    onClick = { sale.id?.let { saleToDelete = it } },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, "Удалить",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ============ ВЕРСИЯ ПРИЛОЖЕНИЯ ============
        item(key = "app_version") {
            Text(
                text = "Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(24.dp)) }
    }

    // ============ ДИАЛОГИ ============
    if (showAddModelDialog && canEditModels) {
        AddHonorModelDialog(
            onDismiss = { showAddModelDialog = false },
            onSave = { name ->
                vm.addHonorModel(
                    name = name,
                    onDone = { showAddModelDialog = false },
                    onError = { errorMsg = it }
                )
            }
        )
    }

    saleToDelete?.let { id ->
        if (!isReadOnly) {
            ConfirmDialog(
                title = "Удалить продажу?",
                message = "Это действие нельзя отменить.",
                confirmText = "Удалить",
                onDismiss = { saleToDelete = null },
                onConfirm = {
                    vm.deleteSale(id)
                    saleToDelete = null
                }
            )
        }
    }

    if (showClearAllConfirm && !isReadOnly) {
        ConfirmDialog(
            title = "Очистить все продажи?",
            message = "Будут удалены ВСЕ ${data.sales.size} продаж за ${MonthUtils.monthDisplay(data.month)}.",
            confirmText = "Очистить",
            onDismiss = { showClearAllConfirm = false },
            onConfirm = { vm.deleteAllSales { showClearAllConfirm = false } }
        )
    }
}

@Composable
private fun DeveloperFlagsCard() {
    val flags by by.grinich.hscontrol.data.FeatureFlagsState.flags.collectAsState()
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Управление функциями (Developer)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Включение/выключение функций для всех пользователей приложения",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.height(12.dp))

            if (flags.isEmpty()) {
                Text(
                    "Загрузка...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            } else {
                flags.values.forEach { flag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                flag.title ?: flag.key,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            if (flag.message != null) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    flag.message,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = flag.enabled,
                            onCheckedChange = { newValue ->
                                scope.launch {
                                    by.grinich.hscontrol.data.FeatureFlagsState
                                        .setFlag(flag.key, newValue)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddHonorModelDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить модель Honor") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название модели") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.trim().isNotEmpty()) onSave(name.trim())
            }) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}