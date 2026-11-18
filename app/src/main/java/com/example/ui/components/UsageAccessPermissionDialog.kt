package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightOutline
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.NeonMint
import com.example.ui.theme.NeonMintContainer
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OnNeonMintContainer

@Composable
fun UsageAccessPermissionDialog(
    hasPermission: Boolean,
    onDismiss: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .testTag("usage_access_permission_dialog"),
            shape = MaterialTheme.shapes.large,
            color = MidnightCard,
            border = BorderStroke(1.5.dp, if (hasPermission) NeonMint else AmberGlow),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasPermission) NeonMintContainer else AmberGlow.copy(alpha = 0.16f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Filled.CheckCircle else Icons.Filled.QueryStats,
                        contentDescription = stringResource(R.string.usage_access_disclosure_title),
                        tint = if (hasPermission) NeonMint else AmberGlow,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (hasPermission) NeonMintContainer else AmberContainer
                ) {
                    Text(
                        text = if (hasPermission) "ĐÃ CẤP QUYỀN" else "CẦN QUYỀN HỆ THỐNG",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasPermission) OnNeonMintContainer else OnAmberContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.usage_access_disclosure_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.usage_access_disclosure_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MidnightVoid.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MidnightOutline)
                ) {
                    Text(
                        text = "Hướng dẫn: Trong màn hình Cài đặt vừa mở, hãy tìm KidGuard và bật công tắc 'Cho phép truy cập dữ liệu sử dụng'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dismiss_usage_access_dialog_button"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MidnightOutline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(if (hasPermission) "Đóng" else "Để sau")
                    }

                    Button(
                        onClick = onOpenUsageAccessSettings,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("confirm_usage_access_settings_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGlow,
                            contentColor = MidnightVoid
                        )
                    ) {
                        Text(
                            text = if (hasPermission) "Mở lại Cài đặt" else "Cấp quyền ngay",
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
