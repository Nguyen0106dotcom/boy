package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.PlaceholderChildDetectionRepository
import com.example.domain.model.ActiveAlert
import com.example.domain.model.AlertReason
import com.example.domain.model.DemoTargetProfile
import com.example.domain.model.DetectedSubject
import com.example.domain.model.ScareTheme
import com.example.ui.components.InitialPinSetupDialog
import com.example.ui.components.NumericPinInputDialog
import com.example.ui.components.OverlayPermissionDialog
import com.example.ui.components.ScareAlertOverlay
import com.example.ui.components.UsageAccessPermissionDialog
import com.example.ui.theme.KidGuardTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KidGuard", appName)
    }

    @Test
    fun `threshold triggered bitmap frame produces soft advisory classification`() = runTest {
        val repo = PlaceholderChildDetectionRepository()
        val thresholdBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val thresholdRes = repo.analyzeFrame(
            bitmap = thresholdBitmap,
            demoProfile = DemoTargetProfile.SIMULATE_CHILD,
            assumePresenceWithoutCamera = false,
            isThresholdTriggered = true
        )
        assertEquals(DetectedSubject.CHILD, thresholdRes.subject)
        assertTrue(thresholdRes.summary.contains("vượt ngưỡng"))
        assertTrue(thresholdRes.summary.contains("không lưu ảnh"))
        thresholdBitmap.recycle()
    }

    @Test
    fun `initial pin setup dialog validates matching 4-digit pin and supports default quick option`() {
        var savedPin = ""
        composeTestRule.setContent {
            KidGuardTheme(darkTheme = false) {
                InitialPinSetupDialog(
                    onPinCreated = { pin -> savedPin = pin }
                )
            }
        }

        composeTestRule.onNodeWithTag("initial_pin_setup_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_initial_pin_button").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("initial_pin_field").performTextInput("2580")
        composeTestRule.onNodeWithTag("initial_pin_confirm_field").performTextInput("2581")
        composeTestRule.onNodeWithTag("save_initial_pin_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("save_initial_pin_button").performClick()

        composeTestRule.onNodeWithText("Mã PIN xác nhận chưa khớp.").assertIsDisplayed()
        assertEquals("", savedPin)

        composeTestRule.onNodeWithTag("use_default_pin_button").performClick()
        assertEquals("1234", savedPin)
    }

    @Test
    fun `scare alert overlay renders warm reminder and invokes Da hieu callback`() {
        var understoodClicked = false
        val alert = ActiveAlert(
            reason = AlertReason.TIME_LIMIT_EXCEEDED,
            scareTheme = ScareTheme.GENTLE_EYECARE,
            triggeredAtMillis = System.currentTimeMillis(),
            elapsedMinutes = 45,
            detailText = "Đã dùng 45 / 45 phút — Nhắc nghỉ mắt, nhìn xa 20 giây."
        )

        composeTestRule.setContent {
            KidGuardTheme(darkTheme = true) {
                ScareAlertOverlay(
                    alert = alert,
                    onAcknowledgeUnderstood = { understoodClicked = true },
                    onDismissWithPin = { _, _ -> false }
                )
            }
        }

        composeTestRule.onNodeWithTag("alert_understood_button").performScrollTo()
        composeTestRule.onNodeWithTag("alert_understood_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("alert_understood_button").performClick()
        assertTrue(understoodClicked)
    }

    @Test
    fun `numeric pin dialog enforces 4 digits and rejects wrong pin`() {
        var submittedPin = ""
        var unlocked = false

        composeTestRule.setContent {
            KidGuardTheme {
                NumericPinInputDialog(
                    onDismiss = {},
                    onPinSubmitted = { pin ->
                        submittedPin = pin
                        val valid = pin == "1234"
                        if (valid) unlocked = true
                        valid
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("numeric_pin_input_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("numeric_pin_confirm_button").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("numeric_pin_text_field").performTextInput("12ab")
        composeTestRule.onNodeWithTag("numeric_pin_confirm_button").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("numeric_pin_text_field").performTextInput("99")
        composeTestRule.onNodeWithTag("numeric_pin_confirm_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("numeric_pin_confirm_button").performClick()

        assertEquals("1299", submittedPin)
        assertTrue(!unlocked)
        composeTestRule.onNodeWithText("Mã PIN không đúng. Vui lòng thử lại.").assertIsDisplayed()

        composeTestRule.onNodeWithTag("numeric_pin_text_field").performTextInput("1234")
        composeTestRule.onNodeWithTag("numeric_pin_confirm_button").performClick()

        assertEquals("1234", submittedPin)
        assertTrue(unlocked)
    }

    @Test
    fun `usage access and overlay permission dialogs trigger callbacks`() {
        var openedUsage = false
        var openedOverlay = false

        composeTestRule.setContent {
            KidGuardTheme {
                UsageAccessPermissionDialog(
                    hasPermission = false,
                    onDismiss = {},
                    onOpenUsageAccessSettings = { openedUsage = true }
                )
                OverlayPermissionDialog(
                    hasPermission = false,
                    onDismiss = {},
                    onOpenOverlaySettings = { openedOverlay = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("confirm_overlay_settings_button").performClick()
        assertTrue(openedOverlay)

        composeTestRule.onNodeWithTag("confirm_usage_access_settings_button").performClick()
        assertTrue(openedUsage)
    }
}
