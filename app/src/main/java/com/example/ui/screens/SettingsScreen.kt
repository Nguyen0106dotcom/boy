package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.domain.model.CameraScanPolicy
import com.example.domain.model.DetectionSensitivity
import com.example.domain.model.ScareTheme
import com.example.ui.KidGuardUiState
import com.example.ui.components.NumericPinInputDialog

@Composable
fun SettingsScreen(
    uiState: KidGuardUiState,
    onUnlockLimitsWithPin: (String) -> Boolean,
    onUpdatePin: (String) -> Unit,
    onCameraScanPolicyChanged: (CameraScanPolicy) -> Unit,
    onDetectionSensitivityChanged: (DetectionSensitivity) -> Unit,
    onAiDetectionEnabledChanged: (Boolean) -> Unit,
    onScareThemeSelected: (ScareTheme) -> Unit,
    onSoundEnabledChanged: (Boolean) -> Unit,
    onVoiceEnabledChanged: (Boolean) -> Unit,
    onVibrationEnabledChanged: (Boolean) -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    var showVerifyOldPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

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
                    SettingsHeader()
                    SecurityAndAppearanceCard(
                        isDarkTheme = settings.isDarkTheme,
                        onDarkThemeChanged = onDarkThemeChanged,
                        onChangePinClick = {
                            if (uiState.isLimitsEditUnlocked) {
                                showChangePinDialog = true
                            } else {
                                showVerifyOldPinDialog = true
                            }
                        }
                    )
                    AiAndCameraSettingsCard(
                        aiEnabled = settings.aiDetectionEnabled,
                        scanPolicy = settings.cameraScanPolicy,
                        sensitivity = settings.detectionSensitivity,
                        onAiEnabledChanged = onAiDetectionEnabledChanged,
                        onScanPolicyChanged = onCameraScanPolicyChanged,
                        onSensitivityChanged = onDetectionSensitivityChanged
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SoundAndReminderStyleCard(
                        selectedTheme = settings.scareTheme,
                        soundEnabled = settings.soundEnabled,
                        voiceEnabled = settings.voiceEnabled,
                        vibrationEnabled = settings.vibrationEnabled,
                        onScareThemeSelected = onScareThemeSelected,
                        onSoundEnabledChanged = onSoundEnabledChanged,
                        onVoiceEnabledChanged = onVoiceEnabledChanged,
                        onVibrationEnabledChanged = onVibrationEnabledChanged
                    )
                    PrivacyPolicyAndDataCard(
                        onClearAllDataClick = { showClearDataDialog = true }
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
                    SettingsHeader()
                    SecurityAndAppearanceCard(
                        isDarkTheme = settings.isDarkTheme,
                        onDarkThemeChanged = onDarkThemeChanged,
                        onChangePinClick = {
                            if (uiState.isLimitsEditUnlocked) {
                                showChangePinDialog = true
                            } else {
                                showVerifyOldPinDialog = true
                            }
                        }
                    )
                    AiAndCameraSettingsCard(
                        aiEnabled = settings.aiDetectionEnabled,
                        scanPolicy = settings.cameraScanPolicy,
                        sensitivity = settings.detectionSensitivity,
                        onAiEnabledChanged = onAiDetectionEnabledChanged,
                        onScanPolicyChanged = onCameraScanPolicyChanged,
                        onSensitivityChanged = onDetectionSensitivityChanged
                    )
                    SoundAndReminderStyleCard(
                        selectedTheme = settings.scareTheme,
                        soundEnabled = settings.soundEnabled,
                        voiceEnabled = settings.voiceEnabled,
                        vibrationEnabled = settings.vibrationEnabled,
                        onScareThemeSelected = onScareThemeSelected,
                        onSoundEnabledChanged = onSoundEnabledChanged,
                        onVoiceEnabledChanged = onVoiceEnabledChanged,
                        onVibrationEnabledChanged = onVibrationEnabledChanged
                    )
                    PrivacyPolicyAndDataCard(
                        onClearAllDataClick = { showClearDataDialog = true }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showVerifyOldPinDialog) {
        NumericPinInputDialog(
            title = "Xác thực PIN hiện tại",
            subtitle = "Nhập mã PIN hiện tại trước khi đặt mã PIN mới",
            onDismiss = { showVerifyOldPinDialog = false },
            onPinSubmitted = { pin ->
                val ok = onUnlockLimitsWithPin(pin)
                if (ok) {
                    showVerifyOldPinDialog = false
                    showChangePinDialog = true
                }
                ok
            }
        )
    }

    if (showChangePinDialog) {
        NumericPinInputDialog(
            title = "Đổi mã PIN Phụ huynh",
            subtitle = "Nhập mã PIN 4 chữ số mới để bảo vệ ứng dụng",
            confirmLabel = "Cập nhật",
            showDefaultPinHint = false,
            onDismiss = { showChangePinDialog = false },
            onPinSubmitted = { newPin ->
                onUpdatePin(newPin)
                showChangePinDialog = false
                true
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Xóa toàn bộ dữ liệu?") },
            text = {
                Text(
                    "Thao tác này sẽ đặt lại bộ đếm thời gian hôm nay, xóa toàn bộ nhật ký sự kiện và khôi phục danh sách quy tắc ứng dụng mặc định. Mã PIN của bạn vẫn được giữ nguyên."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Xóa dữ liệu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun SettingsHeader() {
    Column {
        Text(
            text = "Cài đặt hệ thống & Riêng tư",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Quản lý mã PIN, giao diện Tối/Sáng, chu kỳ AI CameraX, âm báo và quyền riêng tư",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SecurityAndAppearanceCard(
    isDarkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onChangePinClick: () -> Unit
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
                        imageVector = Icons.Filled.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isDarkTheme) "Giao diện Tối (Midnight)" else "Giao diện Sáng (Daylight)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Chuyển đổi màu sắc Tối / Sáng tức thì",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onDarkThemeChanged,
                    modifier = Modifier.testTag("dark_theme_switch")
                )
            }

            OutlinedButton(
                onClick = onChangePinClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("change_pin_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đổi mã PIN Phụ huynh (4 số)")
            }
        }
    }
}

@Composable
private fun AiAndCameraSettingsCard(
    aiEnabled: Boolean,
    scanPolicy: CameraScanPolicy,
    sensitivity: DetectionSensitivity,
    onAiEnabledChanged: (Boolean) -> Unit,
    onScanPolicyChanged: (CameraScanPolicy) -> Unit,
    onSensitivityChanged: (DetectionSensitivity) -> Unit
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
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Nhận diện AI qua Gemini Vision",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bật/tắt phân tích độ tuổi khi kiểm tra camera",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = aiEnabled,
                    onCheckedChange = onAiEnabledChanged,
                    modifier = Modifier.testTag("ai_detection_switch")
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Chu kỳ làm mới camera trước",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CameraScanPolicy.entries.forEach { policy ->
                    val selected = scanPolicy == policy
                    FilterChip(
                        selected = selected,
                        onClick = { onScanPolicyChanged(policy) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("camera_policy_${policy.name.lowercase()}"),
                        label = { Text(policy.shortLabel, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                        )
                    )
                }
            }
            Text(
                text = scanPolicy.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Độ nhạy nhận diện khuôn mặt",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetectionSensitivity.entries.forEach { level ->
                    val selected = sensitivity == level
                    FilterChip(
                        selected = selected,
                        onClick = { onSensitivityChanged(level) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sensitivity_${level.name.lowercase()}"),
                        label = { Text(level.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundAndReminderStyleCard(
    selectedTheme: ScareTheme,
    soundEnabled: Boolean,
    voiceEnabled: Boolean,
    vibrationEnabled: Boolean,
    onScareThemeSelected: (ScareTheme) -> Unit,
    onSoundEnabledChanged: (Boolean) -> Unit,
    onVoiceEnabledChanged: (Boolean) -> Unit,
    onVibrationEnabledChanged: (Boolean) -> Unit
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
                    text = "Âm báo & Phong cách nhắc nhở",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            SettingToggleRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Âm thanh chuông nhắc nhở",
                checked = soundEnabled,
                onCheckedChange = onSoundEnabledChanged,
                testTag = "sound_toggle"
            )
            SettingToggleRow(
                icon = Icons.Filled.RecordVoiceOver,
                title = "Giọng nói nhắc nhở dịu nhẹ (Tiếng Việt)",
                checked = voiceEnabled,
                onCheckedChange = onVoiceEnabledChanged,
                testTag = "voice_toggle"
            )
            SettingToggleRow(
                icon = Icons.Filled.Vibration,
                title = "Rung nhẹ khi chạm ngưỡng",
                checked = vibrationEnabled,
                onCheckedChange = onVibrationEnabledChanged,
                testTag = "vibration_toggle"
            )

            ScareTheme.entries.forEach { theme ->
                val selected = selectedTheme == theme
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onScareThemeSelected(theme) }
                        .testTag("scare_theme_${theme.name.lowercase()}"),
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = theme.drawableRes),
                            contentDescription = theme.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = theme.scareSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Đã chọn",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicyAndDataCard(
    onClearAllDataClick: () -> Unit
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
                    imageVector = Icons.Filled.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quyền riêng tư & An toàn trẻ em",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "• Không lưu trữ hình ảnh: Khung hình camera chỉ phân tích tức thời trong RAM và hủy ngay sau đó.\n" +
                            "• Chụp thưa tiết kiệm pin: Chỉ kiểm tra theo chu kỳ đã chọn (hoặc chỉ 1 khung hình khi vượt ngưỡng).\n" +
                            "• Minh bạch với trẻ: Luôn hiển thị huy hiệu CAMERA BẬT rõ ràng khi camera đang hoạt động.\n" +
                            "• Ước lượng mềm: Nhận diện độ tuổi chỉ hỗ trợ nhắc nhở nhẹ nhàng, không phải kết luận tuyệt đối.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Firebase AI Logic & Đồng bộ cục bộ",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Khi chưa cấu hình Firebase hoặc máy không có camera, KidGuard tự động dùng chế độ mô phỏng an toàn trên thiết bị cùng cơ sở dữ liệu Room & DataStore cục bộ.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onClearAllDataClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("clear_all_data_button"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xóa dữ liệu sử dụng & nhật ký")
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
