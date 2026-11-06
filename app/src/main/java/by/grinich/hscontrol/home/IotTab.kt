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
fun IotTab(
    profile: UserProfile,
    month: String,
    isReadOnly: Boolean = false
) {
    val vm: IotViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(profile.id, month) {
        vm.load(profile.id, month)
    }

    when (val s = state) {
        is IotState.Loading -> LoadingState()
        is IotState.NotAvailable -> IotNotAvailable(month)
        is IotState.Error -> ErrorState(s.message) { vm.refresh() }
        is IotState.Success -> IotContent(s, vm, isReadOnly)
    }
}

@Composable
private fun IotNotAvailable(month: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("ℹ️", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text("Мотивация IOT недоступна",
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Доступно с июня 2026",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("Текущий месяц: ${MonthUtils.monthDisplay(month)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IotContent(
    data: IotState.Success,
    vm: IotViewModel,
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
                Text("Носимые устройства (IOT)",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                InfoRow("Канал:", data.channel)
                InfoRow("Тип канала:", when (data.channelType) {
                    "OM" -> "OM (Открытый рынок)"
                    else -> "OP (Операторы)"
                })
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isReadOnly)
                        "Просмотр продаж за месяц."
                    else
                        "Введите сумму продаж за месяц по каждой категории. Сохраняется автоматически.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        data.categories.forEach { category ->
            IotCategoryCard(
                category = category,
                isReadOnly = isReadOnly,
                onAmountChange = { vm.saveIot(category.id, it) }
            )
            Spacer(Modifier.height(10.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Итого бонус за IOT:",
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${String.format("%.2f", data.totalBonus)} BYN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun IotCategoryCard(
    category: IotCategory,
    isReadOnly: Boolean,
    onAmountChange: (Double) -> Unit
) {
    // Локальное значение, синхронизируется с сервером через LaunchedEffect
    var text by remember(category.amount) {
        mutableStateOf(
            if (category.amount == 0.0) ""
            else if (category.amount % 1.0 == 0.0) category.amount.toLong().toString()
            else category.amount.toString()
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category.label,
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${category.percent}%",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { newText ->
                    if (!isReadOnly) {
                        val filtered = newText.filter { it.isDigit() || it == '.' || it == ',' }
                        text = filtered
                        val v = filtered.replace(",", ".").toDoubleOrNull() ?: 0.0
                        onAmountChange(v)
                    }
                },
                label = { Text("Сумма продаж, BYN") },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !isReadOnly,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Бонус:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${String.format("%.2f", category.bonus)} BYN",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}