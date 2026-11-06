package by.grinich.hscontrol.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import by.grinich.hscontrol.data.PremiumItem

// ============================================================================
// ДОБАВЛЕНИЕ PREMIUM-МОДЕЛИ
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPremiumItemDialog(
    models: List<String>,
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, rate: Double, fixedBonus: Double) -> Unit
) {
    var selectedModel by remember { mutableStateOf<String?>(null) }
    var customName by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var fixedText by remember { mutableStateOf("0") }
    var useCustom by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dropdownOpen by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Добавить Premium-модель",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                if (!useCustom) {
                    OutlinedButton(
                        onClick = { dropdownOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedModel ?: "-- Выберите модель --")
                    }
                    DropdownMenu(
                        expanded = dropdownOpen,
                        onDismissRequest = { dropdownOpen = false }
                    ) {
                        models.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    selectedModel = m
                                    dropdownOpen = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("✏️ Ввести вручную...") },
                            onClick = {
                                useCustom = true
                                dropdownOpen = false
                            }
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Название модели") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate (коэффициент)") },
                    placeholder = { Text("Например: 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = fixedText,
                    onValueChange = { fixedText = it },
                    label = { Text("Фикс. выплата за шт. (BYN)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            val name = if (useCustom) customName.trim() else (selectedModel ?: "")
                            if (name.isEmpty()) { error = "Выберите модель"; return@Button }
                            if (name.lowercase() in existingNames) {
                                error = "Эта модель уже добавлена"
                                return@Button
                            }
                            val rate = rateText.replace(",", ".").toDoubleOrNull()
                            if (rate == null || rate < 0) {
                                error = "Введите Rate (число ≥ 0)"
                                return@Button
                            }
                            val fixed = fixedText.replace(",", ".").toDoubleOrNull() ?: 0.0
                            onSave(name, rate, fixed)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Добавить")
                    }
                }
            }
        }
    }
}

// ============================================================================
// РЕДАКТИРОВАНИЕ PREMIUM-МОДЕЛИ (Rate + Fixed)
// ============================================================================
@Composable
fun EditPremiumDialog(
    item: PremiumItem,
    onDismiss: () -> Unit,
    onSave: (newRate: Double, newFixed: Double) -> Unit
) {
    var rateText by remember { mutableStateOf(item.rate.toString()) }
    var fixedText by remember { mutableStateOf(item.fixedBonus.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Изменить Premium-модель", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(item.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = fixedText,
                    onValueChange = { fixedText = it },
                    label = { Text("Фикс. выплата (BYN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            val r = rateText.replace(",", ".").toDoubleOrNull()
                            val f = fixedText.replace(",", ".").toDoubleOrNull()
                            if (r != null && r >= 0 && f != null && f >= 0) {
                                onSave(r, f)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

// ============================================================================
// НАСТРОЙКИ PREMIUM (Tar/Bonus)
// ============================================================================
@Composable
fun PremiumSettingsDialog(
    initialBonus1: Double,
    initialBonus2: Double,
    initialTarget1: Double,
    initialTarget2: Double,
    initialExtra: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double, Double, Double) -> Unit
) {
    var b1 by remember { mutableStateOf(initialBonus1.toString()) }
    var b2 by remember { mutableStateOf(initialBonus2.toString()) }
    var t1 by remember { mutableStateOf(initialTarget1.toString()) }
    var t2 by remember { mutableStateOf(initialTarget2.toString()) }
    var ex by remember { mutableStateOf(initialExtra.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text("Настройки Premium выплат",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Для текущего месяца",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = b1, onValueChange = { b1 = it },
                        label = { Text("PREM BONUS 1, BYN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = b2, onValueChange = { b2 = it },
                        label = { Text("PREM BONUS 2, BYN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = t1, onValueChange = { t1 = it },
                        label = { Text("Tar 1, %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = t2, onValueChange = { t2 = it },
                        label = { Text("Tar 2, %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ex, onValueChange = { ex = it },
                        label = { Text("Бонус за 1 шт. сверх Tar, BYN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            val vb1 = b1.replace(",", ".").toDoubleOrNull()
                            val vb2 = b2.replace(",", ".").toDoubleOrNull()
                            val vt1 = t1.replace(",", ".").toDoubleOrNull()
                            val vt2 = t2.replace(",", ".").toDoubleOrNull()
                            val vex = ex.replace(",", ".").toDoubleOrNull()
                            if (vb1 == null || vb2 == null || vt1 == null || vt2 == null || vex == null) {
                                error = "Проверьте введённые значения"
                                return@Button
                            }
                            if (vt1 >= vt2) {
                                error = "Tar 1 должен быть меньше Tar 2"
                                return@Button
                            }
                            onSave(vb1, vb2, vt1, vt2, vex)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

// ============================================================================
// ДОБАВЛЕНИЕ ФОКУСНОЙ ПОЗИЦИИ (v1/v2)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFocusItemDialog(
    version: Int,
    onDismiss: () -> Unit,
    onSave: (category: String, name: String, bonus: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bonusText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Смартфоны") }
    var error by remember { mutableStateOf<String?>(null) }
    var dropdownOpen by remember { mutableStateOf(false) }

    val categoriesV1 = listOf(
        "Смартфоны", "Наушники", "Планшеты", "Часы",
        "Ноутбуки", "Колонки", "Аксессуары", "Другое"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (version == 2) "Добавить фокусный смартфон"
                    else "Добавить фокусную позицию",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                if (version == 1) {
                    OutlinedButton(
                        onClick = { dropdownOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Категория: $category")
                    }
                    DropdownMenu(
                        expanded = dropdownOpen,
                        onDismissRequest = { dropdownOpen = false }
                    ) {
                        categoriesV1.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = { category = c; dropdownOpen = false }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название модели") },
                    placeholder = { Text("Например: Honor Magic 7 Pro") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = bonusText,
                    onValueChange = { bonusText = it },
                    label = { Text("Бонус за штуку, BYN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) { error = "Введите название"; return@Button }
                            val bonus = bonusText.replace(",", ".").toDoubleOrNull()
                            if (bonus == null || bonus < 0) {
                                error = "Введите бонус (число ≥ 0)"; return@Button
                            }
                            onSave(category, name.trim(), bonus)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Добавить")
                    }
                }
            }
        }
    }
}

// ============================================================================
// РЕДАКТИРОВАНИЕ БОНУСА (v1/v2)
// ============================================================================
@Composable
fun EditBonusDialog(
    currentBonus: Double,
    itemName: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var bonusText by remember { mutableStateOf(currentBonus.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Изменить бонус", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(itemName, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = bonusText,
                    onValueChange = { bonusText = it },
                    label = { Text("Бонус, BYN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            val v = bonusText.replace(",", ".").toDoubleOrNull()
                            if (v != null && v >= 0) onSave(v)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}