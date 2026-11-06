package by.grinich.hscontrol.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import by.grinich.hscontrol.auth.UserProfile

@Composable
fun DashboardTab(
    profile: UserProfile,
    month: String,
    refreshKey: Int = 0
) {
    val vm: DashboardViewModel = viewModel()
    val state by vm.state.collectAsState()

    // LaunchedEffect срабатывает при каждом появлении вкладки в дереве,
    // потому что при переключении табов composable удаляется и создаётся заново.
    LaunchedEffect(profile.id, month, refreshKey) {
        vm.load(profile.id, month)
    }

    when (val s = state) {
        is DashboardState.Loading -> LoadingState()
        is DashboardState.Error -> ErrorState(s.message) { vm.refresh() }
        is DashboardState.Success -> DashboardContent(s)
    }
}

@Composable
private fun DashboardContent(data: DashboardState.Success) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (data.version == 1) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("План по Honor", "🎯")
                        val plan = 30
                        val progress = if (plan > 0) data.totalHonor * 100.0 / plan else 0.0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Продано: ${data.totalHonor} из $plan шт.")
                            Text(String.format("%.2f", progress) + "%")
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (progress / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        value = data.totalHonor.toString(),
                        label = "Продано Honor",
                        valueColor = Color(0xFFFF2D55),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = data.totalAll.toString(),
                        label = "Всего телефонов",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (data.version >= 2 && data.share != null) {
            item { SectionTitle("Доля Honor", "📊") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = "${data.share.currentShare}%",
                        label = "Текущая доля",
                        valueColor = Color(0xFFFF2D55),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${data.share.targetShare}%",
                        label = "Целевая доля",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = "${data.share.shareDiff}%",
                        label = "Перевып.",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow("Выплата за долю:",
                            "%.2f BYN".format(data.share.sharePayment),
                            bold = true, valueColor = Color(0xFF34C759))
                        InfoRow("Базовая выплата:", "%.0f BYN".format(data.share.targetBonus))
                        InfoRow("Бонус за 1% перевып.:", "%.0f BYN".format(data.share.overBonus))
                    }
                }
            }
        }

        if (data.version == 3 && data.premium != null) {
            item { SectionTitle("Premium мотивация", "👑") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = String.format("%.2f", data.premium.points),
                        label = "Баллы",
                        valueColor = Color(0xFFFF2D55),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = String.format("%.1f%%", data.premium.percent),
                        label = "Процент",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = when (data.premium.tier) {
                            2 -> "Вторая"
                            1 -> "Первая"
                            else -> "Ниже"
                        },
                        label = "Ступень",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow("Premium выплата:",
                            "%.2f BYN".format(data.premium.payout),
                            bold = true, valueColor = Color(0xFF34C759))
                        InfoRow("Tar 1 / Tar 2:",
                            "${data.premium.target1.toInt()}% / ${data.premium.target2.toInt()}%")
                        InfoRow("Bonus 1 / Bonus 2:",
                            "%.0f / %.0f BYN".format(data.premium.bonus1, data.premium.bonus2))
                        if (data.premium.extraPay > 0) {
                            InfoRow("Сверх таргета:",
                                "%.2f BYN".format(data.premium.extraPay))
                        }
                    }
                }
            }
        }

        item { SectionTitle("Общие продажи", "📱") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    value = data.totalHonor.toString(),
                    label = "Honor",
                    valueColor = Color(0xFFFF2D55),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = (data.totalAll - data.totalHonor).toString(),
                    label = "Конкуренты",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = data.totalAll.toString(),
                    label = "Всего",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (data.brandStats.isNotEmpty()) {
            item { SectionTitle("Доли брендов", "🏢") }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        data.brandStats.forEach { stat ->
                            BrandProgressBar(
                                brand = stat.brand,
                                quantity = stat.quantity,
                                percent = stat.percent,
                                color = when (stat.brand) {
                                    "Honor" -> Color(0xFFFF2D55)
                                    "Xiaomi+POCO" -> Color(0xFFFF6900)
                                    "Samsung" -> Color(0xFF1428A0)
                                    "Apple" -> Color(0xFFA2AAAD)
                                    "Infinix" -> Color(0xFF00B0FF)
                                    "TECNO" -> Color(0xFF00838F)
                                    else -> Color(0xFF8E8E93)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (data.sales.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📭", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Нет продаж за этот месяц",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}