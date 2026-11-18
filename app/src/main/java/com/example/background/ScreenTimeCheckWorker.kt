package com.example.background

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.preferences.GuardSettingsRepository
import com.example.domain.model.AlertLogEntry
import com.example.domain.model.AlertReason
import com.example.domain.model.DemoTargetProfile
import com.example.domain.model.ScreenTimeLimitEvaluator
import com.example.usage.UsageStatsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ScreenTimeCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = GuardSettingsRepository(applicationContext)
        val now = System.currentTimeMillis()
        repository.resetDailyCountersIfNewDay(now)

        val settings = repository.getSettingsSnapshot()
        val usage = repository.getUsageSnapshot()

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val keyguardManager = applicationContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isScreenActive = (powerManager?.isInteractive ?: true) &&
            !(keyguardManager?.isKeyguardLocked ?: false)

        val usageSnapshot = UsageStatsHelper.queryTodayUsageSnapshot(
            context = applicationContext,
            rules = settings.appLimits
        )

        val totalSeconds = if (usageSnapshot.hasPermission) {
            val systemSeconds = usageSnapshot.totalForegroundSeconds
            val merged = maxOf(usage.accumulatedUsageSeconds, systemSeconds)
            repository.setAccumulatedUsageSeconds(merged, now)
            merged
        } else if (
            !usage.isBackgroundServiceRunning &&
            settings.isGuardArmed &&
            isScreenActive &&
            settings.demoTargetProfile == DemoTargetProfile.SIMULATE_CHILD &&
            usage.lastScreenCheckMillis > 0L &&
            usage.pendingAlertReason == null
        ) {
            val rawDeltaSec = ((now - usage.lastScreenCheckMillis) / 1000L).coerceIn(0L, 15 * 60L)
            repository.incrementUsageSeconds(rawDeltaSec, now)
        } else {
            usage.accumulatedUsageSeconds
        }

        val evaluation = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = totalSeconds,
            settings = settings,
            nowMillis = now,
            curfewSnoozedUntilMillis = usage.curfewSnoozedUntilMillis,
            focusLockMode = usage.focusLockMode,
            focusLockUntilMillis = usage.focusLockUntilMillis,
            extraMinutesGrantedToday = usage.extraMinutesGrantedToday,
            pauseMonitorUntilMillis = usage.pauseMonitorUntilMillis,
            softLockActive = usage.softLockActive
        )

        val exceededApp = if (settings.isGuardArmed) usageSnapshot.exceededRule else null
        val triggered = evaluation.triggeredReason ?: if (exceededApp != null) {
            AlertReason.APP_LIMIT_EXCEEDED
        } else {
            null
        }

        val effectiveLimitMins = (evaluation.limitSeconds / 60L).toInt()
        val elapsedMins = (totalSeconds / 60L).toInt()
        val remainingMins = (effectiveLimitMins - elapsedMins).coerceAtLeast(0)
        val timeLabel = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now))
        val sourceLabel = if (usageSnapshot.hasPermission) "UsageStats + WorkManager" else "WorkManager"

        val summary = if (triggered != null) {
            "$sourceLabel ($timeLabel) • Vượt ngưỡng ($elapsedMins/$effectiveLimitMins phút)"
        } else {
            "$sourceLabel ($timeLabel) • $elapsedMins/$effectiveLimitMins phút (Còn $remainingMins phút)"
        }
        repository.updateBackgroundCheckSummary(summary, now)

        if (triggered != null && usage.pendingAlertReason == null) {
            repository.setPendingAlertReason(triggered)
            val noteText = when (triggered) {
                AlertReason.TIME_LIMIT_EXCEEDED ->
                    "$sourceLabel phát hiện đã dùng $elapsedMins/$effectiveLimitMins phút"
                AlertReason.APP_LIMIT_EXCEEDED ->
                    "Ứng dụng ${exceededApp?.appName.orEmpty()} vượt quá ${exceededApp?.dailyLimitMinutes ?: 30} phút"
                AlertReason.BEDTIME_CURFEW ->
                    "WorkManager phát hiện dùng trong khung giờ ngủ (${settings.formattedBedtimeWindow()})"
                AlertReason.FOCUS_MODE_LOCK ->
                    "Đang khóa theo chế độ ${usage.focusLockMode.label}"
                AlertReason.QUICK_LOCK ->
                    "Đang khóa màn hình theo yêu cầu phụ huynh"
                AlertReason.CHILD_DETECTED ->
                    "Phát hiện trẻ em xem quá lâu"
                AlertReason.MANUAL_TEST -> "Kiểm tra thủ công"
            }
            repository.appendAlertLog(
                AlertLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = now,
                    reason = triggered,
                    scareTheme = settings.scareTheme,
                    elapsedMinutes = elapsedMins,
                    note = noteText
                )
            )
            GuardianNotifications.showLimitExceededNotification(
                context = applicationContext,
                reason = triggered,
                scareTheme = settings.scareTheme,
                elapsedMinutes = elapsedMins,
                maxMinutes = effectiveLimitMins,
                detailOverride = noteText
            )
        }

        return Result.success(
            workDataOf(
                KEY_ELAPSED_SECONDS to totalSeconds,
                KEY_LIMIT_SECONDS to evaluation.limitSeconds,
                KEY_LIMIT_EXCEEDED to (triggered != null)
            )
        )
    }

    companion object {
        const val KEY_ELAPSED_SECONDS = "elapsed_seconds"
        const val KEY_LIMIT_SECONDS = "limit_seconds"
        const val KEY_LIMIT_EXCEEDED = "limit_exceeded"
    }
}
