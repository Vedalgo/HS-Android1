package by.grinich.hscontrol.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import by.grinich.hscontrol.utils.MonthUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    userId: String,
    month: String,
    onBack: () -> Unit
) {
    val vm: MonthlyReportViewModel = viewModel()
    val state by vm.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(userId, month) { vm.load(userId, month) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отчёт для таблицы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (state is ReportState.Success) {
                        IconButton(onClick = {
                            val r = (state as ReportState.Success).report
                            val text = buildReportText(month, r)
                            clipboard.setText(AnnotatedString(text))
                        }) {
                            Icon(Icons.Default.ContentCopy, "Скопировать")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val s = state) {
                is ReportState.Loading -> LoadingState()
                is ReportState.Error -> ErrorState(s.message) { vm.load(userId, month) }
                is ReportState.Success -> ReportContent(s.month, s.report)
            }
        }
    }
}

@Composable
private fun ReportContent(month: String, r: by.grinich.hscontrol.data.MonthlyReport) {
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
                Text("Отчёт за ${MonthUtils.monthDisplay(month)}",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("Нажмите на иконку справа сверху, чтобы скопировать текст",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        // I половина
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("1–${r.midMonthDay} ${MonthUtils.monthDisplay(month).substringBefore(" ").lowercase()}",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                InfoRow("Honor:", "${r.honorFirstHalf} шт.")
                InfoRow("Конкуренты:", "${r.competitorsFirstHalf} шт.")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow("Итого:",
                    "${r.honorFirstHalf + r.competitorsFirstHalf} шт.",
                    bold = true)
            }
        }

        Spacer(Modifier.height(12.dp))

        // II половина
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("${r.midMonthDay + 1}–конец ${MonthUtils.monthDisplay(month).substringBefore(" ").lowercase()}",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                InfoRow("Honor:", "${r.honorSecondHalf} шт.")
                InfoRow("Конкуренты:", "${r.competitorsSecondHalf} шт.")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow("Итого:",
                    "${r.honorSecondHalf + r.competitorsSecondHalf} шт.",
                    bold = true)
            }
        }

        Spacer(Modifier.height(12.dp))

        // За весь месяц
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("За весь месяц",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                InfoRow("Всего Honor:", "${r.honorFirstHalf + r.honorSecondHalf} шт.")
                InfoRow("Всего конкурентов:",
                    "${r.competitorsFirstHalf + r.competitorsSecondHalf} шт.")
                InfoRow("Общее количество:", "${r.totalAll} шт.")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow("Xiaomi+POCO:", "${r.xiaomiPocoTotal} шт.",
                    bold = true,
                    valueColor = MaterialTheme.colorScheme.primary)
                InfoRow("Samsung:", "${r.samsungTotal} шт.")
                InfoRow("Apple:", "${r.appleTotal} шт.")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun buildReportText(month: String, r: by.grinich.hscontrol.data.MonthlyReport): String {
    val monthName = MonthUtils.monthDisplay(month)
    val monthShort = monthName.substringBefore(" ").lowercase()
    return buildString {
        appendLine("Отчёт за $monthName")
        appendLine()
        appendLine("1–${r.midMonthDay} $monthShort:")
        appendLine("  Honor: ${r.honorFirstHalf}")
        appendLine("  Конкуренты: ${r.competitorsFirstHalf}")
        appendLine()
        appendLine("${r.midMonthDay + 1}–конец $monthShort:")
        appendLine("  Honor: ${r.honorSecondHalf}")
        appendLine("  Конкуренты: ${r.competitorsSecondHalf}")
        appendLine()
        appendLine("За месяц:")
        appendLine("  Всего Honor: ${r.honorFirstHalf + r.honorSecondHalf}")
        appendLine("  Всего конкурентов: ${r.competitorsFirstHalf + r.competitorsSecondHalf}")
        appendLine("  Xiaomi+POCO: ${r.xiaomiPocoTotal}")
        appendLine("  Samsung: ${r.samsungTotal}")
        appendLine("  Apple: ${r.appleTotal}")
        appendLine("  Общее: ${r.totalAll}")
    }
}