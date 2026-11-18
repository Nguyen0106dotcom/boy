package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.model.DemoTargetProfile
import com.example.domain.model.ExtraTimeStatus
import com.example.domain.model.FocusLockMode
import com.example.ui.KidGuardUiState
import com.example.ui.components.CameraGuardianCard
import com.example.ui.components.OverlayPermissionDialog
import com.example.ui.components.UsageAccessPermissionDialog
import com.example.usage.UsageStatsHelper

@Composable
fun ParentOverviewScreen(
    uiState: KidGuardUiState,
    hasCameraPermission: Boolean,
    cameraBindFailed: Boolean,
    onRequestCameraPermission: () -> Unit,
    onPreviewViewChanged: (PreviewView?) -> Unit,
    onSelectDemoProfile: (DemoTargetProfile) -> Unit,
    onAnalyzeAiNow: () -> Unit,
    onToggleGuardArmed: (Boolean) -> Unit,
    onResolveExtraTimeRequest: (Boolean) -> Unit,
    onActivateFocusLock: (FocusLockMode, Int) -> Unit,
    onClearFocusLock: () -> Unit,
    onQuickLockNow: () -> Unit,
    onPause15Minutes: () -> Unit,
    onResumeMonitoring: () -> Unit,
    onTogglePause: () -> Unit,
    onResetTimer: () -> Unit,
    onFastForwardNearLimit: () -> Unit,
    onCaptureAndVerifyThresholdNow: () -> Unit,
    onTriggerScareTest: () -> Unit,
    onToggleBackgroundService: () -> Unit,
    onRunWorkManagerNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val context = LocalContext.current

    var showUsageDisclosure by remember { mutableStateOf(false) }
    var showBatteryDisclosure by remember { mutableStateOf(false) }
    var showPinningDisclosure by remember { mutableStateOf(false) }
    var showOverlayDisclosure by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (!uiState.isBackgroundServiceRunning) {
            onToggleBackgroundService()
            if (!uiState.permissionsStatus.hasUsageAccess) {
                showUsageDisclosure = true
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
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
                    MasterGuardHeaderCard(uiState = uiState, onToggleGuardArmed = onToggleGuardArmed)
                    AiDetectionAndCameraSection(
                        uiState = uiState,
                        hasCameraPermission = hasCameraPermission,
                        cameraBindFailed = cameraBindFailed,
                        onRequestCameraPermission = onRequestCameraPermission,
                        onPreviewViewChanged = onPreviewViewChanged,
                        onSelectDemoProfile = onSelectDemoProfile,
                        onAnalyzeAiNow = onAnalyzeAiNow
                    )
                    QuickActionsAndFocusCard(
                        uiState = uiState,
                        onQuickLockNow = onQuickLockNow,
                        onPause15Minutes = onPause15Minutes,
                        onResumeMonitoring = onResumeMonitoring,
                        onActivateFocusLock = onActivateFocusLock,
                        onClearFocusLock = onClearFocusLock
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PendingExtraTimeBanner(
                        uiState = uiState,
                        onResolveExtraTimeRequest = onResolveExtraTimeRequest
                    )
                    TodayUsageCard(
                        uiState = uiState,
                        onTogglePause = onTogglePause,
                        onResetTimer = onResetTimer
                    )
                    ReliabilityAndPermissionsCard(
                        uiState = uiState,
                        hasNotificationPermission = hasNotificationPermission,
                        onOpenUsage = { showUsageDisclosure = true },
                        onOpenBattery = { showBatteryDisclosure = true },
                        onOpenOverlay = { showOverlayDisclosure = true },
                        onOpenPinning = { showPinningDisclosure = true },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onToggleBackgroundService = {
                            val turningOn = !uiState.isBackgroundServiceRunning
                            onToggleBackgroundService()
                            if (turningOn && !uiState.permissionsStatus.hasUsageAccess) {
                                showUsageDisclosure = true
                            }
                        },
                        onRunWorkManagerNow = onRunWorkManagerNow
                    )
                    QuickVerificationCard(
                        isGentleMode = settings.scareTheme.isGentleMode,
                        onFastForwardNearLimit = onFastForwardNearLimit,
                        onCaptureAndVerifyThresholdNow = onCaptureAndVerifyThresholdNow,
                        onTriggerScareTest = onTriggerScareTest
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
                    MasterGuardHeaderCard(uiState = uiState, onToggleGuardArmed = onToggleGuardArmed)

                    if (!uiState.permissionsStatus.hasUsageAccess) {
                        UsageAccessPromptBanner(onClick = { showUsageDisclosure = true })
                    }

                    PendingExtraTimeBanner(
                        uiState = uiState,
                        onResolveExtraTimeRequest = onResolveExtraTimeRequest
                    )

                    AiDetectionAndCameraSection(
                        uiState = uiState,
                        hasCameraPermission = hasCameraPermission,
                        cameraBindFailed = cameraBindFailed,
                        onRequestCameraPermission = onRequestCameraPermission,
                        onPreviewViewChanged = onPreviewViewChanged,
                        onSelectDemoProfile = onSelectDemoProfile,
                        onAnalyzeAiNow = onAnalyzeAiNow
                    )

                    TodayUsageCard(
                        uiState = uiState,
                        onTogglePause = onTogglePause,
                        onResetTimer = onResetTimer
                    )

                    QuickActionsAndFocusCard(
                        uiState = uiState,
                        onQuickLockNow = onQuickLockNow,
                        onPause15Minutes = onPause15Minutes,
                        onResumeMonitoring = onResumeMonitoring,
                        onActivateFocusLock = onActivateFocusLock,
                        onClearFocusLock = onClearFocusLock
                    )

                    ReliabilityAndPermissionsCard(
                        uiState = uiState,
                        hasNotificationPermission = hasNotificationPermission,
                        onOpenUsage = { showUsageDisclosure = true },
                        onOpenBattery = { showBatteryDisclosure = true },
                        onOpenOverlay = { showOverlayDisclosure = true },
                        onOpenPinning = { showPinningDisclosure = true },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onToggleBackgroundService = {
                            val turningOn = !uiState.isBackgroundServiceRunning
                            onToggleBackgroundService()
                            if (turningOn && !uiState.permissionsStatus.hasUsageAccess) {
                                showUsageDisclosure = true
                            }
                        },
                        onRunWorkManagerNow = onRunWorkManagerNow
                    )

                    QuickVerificationCard(
                        isGentleMode = settings.scareTheme.isGentleMode,
                        onFastForwardNearLimit = onFastForwardNearLimit,
                        onCaptureAndVerifyThresholdNow = onCaptureAndVerifyThresholdNow,
                        onTriggerScareTest = onTriggerScareTest
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showUsageDisclosure) {
        UsageAccessPermissionDialog(
            hasPermission = uiState.permissionsStatus.hasUsageAccess,
            onDismiss = { showUsageDisclosure = false },
            onOpenUsageAccessSettings = {
                showUsageDisclosure = false
                UsageStatsHelper.openUsageAccessSettings(context)
            }
        )
    }

    if (showOverlayDisclosure) {
        OverlayPermissionDialog(
            hasPermission = uiState.permissionsStatus.hasOverlayPermission,
            onDismiss = { showOverlayDisclosure = false },
            onOpenOverlaySettings = {
                showOverlayDisclosure = false
                UsageStatsHelper.openOverlayPermissionSettings(context)
            }
        )
    }

    if (showBatteryDisclosure) {
        AlertDialog(
            onDismissRequest = { showBatteryDisclosure = false },
            icon = {
                Icon(
                    Icons.Filled.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.battery_disclosure_title)) },
            text = {
                Text(
                    stringResource(R.string.battery_disclosure_body) +
                        "\n\n• Xiaomi/Redmi: Cài đặt -> Ứng dụng -> KidGuard -> Tiết kiệm pin (Không hạn chế) & Tự khởi chạy.\n" +
                        "• Oppo/Realme: Quản lý pin -> Cho phép hoạt động ngầm.\n" +
                        "• Samsung: Chăm sóc thiết bị -> Pin -> Ứng dụng không bao giờ ngủ."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryDisclosure = false
                        UsageStatsHelper.openBatteryOptimizationSettings(context)
                    }
                ) {
                    Text("Mở Cài đặt Pin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDisclosure = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showPinningDisclosure) {
        AlertDialog(
            onDismissRequest = { showPinningDisclosure = false },
            icon = {
                Icon(
                    Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Khóa màn hình chống thoát KidGuard") },
            text = {
                Text(
                    "Để bé không tự ý thoát ứng dụng hoặc gỡ cài đặt:\n" +
                        "1. Vào Cài đặt Bảo mật -> Bật 'Ghim ứng dụng' (App Pinning) và chọn 'Hỏi mã PIN trước khi bỏ ghim'.\n" +
                        "2. Hoặc sử dụng Google Family Link / Digital Wellbeing trên thiết bị."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPinningDisclosure = false
                        UsageStatsHelper.openSecurityOrPinningSettings(context)
                    }
                ) {
                    Text("Mở Bảo mật")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinningDisclosure = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
private fun MasterGuardHeaderCard(
    uiState: KidGuardUiState,
    onToggleGuardArmed: (Boolean) -> Unit
) {
    val settings = uiState.settings
    val statusText = when {
        !settings.isGuardArmed -> "Đang tạm tắt giám sát"
        uiState.isMonitoringPaused15m -> {
            val m = uiState.pauseRemainingSeconds / 60L
            val s = uiState.pauseRemainingSeconds % 60L
            "Tạm dừng giám sát (%02d:%02d)".format(m, s)
        }
        else -> "Đang canh chừng màn hình • Giới hạn ${uiState.activeDailyLimitMinutes} phút"
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (settings.isGuardArmed) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KidGuard",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (settings.isGuardArmed && !uiState.isMonitoringPaused15m) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            Switch(
                checked = settings.isGuardArmed,
                onCheckedChange = onToggleGuardArmed,
                modifier = Modifier.testTag("guard_armed_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                    checkedTrackColor = MaterialTheme.colorScheme.tertiary
                )
            )
        }
    }
}

@Composable
private fun AiDetectionAndCameraSection(
    uiState: KidGuardUiState,
    hasCameraPermission: Boolean,
    cameraBindFailed: Boolean,
    onRequestCameraPermission: () -> Unit,
    onPreviewViewChanged: (PreviewView?) -> Unit,
    onSelectDemoProfile: (DemoTargetProfile) -> Unit,
    onAnalyzeAiNow: () -> Unit
) {
    val settings = uiState.settings
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
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ai_notice_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = uiState.detectionResult.sourceModeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.ai_notice_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CameraGuardianCard(
                detectionResult = uiState.detectionResult,
                thresholdCaptureState = uiState.thresholdCaptureState,
                isGuardArmed = settings.isGuardArmed,
                hasCameraPermission = hasCameraPermission,
                cameraBindFailed = cameraBindFailed,
                isAnalyzingAi = uiState.isAnalyzingAi,
                onRequestCameraPermission = onRequestCameraPermission,
                onPreviewViewChanged = onPreviewViewChanged
            )

            if (!uiState.aiErrorMessage.isNullOrBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = uiState.aiErrorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Sample AI profile quick-switch buttons ("Mẫu AI: Trẻ em" & "Mẫu AI: Người lớn")
            Text(
                text = "Hồ sơ nhận diện mẫu (khi thử nghiệm hoặc thiếu camera):",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoTargetProfile.entries.forEach { profile ->
                    val selected = settings.demoTargetProfile == profile
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectDemoProfile(profile) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("demo_profile_${profile.name.lowercase()}"),
                        label = { Text(profile.label, maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (profile == DemoTargetProfile.SIMULATE_CHILD) {
                                    Icons.Filled.ChildCare
                                } else {
                                    Icons.Filled.Person
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (profile == DemoTargetProfile.SIMULATE_CHILD) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            },
                            selectedLabelColor = if (profile == DemoTargetProfile.SIMULATE_CHILD) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            }
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = onAnalyzeAiNow,
                enabled = !uiState.isAnalyzingAi,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("analyze_ai_now_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Phân tích khung hình AI ngay (${settings.cameraScanPolicy.shortLabel})")
            }
        }
    }
}

@Composable
private fun TodayUsageCard(
    uiState: KidGuardUiState,
    onTogglePause: () -> Unit,
    onResetTimer: () -> Unit
) {
    val settings = uiState.settings
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thời lượng sử dụng của bé",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                BedtimeStatusBadge(
                    enabled = settings.bedtimeEnabled,
                    isInBedtime = uiState.isInBedtimeWindow,
                    windowText = settings.formattedBedtimeWindow()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TimerProgressRing(
                elapsedSeconds = uiState.elapsedSeconds,
                maxMinutes = uiState.activeDailyLimitMinutes,
                progress = uiState.progressFraction,
                isPaused = uiState.isTimerPaused || uiState.isMonitoringPaused15m,
                isArmed = settings.isGuardArmed
            )

            if (!uiState.currentForegroundAppName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Đang đo nền: ${uiState.currentForegroundAppName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("toggle_pause_button"),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = if (uiState.isTimerPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isTimerPaused) "Tiếp tục" else "Tạm dừng")
                }

                OutlinedButton(
                    onClick = onResetTimer,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("reset_timer_button"),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đặt lại")
                }
            }
        }
    }
}

@Composable
private fun QuickActionsAndFocusCard(
    uiState: KidGuardUiState,
    onQuickLockNow: () -> Unit,
    onPause15Minutes: () -> Unit,
    onResumeMonitoring: () -> Unit,
    onActivateFocusLock: (FocusLockMode, Int) -> Unit,
    onClearFocusLock: () -> Unit
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
            Text(
                text = "Nút thao tác nhanh",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Khóa màn hình ngay lập tức, tạm dừng 15 phút hoặc kích hoạt chế độ giờ học/giờ ăn.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Primary Quick Lock & 15m Pause row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onQuickLockNow,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("quick_lock_now_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Khóa ngay", maxLines = 1)
                }

                if (uiState.isMonitoringPaused15m) {
                    val m = uiState.pauseRemainingSeconds / 60L
                    val s = uiState.pauseRemainingSeconds % 60L
                    Button(
                        onClick = onResumeMonitoring,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("resume_monitoring_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tiếp tục (%02d:%02d)".format(m, s), maxLines = 1)
                    }
                } else {
                    OutlinedButton(
                        onClick = onPause15Minutes,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pause_15m_button"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PauseCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tạm dừng 15p", maxLines = 1)
                    }
                }
            }

            if (uiState.isFocusLockActive) {
                val minsLeft = ((uiState.focusLockUntilMillis - System.currentTimeMillis()) / 60_000L)
                    .coerceAtLeast(1L)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Đang bật: ${uiState.focusLockMode.label}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Còn khoảng $minsLeft phút",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Button(
                            onClick = onClearFocusLock,
                            modifier = Modifier.testTag("clear_focus_lock_button")
                        ) {
                            Text("Tắt khóa")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onActivateFocusLock(FocusLockMode.STUDY, 45) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("focus_study_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Học bài 45p", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onActivateFocusLock(FocusLockMode.MEAL, 30) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("focus_meal_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ăn cơm 30p", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageAccessPromptBanner(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.testTag("usage_access_prompt_banner"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.QueryStats,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cần quyền Usage Access để đếm giờ nền",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Cho phép KidGuard dùng UsageStatsManager đo chính xác thời gian từng ứng dụng ngay cả khi chạy ngầm.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("banner_grant_usage_access_button")
            ) {
                Text("Cấp quyền Usage Access ngay")
            }
        }
    }
}

@Composable
private fun PendingExtraTimeBanner(
    uiState: KidGuardUiState,
    onResolveExtraTimeRequest: (Boolean) -> Unit
) {
    val pendingReq = uiState.pendingExtraTimeRequest
    if (pendingReq == null || pendingReq.status != ExtraTimeStatus.PENDING) return

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.MoreTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Bé đang xin thêm +${pendingReq.minutesRequested} phút",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Bạn có muốn duyệt thêm thời gian sử dụng hôm nay không?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onResolveExtraTimeRequest(false) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("deny_extra_time_button")
                ) {
                    Text("Từ chối")
                }
                Button(
                    onClick = { onResolveExtraTimeRequest(true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("approve_extra_time_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Text("Duyệt +${pendingReq.minutesRequested}p")
                }
            }
        }
    }
}

@Composable
private fun ReliabilityAndPermissionsCard(
    uiState: KidGuardUiState,
    hasNotificationPermission: Boolean,
    onOpenUsage: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenPinning: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onToggleBackgroundService: () -> Unit,
    onRunWorkManagerNow: () -> Unit
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
            Text(
                text = "Độ tin cậy & Chạy nền ổn định",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Cấp quyền hệ thống để đo giờ chính xác qua UsageStatsManager, hiển thị khóa mềm và tự khởi chạy lại sau khi khởi động máy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PermissionActionRow(
                icon = Icons.Filled.QueryStats,
                title = "Quyền đo thời gian (Usage Access)",
                subtitle = if (uiState.permissionsStatus.hasUsageAccess) {
                    "Đã cấp • Đang đo chính xác từng ứng dụng"
                } else {
                    "Chưa cấp • Nên bật để đo không cần camera"
                },
                isGranted = uiState.permissionsStatus.hasUsageAccess,
                actionLabel = if (uiState.permissionsStatus.hasUsageAccess) "Xem lại" else "Cấp quyền",
                onClick = onOpenUsage,
                testTag = "usage_access_action_button"
            )

            PermissionActionRow(
                icon = Icons.Filled.Layers,
                title = "Hiển thị trên ứng dụng khác",
                subtitle = if (uiState.permissionsStatus.hasOverlayPermission) {
                    "Đã bật • Cho phép khóa mềm toàn màn hình"
                } else {
                    "Tùy chọn • Nhắc bé nghỉ mắt ngay cả khi xem YouTube/Game"
                },
                isGranted = uiState.permissionsStatus.hasOverlayPermission,
                actionLabel = if (uiState.permissionsStatus.hasOverlayPermission) "Đã bật" else "Cấp quyền",
                onClick = onOpenOverlay,
                testTag = "overlay_permission_action_button"
            )

            PermissionActionRow(
                icon = Icons.Filled.BatteryChargingFull,
                title = "Tắt tối ưu pin (Xiaomi, Oppo, Samsung)",
                subtitle = if (uiState.permissionsStatus.isIgnoringBatteryOptimizations) {
                    "Đã tắt tối ưu pin cho KidGuard"
                } else {
                    "Nên tắt để dịch vụ nền & tự khởi động chạy bền bỉ"
                },
                isGranted = uiState.permissionsStatus.isIgnoringBatteryOptimizations,
                actionLabel = "Hướng dẫn & Bật",
                onClick = onOpenBattery,
                testTag = "battery_optimization_action_button"
            )

            PermissionActionRow(
                icon = Icons.Filled.Lock,
                title = "Ghim màn hình chống thoát ứng dụng",
                subtitle = "Dùng Screen Pinning hoặc Digital Wellbeing của máy",
                isGranted = false,
                actionLabel = "Thiết lập",
                onClick = onOpenPinning,
                testTag = "screen_pinning_action_button"
            )

            // Foreground Service + WorkManager controls
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (uiState.isBackgroundServiceRunning) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dịch vụ chạy nền liên tục",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (uiState.isBackgroundServiceRunning) "ĐANG CHẠY" else "ĐANG TẮT",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (uiState.isBackgroundServiceRunning) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    Text(
                        text = uiState.lastBackgroundCheckSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val turningOn = !uiState.isBackgroundServiceRunning
                                if (turningOn &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !hasNotificationPermission
                                ) {
                                    onRequestNotificationPermission()
                                } else {
                                    onToggleBackgroundService()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("toggle_background_service_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isBackgroundServiceRunning) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                },
                                contentColor = if (uiState.isBackgroundServiceRunning) {
                                    MaterialTheme.colorScheme.onError
                                } else {
                                    MaterialTheme.colorScheme.onTertiary
                                }
                            )
                        ) {
                            Text(
                                text = if (uiState.isBackgroundServiceRunning) "Dừng Service" else "Bật Service",
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = onRunWorkManagerNow,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("run_workmanager_check_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kiểm tra ngay", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickVerificationCard(
    isGentleMode: Boolean,
    onFastForwardNearLimit: () -> Unit,
    onCaptureAndVerifyThresholdNow: () -> Unit,
    onTriggerScareTest: () -> Unit
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
            Text(
                text = "Thử nghiệm nhắc nhở",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onFastForwardNearLimit,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("fast_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FastForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Còn 5 giây", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onCaptureAndVerifyThresholdNow,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("capture_threshold_check_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chụp kiểm tra", maxLines = 1)
                }
            }

            Button(
                onClick = onTriggerScareTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("test_scare_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isGentleMode) Icons.Filled.NotificationsActive else Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xem thử màn hình nhắc nhở ngay")
            }
        }
    }
}

@Composable
private fun PermissionActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (isGranted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Filled.CheckCircle else icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.testTag(testTag)
            ) {
                Text(actionLabel, maxLines = 1)
            }
        }
    }
}

@Composable
private fun BedtimeStatusBadge(
    enabled: Boolean,
    isInBedtime: Boolean,
    windowText: String
) {
    val bg = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
        isInBedtime -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        isInBedtime -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val label = when {
        !enabled -> "Tắt giờ ngủ"
        isInBedtime -> "$windowText • Đang đêm"
        else -> "$windowText • Ban ngày"
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isInBedtime || !enabled) Icons.Filled.Bedtime else Icons.Filled.WbSunny,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fg
            )
        }
    }
}

@Composable
private fun TimerProgressRing(
    elapsedSeconds: Long,
    maxMinutes: Int,
    progress: Float,
    isPaused: Boolean,
    isArmed: Boolean
) {
    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val formattedElapsed = "%02d:%02d".format(mins, secs)

    val ringColor = when {
        !isArmed || isPaused -> MaterialTheme.colorScheme.outline
        progress >= 0.85f -> MaterialTheme.colorScheme.error
        progress >= 0.55f -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = Modifier.size(172.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 14.dp.toPx()
            val arcDiameter = minOf(size.width, size.height) - strokePx
            val topLeft = Offset(
                (size.width - arcDiameter) / 2f,
                (size.height - arcDiameter) / 2f
            )
            val arcSize = Size(arcDiameter, arcDiameter)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formattedElapsed,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "/ $maxMinutes phút",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    !isArmed -> "Đang tắt"
                    isPaused -> "Đang tạm dừng"
                    else -> "Đang đếm giờ"
                },
                style = MaterialTheme.typography.labelMedium,
                color = ringColor
            )
        }
    }
}
