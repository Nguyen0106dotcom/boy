package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.CameraScanPolicy
import com.example.domain.model.ExtraTimeStatus
import com.example.ui.KidGuardUiState
import com.example.ui.components.PinDialog

@Composable
fun KidModeScreen(
    uiState: KidGuardUiState,
    hasCameraPermission: Boolean,
    onRequestExtraTime: () -> Unit,
    onApproveExtraTimeWithPin: (String) -> Boolean,
    onOpenParentMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val remainingMins = (uiState.remainingSeconds / 60L).toInt()
    val remainingSecs = (uiState.remainingSeconds % 60L).toInt()
    val remainingProgress = (1f - uiState.progressFraction).coerceIn(0f, 1f)
    val isWeekend = settings.isWeekend()
    val pendingRequest = uiState.pendingExtraTimeRequest

    var showApproveExtraPinDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Clean Kid Header with PIN-locked Parent button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.tertiary,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "KidGuard",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "KidGuard",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (isWeekend) "Lịch cuối tuần" else "Lịch ngày học",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenParentMode,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("enter_parent_mode_button"),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = "Chế độ Phụ huynh",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Phụ huynh",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Active Focus Lock or Bedtime Banner
                if (uiState.isFocusLockActive) {
                    val lockMinsLeft = ((uiState.focusLockUntilMillis - System.currentTimeMillis()) / 60_000L)
                        .coerceAtLeast(1L)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LockClock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Đang trong ${uiState.focusLockMode.label}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Còn khoảng $lockMinsLeft phút nữa sẽ mở lại",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                } else if (uiState.isInBedtimeWindow && settings.bedtimeEnabled) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Đã vào giờ đi ngủ",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Khung giờ nghỉ: ${settings.formattedBedtimeWindow()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // Giant Kid-Friendly Remaining Time Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (settings.isGuardArmed) {
                                "Thời gian xem còn lại hôm nay"
                            } else {
                                "Chế độ bảo vệ đang tạm nghỉ"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        val ringColor = when {
                            !settings.isGuardArmed -> MaterialTheme.colorScheme.outline
                            remainingProgress <= 0.15f -> MaterialTheme.colorScheme.error
                            remainingProgress <= 0.40f -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                        val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

                        Box(
                            modifier = Modifier.size(224.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokePx = 18.dp.toPx()
                                val diameter = minOf(size.width, size.height) - strokePx
                                val topLeft = Offset(
                                    (size.width - diameter) / 2f,
                                    (size.height - diameter) / 2f
                                )
                                val arcSize = Size(diameter, diameter)

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
                                    sweepAngle = 360f * remainingProgress,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "%02d:%02d".format(remainingMins, remainingSecs),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.remainingSeconds > 0L) "còn lại" else "đã hết giờ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ringColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        val usedMinutes = (uiState.elapsedSeconds / 60L).toInt()
                        Text(
                            text = "Đã dùng $usedMinutes / ${uiState.activeDailyLimitMinutes} phút",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.extraMinutesGrantedToday > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "(Đã cộng +${uiState.extraMinutesGrantedToday} phút được ba mẹ duyệt)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (settings.bedtimeEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Giờ đi ngủ: ${settings.formattedBedtimeWindow()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Kid-friendly eye-care reminder banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Lời nhắc bảo vệ mắt cho bé",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = settings.scareTheme.scareSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }

                // Transparent Privacy & Camera Indicator Pill
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isCameraChecking = hasCameraPermission && uiState.isCameraActive
                        Icon(
                            imageVector = if (isCameraChecking) {
                                Icons.Filled.CameraFront
                            } else {
                                Icons.Filled.VisibilityOff
                            },
                            contentDescription = null,
                            tint = if (isCameraChecking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    !isCameraChecking -> "Chế độ tiết kiệm pin (Camera đang tắt)"
                                    settings.cameraScanPolicy == CameraScanPolicy.THRESHOLD_ONLY ->
                                        "Camera chỉ kiểm tra 1 lần khi hết giờ"
                                    else -> "Camera đang kiểm tra thưa (${settings.cameraScanPolicy.shortLabel}/lần)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Quyền riêng tư: Không lưu trữ hình ảnh của bé.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Child Extra-Time Request Section
                val isPending = pendingRequest?.status == ExtraTimeStatus.PENDING
                Button(
                    onClick = onRequestExtraTime,
                    enabled = !isPending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("request_extra_time_button"),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = if (isPending) Icons.Filled.HourglassTop else Icons.Filled.MoreTime,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPending) "Đang chờ ba mẹ duyệt (+15 phút)..." else "Xin thêm giờ (+15 phút)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (pendingRequest != null) {
                    val (bgColor, textColor, statusIcon, statusMsg) = when (pendingRequest.status) {
                        ExtraTimeStatus.PENDING -> Quadruple(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                            Icons.Filled.HourglassTop,
                            "Yêu cầu xin thêm ${pendingRequest.minutesRequested} phút đã gửi. Ba mẹ có thể duyệt ngay bằng PIN hoặc trong mục Phụ huynh."
                        )
                        ExtraTimeStatus.APPROVED -> Quadruple(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                            Icons.Filled.CheckCircle,
                            "Ba mẹ đã đồng ý cộng thêm +${pendingRequest.minutesRequested} phút cho bé!"
                        )
                        ExtraTimeStatus.DENIED -> Quadruple(
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer,
                            Icons.Filled.LockClock,
                            "Ba mẹ chưa duyệt thêm giờ lần này. Bé hãy nghỉ ngơi cho đỡ mỏi mắt nhé!"
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = bgColor
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = statusMsg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                            if (pendingRequest.status == ExtraTimeStatus.PENDING) {
                                OutlinedButton(
                                    onClick = { showApproveExtraPinDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("kid_approve_extra_time_pin_button"),
                                    border = BorderStroke(1.dp, textColor.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Key,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ba mẹ duyệt nhanh bằng PIN",
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showApproveExtraPinDialog) {
        PinDialog(
            title = "Duyệt xin thêm +15 phút",
            subtitle = "Ba mẹ nhập mã PIN 4 số để đồng ý cộng thêm thời gian cho bé",
            onDismiss = { showApproveExtraPinDialog = false },
            onPinVerified = { pin ->
                val ok = onApproveExtraTimeWithPin(pin)
                if (ok) showApproveExtraPinDialog = false
                ok
            }
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
