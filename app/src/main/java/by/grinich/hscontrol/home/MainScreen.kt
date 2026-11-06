package by.grinich.hscontrol.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.grinich.hscontrol.auth.UserProfile
import by.grinich.hscontrol.data.FeatureFlag
import by.grinich.hscontrol.data.FeatureFlagsState
import by.grinich.hscontrol.utils.MonthUtils
import androidx.compose.ui.text.style.TextOverflow

private data class TabItem(
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    profile: UserProfile,
    isReadOnly: Boolean = false,
    onBackFromView: (() -> Unit)? = null,
    onOpenTeam: (() -> Unit)? = null,
    onOpenHistory: () -> Unit,
    onOpenReport: () -> Unit,
    onSignOut: () -> Unit,
    darkMode: Boolean?,
    onDarkModeChange: (Boolean?) -> Unit
) {
    var currentMonth by remember { mutableStateOf(MonthUtils.currentMonth()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSale by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    var blockedDialog by remember { mutableStateOf<FeatureFlag?>(null) }

    val flags by FeatureFlagsState.flags.collectAsState()
    val isDeveloper = profile.role == "developer"
    val premiumEnabled = flags["premium_enabled"]?.enabled ?: true

    // Если Premium выключили — увести с этой вкладки
    LaunchedEffect(premiumEnabled, isDeveloper, isReadOnly) {
        if (selectedTab == 1 && !premiumEnabled && !isDeveloper) {
            selectedTab = 0
        }
    }

    val tabs = listOf(
        TabItem("Дашборд", Icons.Default.Dashboard),
        TabItem("Premium", Icons.Default.Star),
        TabItem("IOT", Icons.Default.Info),
        TabItem("Зарплата", Icons.Default.Menu),
        TabItem("Настройки", Icons.Default.Settings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBackFromView != null) {
                        IconButton(onClick = onBackFromView) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    }
                },
                title = {
                    Column {
                        val displayName = profile.fullName
                            ?.takeIf { it.isNotBlank() }
                            ?: profile.email?.substringBefore("@")
                            ?: "Пользователь"

                        Text(
                            text = if (isReadOnly) displayName else "HS-Control",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isReadOnly) {
                                "Просмотр · ${profile.roleDisplay}"
                            } else {
                                "$displayName · ${profile.roleDisplay}"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    if (onOpenTeam != null) {
                        IconButton(onClick = onOpenTeam) {
                            Icon(Icons.Default.AccountBox, "Команда")
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Menu, "Меню")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("История продаж") },
                                leadingIcon = { Icon(Icons.Default.History, null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenHistory()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Отчёт для таблицы") },
                                leadingIcon = { Icon(Icons.Default.Assessment, null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenReport()
                                }
                            )
                            if (!isReadOnly) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Выйти") },
                                    leadingIcon = { Icon(Icons.Default.Logout, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onSignOut()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            // Перехват Premium
                            if (index == 1 && !isDeveloper && !isReadOnly && !premiumEnabled) {
                                blockedDialog = flags["premium_enabled"]
                                    ?: FeatureFlag(
                                        key = "premium_enabled",
                                        enabled = false,
                                        title = "Premium мотивация в разработке",
                                        message = "Функционал временно заблокирован."
                                    )
                            } else {
                                selectedTab = index
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 10.sp) },
                        alwaysShowLabel = true
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isReadOnly && selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddSale = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить продажу")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            MonthSelector(
                month = currentMonth,
                onPrev = { currentMonth = MonthUtils.shiftMonth(currentMonth, -1) },
                onNext = { currentMonth = MonthUtils.shiftMonth(currentMonth, +1) }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> DashboardTab(profile, currentMonth, refreshKey)
                    1 -> PremiumTab(profile, currentMonth, isReadOnly)
                    2 -> IotTab(profile, currentMonth, isReadOnly)
                    3 -> SalaryTab(profile, currentMonth, isReadOnly)
                    4 -> SettingsTab(profile, currentMonth, darkMode, onDarkModeChange, isReadOnly)
                }
            }
        }
    }

    // Диалог блокировки
    blockedDialog?.let { flag ->
        AlertDialog(
            onDismissRequest = { blockedDialog = null },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = {
                Text(flag.title ?: "Функционал в разработке",
                    fontWeight = FontWeight.Bold)
            },
            text = {
                Text(flag.message
                    ?: "Функционал временно заблокирован. Разработчик работает над улучшением.")
            },
            confirmButton = {
                TextButton(onClick = { blockedDialog = null }) {
                    Text("Понятно")
                }
            }
        )
    }

    if (showAddSale) {
        AddSaleDialog(
            userId = profile.id,
            month = currentMonth,
            onDismiss = { showAddSale = false },
            onSaved = {
                showAddSale = false
                refreshKey++
            }
        )
    }
}

@Composable
private fun MonthSelector(
    month: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
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
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Предыдущий месяц")
            }
            Text(
                text = MonthUtils.monthDisplay(month),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Следующий месяц")
            }
        }
    }
}