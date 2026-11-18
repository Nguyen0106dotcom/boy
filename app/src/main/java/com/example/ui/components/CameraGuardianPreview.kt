package com.example.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.domain.model.ChildDetectionResult
import com.example.domain.model.DetectedSubject
import com.example.domain.model.ThresholdCaptureState
import com.example.domain.model.ThresholdVerificationPhase
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.NeonMint

@Composable
fun CameraGuardianCard(
    detectionResult: ChildDetectionResult,
    thresholdCaptureState: ThresholdCaptureState,
    isGuardArmed: Boolean,
    hasCameraPermission: Boolean,
    cameraBindFailed: Boolean,
    isAnalyzingAi: Boolean = false,
    onRequestCameraPermission: () -> Unit,
    onPreviewViewChanged: (PreviewView?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val borderColor by animateColorAsState(
        targetValue = when {
            !isGuardArmed -> MaterialTheme.colorScheme.outline
            thresholdCaptureState.phase == ThresholdVerificationPhase.CAPTURING_FRAME -> MaterialTheme.colorScheme.primary
            detectionResult.subject == DetectedSubject.CHILD -> MaterialTheme.colorScheme.primary
            detectionResult.subject == DetectedSubject.ADULT -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outline
        },
        label = "camera_border_color"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MidnightCard,
        border = BorderStroke(1.5.dp, borderColor.copy(alpha = 0.85f)),
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
                .clip(MaterialTheme.shapes.large)
        ) {
            if (hasCameraPermission && !cameraBindFailed) {
                val previewView = remember {
                    PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                }
                DisposableEffect(previewView) {
                    onPreviewViewChanged(previewView)
                    onDispose {
                        onPreviewViewChanged(null)
                    }
                }
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CameraFallbackContent(
                    hasPermission = hasCameraPermission,
                    bindFailed = cameraBindFailed,
                    onRequestPermission = onRequestCameraPermission
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MidnightVoid.copy(alpha = 0.72f),
                                Color.Transparent,
                                MidnightVoid.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            if (isGuardArmed && hasCameraPermission && !cameraBindFailed) {
                ScanningReticleOverlay(
                    accentColor = borderColor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Row: Trẻ em / Người lớn badge + MÔ PHỎNG / GEMINI VISION + Active camera indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubjectStatusPill(detectionResult = detectionResult)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (detectionResult.isPlaceholderAi) {
                            AmberGlow.copy(alpha = 0.90f)
                        } else {
                            NeonMint.copy(alpha = 0.90f)
                        }
                    ) {
                        Text(
                            text = detectionResult.sourceModeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MidnightVoid,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MidnightVoid.copy(alpha = 0.84f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (hasCameraPermission && !cameraBindFailed) NeonMint else AmberGlow)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (hasCameraPermission && !cameraBindFailed) "CAMERA BẬT" else "KHÔNG LƯU ẢNH",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (isAnalyzingAi) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(16.dp),
                    color = MidnightVoid.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, NeonMint)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = NeonMint
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Đang phân tích khung hình...",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = detectionResult.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                thresholdCaptureState.capturedThumbnail?.let { thumb ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, AmberGlow),
                        color = MidnightVoid
                    ) {
                        Image(
                            bitmap = thumb.asImageBitmap(),
                            contentDescription = "Khung hình kiểm tra tạm thời (không lưu)",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectStatusPill(detectionResult: ChildDetectionResult) {
    val (bg, contentColor, icon) = when (detectionResult.subject) {
        DetectedSubject.CHILD -> Triple(
            AmberGlow.copy(alpha = 0.94f),
            MidnightVoid,
            Icons.Filled.ChildCare
        )
        DetectedSubject.ADULT -> Triple(
            NeonMint.copy(alpha = 0.94f),
            MidnightVoid,
            Icons.Filled.Person
        )
        DetectedSubject.NONE -> Triple(
            MidnightVoid.copy(alpha = 0.85f),
            AmberGlow,
            Icons.Filled.VisibilityOff
        )
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = detectionResult.subject.label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (detectionResult.faceCount > 0) {
                    "${detectionResult.subject.label} (${detectionResult.faceCount})"
                } else {
                    detectionResult.subject.label
                },
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ScanningReticleOverlay(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "scan_line")
    val progress by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_progress"
    )

    Canvas(modifier = modifier) {
        val y = size.height * progress
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    accentColor.copy(alpha = 0.65f),
                    Color.Transparent
                )
            ),
            start = Offset(24f, y),
            end = Offset(size.width - 24f, y),
            strokeWidth = 3f
        )
    }
}

@Composable
private fun CameraFallbackContent(
    hasPermission: Boolean,
    bindFailed: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF252E56), MidnightVoid)
                )
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CameraFront,
            contentDescription = stringResource(R.string.camera_permission_title),
            tint = AmberGlow,
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (bindFailed) {
                "Không tìm thấy camera trước — Dùng chế độ mô phỏng"
            } else {
                stringResource(R.string.camera_permission_title)
            },
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (bindFailed) {
                "KidGuard tự chuyển sang bộ đếm hệ thống + hồ sơ mẫu để tiết kiệm pin."
            } else {
                stringResource(R.string.camera_permission_desc)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFCDD7F7),
            textAlign = TextAlign.Center
        )
        if (!hasPermission && !bindFailed) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.testTag("grant_camera_permission_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGlow,
                    contentColor = MidnightVoid
                )
            ) {
                Text(
                    text = stringResource(R.string.camera_permission_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
