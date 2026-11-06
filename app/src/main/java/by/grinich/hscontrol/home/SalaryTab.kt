package by.grinich.hscontrol.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import by.grinich.hscontrol.auth.UserProfile
import by.grinich.hscontrol.utils.MonthUtils

@Composable
fun SalaryTab(
    profile: UserProfile,
    month: String,
    isReadOnly: Boolean = false
) {
    val vm: SalaryViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(profile.id, month) {
        vm.load(profile.id, month)
    }

    when (val s = state) {
        is SalaryState.Loading -> LoadingState()
        is SalaryState.Error -> ErrorState(s.message) { vm.refresh() }
        is SalaryState.Success -> SalaryContent(s, vm, isReadOnly)
    }
}

@Composable
private fun SalaryContent(
    data: SalaryState.Success,
    vm: SalaryViewModel,
    isReadOnly: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Расчёт заработной платы",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(MonthUtils.monthDisplay(data.month),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text("Версия мотивации: v${data.version}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Компоненты",
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))

                when (data.version) {
                    1 -> SalaryV1Components(data)
                    2 -> SalaryV2Components(data)
                    else -> SalaryV3Components(data)
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Сдал тест?", fontSize = 14.sp)
                    Switch(
                        checked = data.testPassed,
                        onCheckedChange = {
                            if (!isReadOnly) vm.setTestPassed(it)
                        },
                        enabled = !isReadOnly
                    )
                }

                if (data.testPassed) {
                    InfoRow(
                        "Бонус за тест:",
                        "${String.format("%.2f", data.salary.testBonus)} BYN",
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Аванс",
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))

                AutoAmountInput(
                    label = "Сумма аванса, BYN",
                    value = data.advanceAmount,
                    enabled = !isReadOnly,
                    onValueChange = { vm.setAdvanceAmount(it) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(
                    "Итого к выплате:",
                    "${String.format("%.2f", data.salary.totalSalary)} BYN",
                    bold = true
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("К выплате 10-го:",
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "${String.format("%.2f", data.payment10th)} BYN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Сумма после пересмотра",
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text("Оставьте 0, если пересмотр не требуется",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                AutoAmountInput(
                    label = "Сумма после пересмотра, BYN",
                    value = data.adjustedSalary,
                    enabled = !isReadOnly,
                    onValueChange = { vm.setAdjustedSalary(it) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (data.adjustedSalary > 0)
                        "К выплате после пересмотра:"
                    else
                        "К выплате 10-го:",
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${String.format("%.2f", data.paymentAfterAdjustment)} BYN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Детали",
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))

                InfoRow("Honor продано:", "${data.totalHonor} шт.")
                InfoRow("Всего продано:", "${data.totalAll} шт.")
                InfoRow("Текущая доля:", "${data.currentShare}%")

                if (data.version == 3) {
                    InfoRow("Premium баллы:",
                        String.format("%.2f", data.premiumPoints))
                    InfoRow("Premium %:",
                        "${String.format("%.2f", data.premiumPercent)}%")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val brands = listOf(
                    "Xiaomi", "POCO", "Samsung", "Apple",
                    "Infinix", "TECNO"
                )
                brands.forEach { brand ->
                    val qty = data.brandStats.find { it.brand == brand }?.quantity ?: 0
                    InfoRow("$brand:", "$qty шт.")
                }

                val knownBrands = setOf(
                    "Honor", "Xiaomi", "POCO", "Samsung", "Apple",
                    "Infinix", "TECNO"
                )
                val otherQty = data.brandStats
                    .filter { it.brand !in knownBrands }
                    .sumOf { it.quantity }
                InfoRow("Другие:", "$otherQty шт.")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SalaryV1Components(data: SalaryState.Success) {
    InfoRow("Оклад:", "${String.format("%.2f", data.salary.baseSalary)} BYN")
    InfoRow("Бонусы за фокусные позиции:",
        "${String.format("%.2f", data.salary.focusBonus)} BYN")
    InfoRow("Выплата за план:",
        "${String.format("%.2f", data.salary.sharePayment)} BYN")
}

@Composable
private fun SalaryV2Components(data: SalaryState.Success) {
    InfoRow("Оклад:", "${String.format("%.2f", data.salary.baseSalary)} BYN")
    InfoRow("Бонус за смартфоны (фокусы):",
        "${String.format("%.2f", data.salary.focusBonus)} BYN")
    InfoRow("Бонус за IOT:",
        "${String.format("%.2f", data.salary.iotBonus)} BYN")
    InfoRow("Выплата за долю:",
        "${String.format("%.2f", data.salary.sharePayment)} BYN")
}

@Composable
private fun SalaryV3Components(data: SalaryState.Success) {
    InfoRow("Оклад:", "${String.format("%.2f", data.salary.baseSalary)} BYN")
    InfoRow("Мобильная связь:",
        "${String.format("%.2f", data.salary.mobilePayment)} BYN")
    InfoRow("Выплата за долю Honor:",
        "${String.format("%.2f", data.salary.sharePayment)} BYN")
    InfoRow("Бонус Premium:",
        "${String.format("%.2f", data.salary.premiumPayout)} BYN")
    InfoRow("Бонус IOT:",
        "${String.format("%.2f", data.salary.iotBonus)} BYN")
}

/**
 * Поле для ввода суммы с автосохранением (без кнопки OK).
 * Локальный текст обновляется мгновенно, а value отправляется на сервер
 * с debounce (в VM). Сохранение происходит при остановке ввода.
 */
@Composable
private fun AutoAmountInput(
    label: String,
    value: Double,
    enabled: Boolean,
    onValueChange: (Double) -> Unit
) {
    // Инициализация при первом появлении или смене value снаружи
    var text by remember { mutableStateOf(valueToText(value)) }
    // Синхронизация при обновлении value из VM
    LaunchedEffect(value) {
        val parsedCurrent = text.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (parsedCurrent != value) {
            text = valueToText(value)
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            if (enabled) {
                val filtered = new.filter { it.isDigit() || it == '.' || it == ',' }
                text = filtered
                val v = filtered.replace(",", ".").toDoubleOrNull()
                if (v != null) onValueChange(v)
            }
        },
        label = { Text(label) },
        placeholder = { Text("0.00") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun valueToText(v: Double): String {
    return when {
        v == 0.0 -> ""
        v % 1.0 == 0.0 -> v.toLong().toString()
        else -> v.toString()
    }
}