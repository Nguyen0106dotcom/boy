package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.domain.model.ActiveAlert
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.NeonMint
import com.example.ui.theme.NeonMintContainer
import com.example.ui.theme.OnNeonMintContainer
import kotlin.math.roundToInt

@Composable
fun ScareAlertOverlay(
    alert: ActiveAlert,
    capturedThumbnail: Bitmap? = null,
    onAcknowledgeUnderstood: () -> Unit = {},
    onRequestExtraTime: () -> Unit = {},
    onDismissWithPin: (pin: String, addExtraMinutes: Int?) -> Boolean
) {
    // Prevent bypassing the break/limit overlay with system back button
    BackHandler(enabled = true) { }

    var showPinPrompt by remember { mutableStateOf(false) }
    var extraMinutesOnUnlock by remember { mutableStateOf<Int?>(null) }
    var requestedExtraSent by remember { mutableStateOf(false) }

    val isGentle = alert.scareTheme.isGentleMode
    val transition = rememberInfiniteTransition(label = "overlay_effects")

    val pulseAlpha by transition.animateFloat(
        initialValue = if (isGentle) 0.35f else 0.30f,
        targetValue = if (isGentle) 0.65f else 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isGentle) 1600 else 420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val shakeX by transition.animateFloat(
        initialValue = if (isGentle) 0f else -6f,
        targetValue = if (isGentle) 0f else 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake_x"
    )
    val tiltDeg by transition.animateFloat(
        initialValue = if (isGentle) 0f else -1.4f,
        targetValue = if (isGentle) 0f else 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt_deg"
    )

    val accentColor = if (isGentle) NeonMint else CrimsonAlert
    val backdropGradient = if (isGentle) {
        listOf(
            NeonMintContainer.copy(alpha = pulseAlpha),
            Color(0xFF0E1D2F),
            MidnightVoid
        )
    } else {
        listOf(
            CrimsonAlert.copy(alpha = pulseAlpha * 0.45f),
            Color(0xFF1A040A),
            MidnightVoid
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = backdropGradient))
            .systemBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isGentle) NeonMintContainer else CrimsonAlert.copy(alpha = 0.92f),
                border = if (isGentle) BorderStroke(1.dp, NeonMint) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isGentle) Icons.Filled.RemoveRedEye else Icons.Filled.Warning,
                        contentDescription = alert.reason.label,
                        tint = if (isGentle) OnNeonMintContainer else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.reason.label.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isGentle) OnNeonMintContainer else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .offset { IntOffset(shakeX.roundToInt(), 0) }
                    .rotate(tiltDeg),
                shape = MaterialTheme.shapes.extraLarge,
                color = MidnightCard,
                border = BorderStroke(2.5.dp, accentColor.copy(alpha = pulseAlpha))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = alert.scareTheme.drawableRes),
                        contentDescription = alert.scareTheme.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.extraLarge)
                    )
                    capturedThumbnail?.let { thumb ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MidnightVoid.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, AmberGlow)
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = thumb.asImageBitmap(),
                                    contentDescription = "Ảnh kiểm tra tạm thời (không lưu)",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ước lượng\nkhông lưu ảnh",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberGlow
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = alert.scareTheme.scareHeadline,
                style = MaterialTheme.typography.displaySmall,
                color = if (isGentle) NeonMint else CrimsonAlert,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = alert.scareTheme.scareSubtitle,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            if (alert.detailText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alert.detailText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AmberGlow,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Warm child acknowledgment button ("Đã hiểu" — rests eyes 20s / silences chime)
            Button(
                onClick = onAcknowledgeUnderstood,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("alert_understood_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonMint,
                    contentColor = MidnightVoid
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đã hiểu",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Child option: request 15 extra minutes
            OutlinedButton(
                onClick = {
                    requestedExtraSent = true
                    onRequestExtraTime()
                },
                enabled = !requestedExtraSent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("overlay_request_extra_time_button"),
                border = BorderStroke(1.dp, NeonMint.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonMint),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreTime,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (requestedExtraSent) {
                        "Đã gửi lời xin thêm 15 phút tới ba mẹ"
                    } else {
                        "Xin thêm 15 phút"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    extraMinutesOnUnlock = null
                    showPinPrompt = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("dismiss_alert_pin_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGlow,
                    contentColor = MidnightVoid
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Filled.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ba mẹ nhập PIN để mở khóa",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    extraMinutesOnUnlock = 15
                    showPinPrompt = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("extend_15m_pin_button"),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Duyệt thêm +15 phút ngay (Cần PIN)",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (showPinPrompt) {
        PinDialog(
            title = "Xác nhận Phụ huynh",
            subtitle = if (extraMinutesOnUnlock != null) {
                "Nhập PIN để mở khóa và cộng thêm $extraMinutesOnUnlock phút cho bé"
            } else {
                "Nhập mã PIN 4 số để tắt màn hình nhắc nhở"
            },
            onDismiss = { showPinPrompt = false },
            onPinVerified = { pin ->
                val ok = onDismissWithPin(pin, extraMinutesOnUnlock)
                if (ok) {
                    showPinPrompt = false
                }
                ok
            }
        )
    }
}
