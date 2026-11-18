package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLimitRule
import com.example.ui.KidGuardUiState
import com.example.ui.components.NumericPinInputDialog
import kotlin.math.roundToInt

@Composable
fun RulesScreen(
    uiState: KidGuardUiState,
    onUnlockLimitsWithPin: (String) -> Boolean,
    onLockLimitsEdit: () -> Unit,
    onMaxMinutesChanged: (Int) -> Unit,
    onWeekendMaxMinutesChanged: (Int) -> Unit,
    onUpdateAppLimit: (String, Boolean, Int) -> Unit,
    onAddAppRule: (String, List<String>, Int) -> Unit,
    onUpdateAppRuleFull: (String, String, List<String>, Boolean, Int) -> Unit,
    onDeleteAppRule: (String) -> Unit,
    onWarningThresholdsChanged: (Boolean, Boolean, Boolean) -> Unit,
    onBedtimeEnabledChanged: (Boolean) -> Unit,
    onBedtimeStartChanged: (Int, Int) -> Unit,
    onBedtimeEndChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val isLimitsUnlocked = uiState.isLimitsEditUnlocked
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showLimitsPinDialog by remember { mutableStateOf(false) }
    var pendingLimitAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    var showAddRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<AppLimitRule?>(null) }
    var deletingRule by remember { mutableStateOf<AppLimitRule?>(null) }

    fun requireLimitsPin(action: () -> Unit) {
        if (isLimitsUnlocked) {
            action()
        } else {
            pendingLimitAction = action
            showLimitsPinDialog = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RulesHeader()
                    LimitsLockCard(
                        isLimitsUnlocked = isLimitsUnlocked,
                        onToggleLock = {
                            if (isLimitsUnlocked) onLockLimitsEdit() else {
                                pendingLimitAction = null
                                showLimitsPinDialog = true
                            }
                        }
                    )
                    DailyScheduleCard(
                        weekdayMinutes = settings.maxMinutes,
                        weekendMinutes = settings.weekendMaxMinutes,
                        onWeekdayChange = { v -> requireLimitsPin { onMaxMinutesChanged(v) } },
                        onWeekendChange = { v -> requireLimitsPin { onWeekendMaxMinutesChanged(v) } }
                    )
                    BedtimeScheduleCard(
                        enabled = settings.bedtimeEnabled,
                        startHour = settings.bedtimeStartHour,
                        startMinute = settings.bedtimeStartMinute,
                        endHour = settings.bedtimeEndHour,
                        endMinute = settings.bedtimeEndMinute,
                        onEnabledChange = { en -> requireLimitsPin { onBedtimeEnabledChanged(en) } },
                        onClickStart = { requireLimitsPin { showStartPicker = true } },
                        onClickEnd = { requireLimitsPin { showEndPicker = true } }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    WarningThresholdsCard(
                        warn10 = settings.warnAt10Min,
                        warn5 = settings.warnAt5Min,
                        warnLimit = settings.warnAtLimit,
                        onChanged = { w10, w5, wl ->
                            requireLimitsPin { onWarningThresholdsChanged(w10, w5, wl) }
                        }
                    )
                    PerAppLimitsCard(
                        rules = settings.appLimits,
                        onAddClick = { requireLimitsPin { showAddRuleDialog = true } },
                        onToggleRule = { id, en, mins ->
                            requireLimitsPin { onUpdateAppLimit(id, en, mins) }
                        },
                        onEditRule = { rule -> requireLimitsPin { editingRule = rule } },
                        onDeleteRule = { rule -> requireLimitsPin { deletingRule = rule } }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 620.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RulesHeader()
                    LimitsLockCard(
                        isLimitsUnlocked = isLimitsUnlocked,
                        onToggleLock = {
                            if (isLimitsUnlocked) onLockLimitsEdit() else {
                                pendingLimitAction = null
                                showLimitsPinDialog = true
                            }
                        }
                    )
                    DailyScheduleCard(
                        weekdayMinutes = settings.maxMinutes,
                        weekendMinutes = settings.weekendMaxMinutes,
                        onWeekdayChange = { v -> requireLimitsPin { onMaxMinutesChanged(v) } },
                        onWeekendChange = { v -> requireLimitsPin { onWeekendMaxMinutesChanged(v) } }
                    )
                    BedtimeScheduleCard(
                        enabled = settings.bedtimeEnabled,
                        startHour = settings.bedtimeStartHour,
                        startMinute = settings.bedtimeStartMinute,
                        endHour = settings.bedtimeEndHour,
                        endMinute = settings.bedtimeEndMinute,
                        onEnabledChange = { en -> requireLimitsPin { onBedtimeEnabledChanged(en) } },
                        onClickStart = { requireLimitsPin { showStartPicker = true } },
                        onClickEnd = { requireLimitsPin { showEndPicker = true } }
                    )
                    WarningThresholdsCard(
                        warn10 = settings.warnAt10Min,
                        warn5 = settings.warnAt5Min,
                        warnLimit = settings.warnAtLimit,
                        onChanged = { w10, w5, wl ->
                            requireLimitsPin { onWarningThresholdsChanged(w10, w5, wl) }
                        }
                    )
                    PerAppLimitsCard(
                        rules = settings.appLimits,
                        onAddClick = { requireLimitsPin { showAddRuleDialog = true } },
                        onToggleRule = { id, en, mins ->
                            requireLimitsPin { onUpdateAppLimit(id, en, mins) }
                        },
                        onEditRule = { rule -> requireLimitsPin { editingRule = rule } },
                        onDeleteRule = { rule -> requireLimitsPin { deletingRule = rule } }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showStartPicker) {
        RuleTimePickerDialog(
            title = "Chọn giờ bắt đầu tối",
            initialHour = settings.bedtimeStartHour,
            initialMinute = settings.bedtimeStartMinute,
            onDismiss = { showStartPicker = false },
            onConfirm = { h, m ->
                onBedtimeStartChanged(h, m)
                showStartPicker = false
            }
        )
    }

    if (showEndPicker) {
        RuleTimePickerDialog(
            title = "Chọn giờ kết thúc sáng",
            initialHour = settings.bedtimeEndHour,
            initialMinute = settings.bedtimeEndMinute,
            onDismiss = { showEndPicker = false },
            onConfirm = { h, m ->
                onBedtimeEndChanged(h, m)
                showEndPicker = false
            }
        )
    }

    if (showLimitsPinDialog) {
        NumericPinInputDialog(
            title = "Xác thực mã PIN Phụ huynh",
            subtitle = "Nhập mã PIN 4 chữ số để mở khóa thay đổi quy tắc",
            onDismiss = {
                showLimitsPinDialog = false
                pendingLimitAction = null
            },
            onPinSubmitted = { pin ->
                val ok = onUnlockLimitsWithPin(pin)
                if (ok) {
                    showLimitsPinDialog = false
                    pendingLimitAction?.invoke()
                    pendingLimitAction = null
                }
                ok
            }
        )
    }

    if (showAddRuleDialog) {
        AddOrEditAppRuleDialog(
            initialRule = null,
            existingRuleNames = settings.appLimits.map { it.appName },
            onDismiss = { showAddRuleDialog = false },
            onSave = { name, pkgs, mins ->
                onAddAppRule(name, pkgs, mins)
                showAddRuleDialog = false
            }
        )
    }

    editingRule?.let { target ->
        AddOrEditAppRuleDialog(
            initialRule = target,
            existingRuleNames = settings.appLimits.map { it.appName },
            onDismiss = { editingRule = null },
            onSave = { name, pkgs, mins ->
                onUpdateAppRuleFull(target.id, name, pkgs, target.enabled, mins)
                editingRule = null
            }
        )
    }

    deletingRule?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingRule = null },
            title = { Text("Xóa quy tắc ${target.appName}?") },
            text = { Text("Quy tắc giới hạn cho ứng dụng này sẽ bị xóa khỏi danh sách theo dõi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAppRule(target.id)
                        deletingRule = null
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRule = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun RulesHeader() {
    Column {
        Text(
            text = "Quy tắc thời gian & Ứng dụng",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Thiết lập ngày học, cuối tuần, khung giờ ngủ, ngưỡng cảnh báo và giới hạn từng ứng dụng",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LimitsLockCard(
    isLimitsUnlocked: Boolean,
    onToggleLock: () -> Unit
) {
    Surface(
        modifier = Modifier.testTag("limits_lock_status_card"),
        shape = MaterialTheme.shapes.large,
        color = if (isLimitsUnlocked) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        border = BorderStroke(
            1.dp,
            if (isLimitsUnlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isLimitsUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (isLimitsUnlocked) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isLimitsUnlocked) {
                            "Đã mở khóa chỉnh sửa quy tắc"
                        } else {
                            "Quy tắc đang khóa PIN"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isLimitsUnlocked) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                    Text(
                        text = if (isLimitsUnlocked) {
                            "Bấm Khóa lại sau khi hoàn tất."
                        } else {
                            "Cần nhập PIN để sửa giới hạn giờ xem hoặc thêm/xóa app."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isLimitsUnlocked) {
                            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.88f)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onToggleLock,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("toggle_limits_lock_button")
            ) {
                Text(if (isLimitsUnlocked) "Khóa lại" else "Mở khóa")
            }
        }
    }
}

@Composable
private fun DailyScheduleCard(
    weekdayMinutes: Int,
    weekendMinutes: Int,
    onWeekdayChange: (Int) -> Unit,
    onWeekendChange: (Int) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ngày học (Thứ 2 – Thứ 6): $weekdayMinutes phút",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(30, 45, 60, 90).forEach { preset ->
                    FilterChip(
                        selected = weekdayMinutes == preset,
                        onClick = { onWeekdayChange(preset) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("preset_${preset}m"),
                        label = { Text("${preset}p") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Slider(
                value = weekdayMinutes.toFloat(),
                onValueChange = { onWeekdayChange(it.roundToInt()) },
                valueRange = 10f..180f,
                steps = 33
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Weekend,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cuối tuần (T7 – CN): $weekendMinutes phút",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(45, 60, 75, 120).forEach { preset ->
                    FilterChip(
                        selected = weekendMinutes == preset,
                        onClick = { onWeekendChange(preset) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("weekend_preset_${preset}m"),
                        label = { Text("${preset}p") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                        )
                    )
                }
            }

            Slider(
                value = weekendMinutes.toFloat(),
                onValueChange = { onWeekendChange(it.roundToInt()) },
                valueRange = 15f..240f,
                steps = 44,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.tertiary,
                    activeTrackColor = MaterialTheme.colorScheme.tertiary
                )
            )
        }
    }
}

@Composable
private fun BedtimeScheduleCard(
    enabled: Boolean,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    onEnabledChange: (Boolean) -> Unit,
    onClickStart: () -> Unit,
    onClickEnd: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Khung giờ đi ngủ",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Nhắc bé nghỉ ngơi khi mở máy ban đêm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.testTag("bedtime_switch")
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RuleTimeTile(
                        label = "Bắt đầu tối",
                        timeText = "%02d:%02d".format(startHour, startMinute),
                        onClick = onClickStart,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bedtime_start_button")
                    )
                    RuleTimeTile(
                        label = "Kết thúc sáng",
                        timeText = "%02d:%02d".format(endHour, endMinute),
                        onClick = onClickEnd,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bedtime_end_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningThresholdsCard(
    warn10: Boolean,
    warn5: Boolean,
    warnLimit: Boolean,
    onChanged: (Boolean, Boolean, Boolean) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ngưỡng cảnh báo thời gian",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            ThresholdSwitchRow(
                title = "Nhắc trước khi còn 10 phút",
                checked = warn10,
                onCheckedChange = { onChanged(it, warn5, warnLimit) },
                testTag = "warn_10m_switch"
            )
            ThresholdSwitchRow(
                title = "Nhắc trước khi còn 5 phút",
                checked = warn5,
                onCheckedChange = { onChanged(warn10, it, warnLimit) },
                testTag = "warn_5m_switch"
            )
            ThresholdSwitchRow(
                title = "Hiển thị khóa mềm khi hết giờ",
                checked = warnLimit,
                onCheckedChange = { onChanged(warn10, warn5, it) },
                testTag = "warn_limit_switch"
            )
        }
    }
}

@Composable
private fun ThresholdSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PerAppLimitsCard(
    rules: List<AppLimitRule>,
    onAddClick: () -> Unit,
    onToggleRule: (String, Boolean, Int) -> Unit,
    onEditRule: (AppLimitRule) -> Unit,
    onDeleteRule: (AppLimitRule) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Giới hạn từng ứng dụng",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedButton(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("add_app_rule_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm app")
                }
            }

            rules.forEach { rule ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rule.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (rule.enabled) {
                                        "Tối đa ${rule.dailyLimitMinutes} phút/ngày"
                                    } else {
                                        "Không giới hạn riêng"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (rule.enabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            IconButton(
                                onClick = { onEditRule(rule) },
                                modifier = Modifier.testTag("edit_rule_${rule.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Sửa ${rule.appName}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { onDeleteRule(rule) },
                                modifier = Modifier.testTag("delete_rule_${rule.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Xóa ${rule.appName}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { enabled ->
                                    onToggleRule(rule.id, enabled, rule.dailyLimitMinutes)
                                },
                                modifier = Modifier.testTag("app_limit_switch_${rule.id}")
                            )
                        }

                        if (rule.enabled) {
                            Slider(
                                value = rule.dailyLimitMinutes.toFloat(),
                                onValueChange = { newVal ->
                                    onToggleRule(rule.id, true, newVal.roundToInt())
                                },
                                valueRange = 10f..120f,
                                steps = 21,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.tertiary,
                                    activeTrackColor = MaterialTheme.colorScheme.tertiary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddOrEditAppRuleDialog(
    initialRule: AppLimitRule?,
    existingRuleNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (appName: String, packageNames: List<String>, limitMinutes: Int) -> Unit
) {
    var appName by remember { mutableStateOf(initialRule?.appName ?: "") }
    var packageInput by remember { mutableStateOf(initialRule?.packageNames?.joinToString(", ") ?: "") }
    var limitMinutes by remember { mutableIntStateOf(initialRule?.dailyLimitMinutes ?: 30) }

    val availableCatalog = remember(existingRuleNames) {
        AppLimitRule.sampleCatalog().filter { cat ->
            existingRuleNames.none { it.equals(cat.appName, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialRule == null) "Thêm quy tắc ứng dụng" else "Sửa quy tắc ${initialRule.appName}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (initialRule == null && availableCatalog.isNotEmpty()) {
                    Text(
                        text = "Chọn nhanh từ danh sách mẫu:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableCatalog.forEach { sample ->
                            FilterChip(
                                selected = appName == sample.appName,
                                onClick = {
                                    appName = sample.appName
                                    packageInput = sample.packageNames.joinToString(", ")
                                    limitMinutes = sample.dailyLimitMinutes
                                },
                                label = { Text(sample.appName) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("Tên ứng dụng (ví dụ: YouTube Kids)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_name_input")
                )

                OutlinedTextField(
                    value = packageInput,
                    onValueChange = { packageInput = it },
                    label = { Text("Package Android (tùy chọn)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Giới hạn mỗi ngày: $limitMinutes phút",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = limitMinutes.toFloat(),
                    onValueChange = { limitMinutes = it.roundToInt() },
                    valueRange = 5f..180f,
                    steps = 34
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pkgs = packageInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(appName, pkgs, limitMinutes)
                },
                enabled = appName.isNotBlank(),
                modifier = Modifier.testTag("save_app_rule_button")
            ) {
                Text("Lưu quy tắc")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun RuleTimeTile(
    label: String,
    timeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
