package by.grinich.hscontrol.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import by.grinich.hscontrol.auth.UserProfile
import by.grinich.hscontrol.data.PromoterStats
import by.grinich.hscontrol.utils.MonthUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    currentProfile: UserProfile,
    onSelectPromoter: (UserProfile) -> Unit,
    onBack: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null
) {
    val vm: TeamViewModel = viewModel()
    val state by vm.state.collectAsState()
    var currentMonth by remember { mutableStateOf(MonthUtils.currentMonth()) }

    LaunchedEffect(currentMonth) { vm.load(currentMonth) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (onBack != null) "Команда" else "Мои промоутеры") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    }
                },
                actions = {
                    if (onSignOut != null) {
                        IconButton(onClick = onSignOut) {
                            Icon(Icons.Default.Logout, contentDescription = "Выйти")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { currentMonth = MonthUtils.shiftMonth(currentMonth, -1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Предыдущий месяц")
                    }
                    Text(
                        MonthUtils.monthDisplay(currentMonth),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { currentMonth = MonthUtils.shiftMonth(currentMonth, +1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Следующий месяц")
                    }
                }
            }

            when (val s = state) {
                is TeamState.Loading -> LoadingState()
                is TeamState.Error -> ErrorState(s.message) { vm.refresh() }
                is TeamState.Success -> TeamList(
                    month = s.month,
                    promoters = s.promoters.filter { it.id != currentProfile.id },
                    onSelect = { p ->
                        val profile = UserProfile(
                            id = p.id,
                            email = p.email,
                            fullName = p.fullName,
                            role = p.role,
                            supervisorId = p.supervisorId
                        )
                        onSelectPromoter(profile)
                    }
                )
            }
        }
    }
}

@Composable
private fun TeamList(
    month: String,
    promoters: List<PromoterStats>,
    onSelect: (PromoterStats) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Показатели за месяц",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(MonthUtils.monthDisplay(month),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("Всего: ${promoters.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("Нажмите на промоутера, чтобы посмотреть подробно",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (promoters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет промоутеров для отображения",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(promoters) { _, p ->
                    PromoterRow(promoter = p, onClick = { onSelect(p) })
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun PromoterRow(
    promoter: PromoterStats,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = when (promoter.role) {
                    "developer" -> MaterialTheme.colorScheme.tertiaryContainer
                    "supervisor" -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = promoter.fullName?.ifBlank { promoter.email ?: "—" }
                        ?: promoter.email ?: "—",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when (promoter.role) {
                        "developer" -> "Разработчик"
                        "supervisor" -> "Супервайзер"
                        else -> "Промоутер"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Honor: ${promoter.totalHonor}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Доля: ${promoter.currentShare}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${String.format("%.0f", promoter.totalSalary)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "BYN",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}