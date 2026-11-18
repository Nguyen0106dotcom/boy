package com.example

import com.example.data.ai.PlaceholderChildDetectionRepository
import com.example.domain.model.AlertReason
import com.example.domain.model.CameraScanPolicy
import com.example.domain.model.DemoTargetProfile
import com.example.domain.model.DetectedSubject
import com.example.domain.model.FocusLockMode
import com.example.domain.model.GuardSettings
import com.example.domain.model.LogEventType
import com.example.domain.model.ScareTheme
import com.example.domain.model.ScreenTimeLimitEvaluator
import com.example.usage.ForegroundUsageCalculator
import com.example.usage.UsageEventSample
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GuardLogicTest {

    private fun calendarAt(
        hour: Int,
        minute: Int,
        dayOfWeek: Int = Calendar.WEDNESDAY
    ): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
    }

    @Test
    fun `default theme is gentle eye-care reminder`() {
        val settings = GuardSettings()
        assertEquals(ScareTheme.GENTLE_EYECARE, settings.scareTheme)
        assertTrue(settings.scareTheme.isGentleMode)
    }

    @Test
    fun `overnight bedtime curfew detects late night and early morning`() {
        val settings = GuardSettings(
            bedtimeEnabled = true,
            bedtimeStartHour = 21,
            bedtimeStartMinute = 0,
            bedtimeEndHour = 6,
            bedtimeEndMinute = 0
        )

        assertTrue(settings.isCurrentlyInBedtime(calendarAt(21, 0)))
        assertTrue(settings.isCurrentlyInBedtime(calendarAt(23, 30)))
        assertTrue(settings.isCurrentlyInBedtime(calendarAt(3, 15)))
        assertFalse(settings.isCurrentlyInBedtime(calendarAt(6, 0)))
        assertFalse(settings.isCurrentlyInBedtime(calendarAt(14, 0)))
    }

    @Test
    fun `disabled bedtime curfew always returns false`() {
        val settings = GuardSettings(bedtimeEnabled = false)
        assertFalse(settings.isCurrentlyInBedtime(calendarAt(23, 0)))
    }

    @Test
    fun `screen time evaluator applies weekday vs weekend limits and extra granted minutes`() {
        val settings = GuardSettings(
            maxMinutes = 45,
            weekendMaxMinutes = 75,
            bedtimeEnabled = false,
            isGuardArmed = true
        )
        val weekday = calendarAt(14, 0, Calendar.WEDNESDAY)
        val weekend = calendarAt(14, 0, Calendar.SUNDAY)

        val underWeekday = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 44 * 60L + 59L,
            settings = settings,
            calendar = weekday
        )
        assertFalse(underWeekday.isTimeLimitExceeded)
        assertNull(underWeekday.triggeredReason)

        val atWeekdayLimit = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 45 * 60L,
            settings = settings,
            calendar = weekday
        )
        assertTrue(atWeekdayLimit.isTimeLimitExceeded)
        assertEquals(AlertReason.TIME_LIMIT_EXCEEDED, atWeekdayLimit.triggeredReason)

        val extendedWeekday = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 50 * 60L,
            settings = settings,
            extraMinutesGrantedToday = 15,
            calendar = weekday
        )
        assertFalse(extendedWeekday.isTimeLimitExceeded)
        assertNull(extendedWeekday.triggeredReason)

        val underWeekend = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 60 * 60L,
            settings = settings,
            calendar = weekend
        )
        assertFalse(underWeekend.isTimeLimitExceeded)

        val atWeekendLimit = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 75 * 60L,
            settings = settings,
            calendar = weekend
        )
        assertTrue(atWeekendLimit.isTimeLimitExceeded)
        assertEquals(AlertReason.TIME_LIMIT_EXCEEDED, atWeekendLimit.triggeredReason)

        val disarmed = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 120 * 60L,
            settings = settings.copy(isGuardArmed = false),
            calendar = weekday
        )
        assertFalse(disarmed.isTimeLimitExceeded)
        assertNull(disarmed.triggeredReason)
    }

    @Test
    fun `approaching warning detects 10-minute and 5-minute remaining thresholds`() {
        val settings = GuardSettings(
            maxMinutes = 45,
            bedtimeEnabled = false,
            isGuardArmed = true,
            warnAt10Min = true,
            warnAt5Min = true
        )
        val weekday = calendarAt(14, 0, Calendar.WEDNESDAY)

        // 36 minutes used -> 9 minutes remaining -> 10m warning
        val warn10 = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 36 * 60L,
            settings = settings,
            calendar = weekday
        )
        assertEquals(10, warn10.approachingWarningMinutes)
        assertNull(warn10.triggeredReason)

        // 41 minutes used -> 4 minutes remaining -> 5m warning
        val warn5 = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 41 * 60L,
            settings = settings,
            calendar = weekday
        )
        assertEquals(5, warn5.approachingWarningMinutes)
        assertNull(warn5.triggeredReason)
    }

    @Test
    fun `temporary 15-minute pause suppresses time alerts while quick lock triggers immediately`() {
        val settings = GuardSettings(maxMinutes = 45, bedtimeEnabled = false, isGuardArmed = true)
        val now = 5_000_000L
        val weekday = calendarAt(14, 0, Calendar.WEDNESDAY)

        val paused = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 60 * 60L,
            settings = settings,
            nowMillis = now,
            pauseMonitorUntilMillis = now + 15 * 60_000L,
            softLockActive = false,
            calendar = weekday
        )
        assertTrue(paused.isTemporarilyPaused)
        assertNull(paused.triggeredReason)

        val quickLocked = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 10L,
            settings = settings,
            nowMillis = now,
            pauseMonitorUntilMillis = now + 15 * 60_000L,
            softLockActive = true,
            calendar = weekday
        )
        assertTrue(quickLocked.isQuickLocked)
        assertEquals(AlertReason.QUICK_LOCK, quickLocked.triggeredReason)
    }

    @Test
    fun `focus lock mode triggers lock alert immediately until expiry`() {
        val settings = GuardSettings(bedtimeEnabled = false, isGuardArmed = true)
        val now = 1_000_000L

        val activeStudyLock = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 10L,
            settings = settings,
            nowMillis = now,
            focusLockMode = FocusLockMode.STUDY,
            focusLockUntilMillis = now + 60_000L,
            calendar = calendarAt(14, 0)
        )
        assertTrue(activeStudyLock.isFocusLocked)
        assertEquals(AlertReason.FOCUS_MODE_LOCK, activeStudyLock.triggeredReason)

        val expiredStudyLock = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = 10L,
            settings = settings,
            nowMillis = now + 60_000L,
            focusLockMode = FocusLockMode.STUDY,
            focusLockUntilMillis = now + 60_000L,
            calendar = calendarAt(14, 0)
        )
        assertFalse(expiredStudyLock.isFocusLocked)
        assertNull(expiredStudyLock.triggeredReason)
    }

    @Test
    fun `camera scan policies map to expected intervals and log event types map accurately`() {
        assertEquals(10_000L, CameraScanPolicy.SPARSE_10S.intervalMillis)
        assertEquals(30_000L, CameraScanPolicy.SPARSE_30S.intervalMillis)
        assertEquals(60_000L, CameraScanPolicy.SPARSE_60S.intervalMillis)
        assertNull(CameraScanPolicy.THRESHOLD_ONLY.intervalMillis)

        assertEquals(LogEventType.THRESHOLD_EXCEEDED, AlertReason.TIME_LIMIT_EXCEEDED.toLogEventType())
        assertEquals(LogEventType.CHILD_DETECTED, AlertReason.CHILD_DETECTED.toLogEventType())
        assertEquals(LogEventType.SCREEN_LOCKED, AlertReason.QUICK_LOCK.toLogEventType())
        assertEquals(LogEventType.SCREEN_LOCKED, AlertReason.BEDTIME_CURFEW.toLogEventType())
    }

    @Test
    fun `placeholder AI repository marks soft advisory output and respects profile`() = runTest {
        val repo = PlaceholderChildDetectionRepository()

        val childRes = repo.analyzeFrame(
            bitmap = null,
            demoProfile = DemoTargetProfile.SIMULATE_CHILD,
            assumePresenceWithoutCamera = true
        )
        assertTrue(childRes.isPlaceholderAi)
        assertEquals(DetectedSubject.CHILD, childRes.subject)
        assertTrue(childRes.summary.contains("Ước lượng mềm"))

        val adultRes = repo.analyzeFrame(
            bitmap = null,
            demoProfile = DemoTargetProfile.SIMULATE_ADULT,
            assumePresenceWithoutCamera = true
        )
        assertTrue(adultRes.isPlaceholderAi)
        assertEquals(DetectedSubject.ADULT, adultRes.subject)
        assertTrue(adultRes.summary.contains("Ước lượng mềm"))
    }

    @Test
    fun `foreground usage calculator sums closed intervals and active foreground session`() {
        val windowStart = 1_000_000L
        val now = windowStart + 120_000L

        val events = listOf(
            UsageEventSample("com.google.android.youtube", 1, windowStart + 10_000L),
            UsageEventSample("com.google.android.youtube", 2, windowStart + 50_000L),
            UsageEventSample("com.android.systemui", 1, windowStart + 55_000L),
            UsageEventSample("com.example", 1, windowStart + 60_000L),
            UsageEventSample("com.zhiliaoapp.musically", 1, windowStart + 70_000L)
        )

        val result = ForegroundUsageCalculator.compute(
            events = events,
            windowStartMillis = windowStart,
            nowMillis = now,
            ignoredPackages = setOf("com.example")
        )

        assertEquals(90L, result.totalForegroundSeconds)
        assertEquals(40_000L, result.perPackageMillis["com.google.android.youtube"])
        assertEquals(50_000L, result.perPackageMillis["com.zhiliaoapp.musically"])
        assertNull(result.perPackageMillis["com.android.systemui"])
        assertNull(result.perPackageMillis["com.example"])
        assertEquals("com.zhiliaoapp.musically", result.activePackageName)
    }

    @Test
    fun `foreground usage calculator stops counting when screen turns off or locks`() {
        val windowStart = 2_000_000L
        val now = windowStart + 300_000L

        val events = listOf(
            UsageEventSample("com.roblox.client", 1, windowStart + 10_000L),
            UsageEventSample("android", 16, windowStart + 35_000L)
        )

        val result = ForegroundUsageCalculator.compute(
            events = events,
            windowStartMillis = windowStart,
            nowMillis = now
        )

        assertEquals(25L, result.totalForegroundSeconds)
        assertEquals(25_000L, result.perPackageMillis["com.roblox.client"])
        assertNull(result.activePackageName)
    }
}
