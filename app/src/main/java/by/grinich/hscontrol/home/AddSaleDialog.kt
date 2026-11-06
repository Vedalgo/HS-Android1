package by.grinich.hscontrol.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import by.grinich.hscontrol.utils.MonthUtils
import java.util.Calendar
import java.util.TimeZone
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleDialog(
    userId: String,
    month: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val vm: AddSaleViewModel = viewModel()
    val models by vm.models.collectAsState()
    val quantities by vm.quantities.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val isSaving by vm.isSaving.collectAsState()
    val error by vm.error.collectAsState()

    var selectedBrand by remember { mutableStateOf("Honor") }
    var saleDate by remember { mutableStateOf(MonthUtils.defaultDateForMonth(month)) }
    var otherQuantity by remember { mutableIntStateOf(1) }
    var localError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var brandDropdownOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadModels() }

    val brandsList = listOf(
        "Honor", "Apple", "Samsung", "Xiaomi", "POCO",
        "Infinix", "TECNO", "Huawei", "Другой"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Добавить продажу",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                HorizontalDivider()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    onClick = { showDatePicker = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Дата", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                MonthUtils.dateDisplay(saleDate),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedButton(
                        onClick = { brandDropdownOpen = true },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Бренд: $selectedBrand")
                            Text("▼", fontSize = 12.sp)
                        }
                    }
                    DropdownMenu(
                        expanded = brandDropdownOpen,
                        onDismissRequest = { brandDropdownOpen = false }
                    ) {
                        brandsList.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b) },
                                onClick = {
                                    selectedBrand = b
                                    brandDropdownOpen = false
                                    localError = null
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (selectedBrand == "Honor") {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                Text(
                                    "Выберите модели Honor",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            itemsIndexed(models) { _, model ->
                                val qty = quantities[model.modelName] ?: 0
                                HonorModelRow(
                                    name = model.modelName,
                                    quantity = qty,
                                    onMinus = { vm.changeQuantity(model.modelName, -1) },
                                    onPlus = { vm.changeQuantity(model.modelName, +1) },
                                    enabled = !isSaving
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            "Количество",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))

                        var quantityText by remember { mutableStateOf(otherQuantity.toString()) }

                        // Синхронизация при нажатии +/-
                        LaunchedEffect(otherQuantity) {
                            val current = quantityText.toIntOrNull()
                            if (current != otherQuantity) {
                                quantityText = otherQuantity.toString()
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Кнопка "−"
                            FilledTonalIconButton(
                                onClick = { otherQuantity = (otherQuantity - 1).coerceAtLeast(1) },
                                enabled = !isSaving && otherQuantity > 1,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Уменьшить",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Поле ввода
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { new ->
                                    val filtered = new.filter { it.isDigit() }.take(4)
                                    quantityText = filtered
                                    val v = filtered.toIntOrNull()
                                    if (v != null && v >= 1) {
                                        otherQuantity = v
                                    }
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                singleLine = true,
                                enabled = !isSaving,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            )

                            // Кнопка "+"
                            FilledTonalIconButton(
                                onClick = { otherQuantity += 1 },
                                enabled = !isSaving,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Увеличить",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Можно вводить с клавиатуры. Минимум 1 шт.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                (error ?: localError)?.let { err ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp
                        )
                    }
                }

                if (selectedBrand == "Honor") {
                    val total = quantities.values.sum()
                    if (total > 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Итого Honor:", fontWeight = FontWeight.SemiBold)
                                Text("$total шт.", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            localError = null
                            if (selectedBrand == "Honor") {
                                vm.saveHonorSales(
                                    userId, saleDate,
                                    onSuccess = onSaved,
                                    onError = { localError = it }
                                )
                            } else {
                                vm.saveOtherBrand(
                                    userId, saleDate, selectedBrand, otherQuantity,
                                    onSuccess = onSaved,
                                    onError = { localError = it }
                                )
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Сохранить")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateToMillis(saleDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        saleDate = millisToDate(it)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun HonorModelRow(
    name: String,
    quantity: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (quantity > 0)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onMinus,
                enabled = enabled && quantity > 0,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Default.Remove, contentDescription = null,
                    modifier = Modifier.size(18.dp))
            }
            Text(
                quantity.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(40.dp)
                    .padding(horizontal = 4.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = onPlus,
                enabled = enabled,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = null,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun parseDateToMillis(date: String): Long {
    val parts = date.split("-")
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun millisToDate(millis: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = millis
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}