package by.grinich.hscontrol.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import by.grinich.hscontrol.auth.UserProfile
import by.grinich.hscontrol.data.FocusItem
import by.grinich.hscontrol.data.PremiumItem
import by.grinich.hscontrol.utils.MonthUtils

@Composable
fun PremiumTab(
    profile: UserProfile,
    month: String,
    isReadOnly: Boolean = false
) {
    val vm: PremiumViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(profile.id, month) {
        vm.load(profile.id, month)
    }

    when (val s = state) {
        is PremiumState.Loading -> LoadingState()
        is PremiumState.Error -> ErrorState(s.message) { vm.refresh() }
        is PremiumState.Success -> {
            when (s.version) {
                3 -> PremiumV3Content(profile, s, vm, isReadOnly)
                else -> FocusV1V2Content(profile, s, vm, isReadOnly)
            }
        }
    }
}

@Composable
private fun PremiumV3Content(
    profile: UserProfile,
    data: PremiumState.Success,
    vm: PremiumViewModel,
    isReadOnly: Boolean
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PremiumItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (data.premiumResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Расчёт Premium", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Баллы:", String.format("%.2f", data.premiumResult.points))
                    InfoRow("Honor / план:", "${data.premiumResult.totalHonor} / ${String.format("%.1f", data.premiumResult.requiredHonor)}")
                    InfoRow("Tar 1:", "${data.premiumResult.target1.toInt()}% (${String.format("%.1f", data.premiumResult.requiredHonor * data.premiumResult.target1 / 100)} шт.)")
                    InfoRow("Tar 2:", "${data.premiumResult.target2.toInt()}% (${String.format("%.1f", data.premiumResult.requiredHonor * data.premiumResult.target2 / 100)} шт.)")
                    InfoRow("Ступень:", when (data.premiumResult.tier) {
                        2 -> "Вторая (≥ Tar 2)"
                        1 -> "Первая (≥ Tar 1)"
                        else -> "Ниже Tar 1"
                    })
                    if (data.premiumResult.extraPay > 0) {
                        InfoRow("Сверх таргета:", "+${String.format("%.2f", data.premiumResult.extraPay)} BYN")
                    }
                    if (data.premiumResult.fixedPay > 0) {
                        InfoRow("Фикс за модели:", "+${String.format("%.2f", data.premiumResult.fixedPay)} BYN")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Выплата Premium:", "${String.format("%.2f", data.premiumResult.payout)} BYN", bold = true)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (!isReadOnly) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showAddDialog = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Модель")
                }
                OutlinedButton(onClick = { showSettingsDialog = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Настройки")
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (data.premiumItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет Premium-моделей" + if (!isReadOnly) ". Добавьте первую." else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(data.premiumItems, key = { _, it -> it.id ?: it.name }) { _, item ->
                    PremiumItemRow(
                        item = item,
                        isReadOnly = isReadOnly,
                        onSoldDelta = { delta -> item.id?.let { vm.updatePremiumSold(it, delta) } },
                        onEdit = { editingItem = item },
                        onDelete = { item.id?.let { vm.deletePremiumItem(it) } }
                    )
                }
            }
        }
    }

    if (showAddDialog && !isReadOnly) {
        AddPremiumItemDialog(
            models = data.honorModels.map { it.modelName },
            existingNames = data.premiumItems.map { it.name.lowercase() }.toSet(),
            onDismiss = { showAddDialog = false },
            onSave = { name, rate, fixedBonus ->
                vm.addPremiumItem(
                    userId = profile.id,
                    month = data.month,
                    name = name,
                    rate = rate,
                    fixedBonus = fixedBonus,
                    onDone = { showAddDialog = false },
                    onError = { }
                )
            }
        )
    }

    if (showSettingsDialog && !isReadOnly) {
        val ps = data.premiumSettings
        PremiumSettingsDialog(
            initialBonus1 = ps?.bonus1 ?: 400.0,
            initialBonus2 = ps?.bonus2 ?: 500.0,
            initialTarget1 = ps?.target1 ?: 20.0,
            initialTarget2 = ps?.target2 ?: 30.0,
            initialExtra = ps?.extraPerPercent ?: 10.0,
            onDismiss = { showSettingsDialog = false },
            onSave = { b1, b2, t1, t2, ex ->
                vm.savePremiumSettings(
                    userId = profile.id, month = data.month,
                    bonus1 = b1, bonus2 = b2, target1 = t1, target2 = t2,
                    extraPerPercent = ex
                ) { showSettingsDialog = false }
            }
        )
    }

    editingItem?.let { item ->
        if (!isReadOnly) {
            EditPremiumDialog(
                item = item,
                onDismiss = { editingItem = null },
                onSave = { newRate, newFixed ->
                    item.id?.let {
                        vm.updatePremiumRate(it, newRate)
                        vm.updatePremiumFixed(it, newFixed)
                    }
                    editingItem = null
                }
            )
        }
    }
}

@Composable
private fun PremiumItemRow(
    item: PremiumItem,
    isReadOnly: Boolean,
    onSoldDelta: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Rate: ×${item.rate}" +
                                    if (item.fixedBonus > 0) " | Фикс: +${item.fixedBonus} BYN" else "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isReadOnly) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Изменить",
                                    modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isReadOnly) {
                        IconButton(onClick = { onSoldDelta(-1) }, modifier = Modifier.size(36.dp)) {
                            Text("−", fontSize = 20.sp)
                        }
                    }
                    Text(
                        item.sold.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.Center
                    )
                    if (!isReadOnly) {
                        IconButton(onClick = { onSoldDelta(+1) }, modifier = Modifier.size(36.dp)) {
                            Text("+", fontSize = 20.sp)
                        }
                    }
                }

                if (!isReadOnly) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            val points = item.rate * item.sold
            val fixed = item.fixedBonus * item.sold
            Text(
                text = "Баллы: ${String.format("%.2f", points)} = ${item.rate} × ${item.sold}" +
                        if (fixed > 0) " | Фикс: ${String.format("%.2f", fixed)} BYN" else "",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FocusV1V2Content(
    profile: UserProfile,
    data: PremiumState.Success,
    vm: PremiumViewModel,
    isReadOnly: Boolean
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<FocusItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    if (data.version == 2) "Фокусные смартфоны" else "Фокусные позиции",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (data.version == 2)
                        "Только смартфоны Honor. Носимые — во вкладке IOT."
                    else
                        "Любые категории: смартфоны, наушники, планшеты, часы, другое.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val totalBonus = data.focusItems.sumOf { it.bonus * it.sold }
                InfoRow("Всего бонусов:", "${String.format("%.2f", totalBonus)} BYN", bold = true)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (!isReadOnly) {
            Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (data.version == 2) "Добавить смартфон" else "Добавить позицию")
            }
            Spacer(Modifier.height(12.dp))
        }

        if (data.focusItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет фокусных позиций", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val grouped = data.focusItems.groupBy { it.category }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                grouped.forEach { (category, items) ->
                    item(key = "cat_$category") {
                        Text(category, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    }
                    itemsIndexed(items, key = { _, it -> it.id ?: it.name }) { _, item ->
                        FocusItemRow(
                            item = item,
                            isReadOnly = isReadOnly,
                            onSoldDelta = { delta -> item.id?.let { vm.updateFocusSold(it, delta) } },
                            onEditBonus = { editingItem = item },
                            onDelete = { item.id?.let { vm.deleteFocusItem(it) } }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog && !isReadOnly) {
        AddFocusItemDialog(
            version = data.version,
            onDismiss = { showAddDialog = false },
            onSave = { category, name, bonus ->
                vm.addFocusItem(
                    userId = profile.id, month = data.month,
                    category = category, name = name, bonus = bonus,
                    onDone = { showAddDialog = false }, onError = { }
                )
            }
        )
    }

    editingItem?.let { item ->
        if (!isReadOnly) {
            EditBonusDialog(
                currentBonus = item.bonus,
                itemName = item.name,
                onDismiss = { editingItem = null },
                onSave = { newBonus ->
                    item.id?.let { vm.updateFocusBonus(it, newBonus) }
                    editingItem = null
                }
            )
        }
    }
}

@Composable
private fun FocusItemRow(
    item: FocusItem,
    isReadOnly: Boolean,
    onSoldDelta: (Int) -> Unit,
    onEditBonus: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Бонус: ${item.bonus} BYN/шт.", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!isReadOnly) {
                        IconButton(onClick = onEditBonus, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("= ${String.format("%.2f", item.bonus * item.sold)} BYN",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isReadOnly) {
                    IconButton(onClick = { onSoldDelta(-1) }, modifier = Modifier.size(36.dp)) {
                        Text("−", fontSize = 20.sp)
                    }
                }
                Text(
                    item.sold.toString(),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.Center
                )
                if (!isReadOnly) {
                    IconButton(onClick = { onSoldDelta(+1) }, modifier = Modifier.size(36.dp)) {
                        Text("+", fontSize = 20.sp)
                    }
                }
            }
            if (!isReadOnly) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}