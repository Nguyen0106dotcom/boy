package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardElevated
import com.example.ui.theme.MidnightOutline
import com.example.ui.theme.MidnightVoid

@Composable
fun NumericPinInputDialog(
    title: String = "Xác thực mã PIN Phụ huynh",
    subtitle: String = "Nhập mã PIN 4 chữ số để thay đổi giới hạn thời gian sử dụng",
    confirmLabel: String = "Xác nhận",
    dismissLabel: String = "Hủy",
    pinLength: Int = 4,
    showDefaultPinHint: Boolean = true,
    errorMessage: String = stringResource(R.string.pin_error_wrong),
    onDismiss: () -> Unit,
    onPinSubmitted: (String) -> Boolean
) {
    var pinInput by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    fun submitPin() {
        if (pinInput.length < pinLength) return
        val accepted = onPinSubmitted(pinInput)
        if (!accepted) {
            showError = true
            pinInput = ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .testTag("numeric_pin_input_dialog"),
            shape = MaterialTheme.shapes.large,
            color = MidnightCard,
            border = BorderStroke(1.5.dp, MidnightOutline),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AmberGlow.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = title,
                        tint = AmberGlow,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4-dot visual PIN indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pinLength) { index ->
                        val filled = index < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        showError -> CrimsonAlert
                                        filled -> AmberGlow
                                        else -> MidnightOutline
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { raw ->
                        val digitsOnly = raw.filter { it.isDigit() }.take(pinLength)
                        pinInput = digitsOnly
                        if (showError) showError = false
                    },
                    singleLine = true,
                    isError = showError,
                    label = { Text("Mã PIN ($pinLength chữ số)") },
                    placeholder = { Text("••••") },
                    visualTransformation = if (isPinVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitPin() }
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { isPinVisible = !isPinVisible },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("numeric_pin_visibility_toggle")
                        ) {
                            Icon(
                                imageVector = if (isPinVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (isPinVisible) "Ẩn mã PIN" else "Hiện mã PIN",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberGlow,
                        unfocusedBorderColor = MidnightOutline,
                        errorBorderColor = CrimsonAlert,
                        focusedLabelColor = AmberGlow,
                        cursorColor = AmberGlow,
                        focusedContainerColor = MidnightCardElevated.copy(alpha = 0.45f),
                        unfocusedContainerColor = MidnightCardElevated.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("numeric_pin_text_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(visible = showError) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = CrimsonAlert,
                        textAlign = TextAlign.Center
                    )
                }

                if (!showError && showDefaultPinHint) {
                    Text(
                        text = stringResource(R.string.pin_default_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
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
                            .testTag("numeric_pin_cancel_button"),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MidnightOutline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = dismissLabel,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = ::submitPin,
                        enabled = pinInput.length == pinLength,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("numeric_pin_confirm_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGlow,
                            contentColor = MidnightVoid,
                            disabledContainerColor = MidnightCardElevated,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = confirmLabel,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
