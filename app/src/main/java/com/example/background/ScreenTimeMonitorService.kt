package com.example.background

import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.example.data.preferences.GuardSettingsRepository
import com.example.domain.model.AlertLogEntry
import com.example.domain.model.AlertReason
import com.example.domain.model.DemoTargetProfile
import com.example.domain.model.ScreenTimeLimitEvaluator
import com.example.usage.UsageStatsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ScreenTimeMonitorService : Service() {

    companion object {
        const val ACTION_START_MONITORING = "com.example.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.action.STOP_MONITORING"

        fun start(context: Context) {
            val intent = Intent(context, ScreenTimeMonitorService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenTimeMonitorService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: GuardSettingsRepository
    private var monitorJob: Job? = null
    private var lastNotifiedAlertAtMillis = 0L

    @Volatile
    private var isScreenInteractiveAndUnlocked = true

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            isScreenInteractiveAndUnlocked = checkScreenInteractiveAndUnlocked()
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = GuardSettingsRepository(applicationContext)
        isScreenInteractiveAndUnlocked = checkScreenInteractiveAndUnlocked()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenStateReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITORING) {
            stopMonitoringAndSelf()
            return START_NOT_STICKY
        }

        val hasAccess = UsageStatsHelper.hasUsageAccessPermission(applicationContext)
        val initialNotification = GuardianNotifications.buildMonitorNotification(
            context = this,
            elapsedSeconds = 0L,
            maxMinutes = 45,
            isScreenActive = isScreenInteractiveAndUnlocked,
            hasUsageAccess = hasAccess
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                GuardianNotifications.MONITOR_NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(GuardianNotifications.MONITOR_NOTIFICATION_ID, initialNotification)
        }

        startTrackingLoop()
        return START_STICKY
    }

    private fun startTrackingLoop() {
        if (monitorJob?.isActive == true) return
        monitorJob = serviceScope.launch {
            repository.setBackgroundServiceRunning(true)
            val tickSeconds = 5L

            while (isActive) {
                pollAndEvaluateOnce(tickSeconds)
                delay(tickSeconds * 1000L)
            }
        }
    }

    private suspend fun pollAndEvaluateOnce(tickSeconds: Long) {
        isScreenInteractiveAndUnlocked = checkScreenInteractiveAndUnlocked()
        val now = System.currentTimeMillis()
        repository.resetDailyCountersIfNewDay(now)

        val settings = repository.getSettingsSnapshot()
        val usage = repository.getUsageSnapshot()

        // Accurate interval-based tracking via UsageStatsManager (UsageEvents)
        val usageSnapshot = UsageStatsHelper.queryTodayUsageSnapshot(
            context = applicationContext,
            rules = settings.appLimits
        )

        val isChildProfile = settings.demoTargetProfile == DemoTargetProfile.SIMULATE_CHILD
        val nextSeconds = if (usageSnapshot.hasPermission) {
            val systemSeconds = usageSnapshot.totalForegroundSeconds
            val merged = maxOf(usage.accumulatedUsageSeconds, systemSeconds)
            repository.setAccumulatedUsageSeconds(merged, now)
            merged
        } else if (
            settings.isGuardArmed &&
            isScreenInteractiveAndUnlocked &&
            isChildProfile &&
            usage.pendingAlertReason == null
        ) {
            repository.incrementUsageSeconds(tickSeconds, now)
        } else {
            usage.accumulatedUsageSeconds
        }

        val evaluation = ScreenTimeLimitEvaluator.evaluate(
            accumulatedSeconds = nextSeconds,
            settings = settings,
            nowMillis = now,
            curfewSnoozedUntilMillis = usage.curfewSnoozedUntilMillis,
            focusLockMode = usage.focusLockMode,
            focusLockUntilMillis = usage.focusLockUntilMillis,
            extraMinutesGrantedToday = usage.extraMinutesGrantedToday,
            pauseMonitorUntilMillis = usage.pauseMonitorUntilMillis,
            softLockActive = usage.softLockActive
        )

        val effectiveLimitMins = (evaluation.limitSeconds / 60L).toInt()
        val exceededAppRule = if (settings.isGuardArmed) usageSnapshot.exceededRule else null
        val finalReason = evaluation.triggeredReason ?: if (exceededAppRule != null) {
            AlertReason.APP_LIMIT_EXCEEDED
        } else {
            null
        }

        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now))
        val elapsedMins = (nextSeconds / 60L).toInt()
        val elapsedRemSecs = (nextSeconds % 60L).toInt()
        val statusText = when {
            !usageSnapshot.hasPermission ->
                "Chưa có quyền Usage Access ($timeStr) • Chạm thông báo để cấp quyền"
            finalReason != null ->
                "UsageStatsManager ($timeStr) • Đạt ngưỡng (${elapsedMins}p ${elapsedRemSecs}s / ${effectiveLimitMins}p)"
            !usageSnapshot.currentForegroundAppName.isNullOrBlank() ->
                "UsageStats ($timeStr) • ${usageSnapshot.currentForegroundAppName}: ${elapsedMins}p ${elapsedRemSecs}s / ${effectiveLimitMins}p"
            else ->
                "UsageStats ($timeStr) • Đã dùng ${elapsedMins}p ${elapsedRemSecs}s / ${effectiveLimitMins}p"
        }
        repository.updateBackgroundCheckSummary(statusText, now)

        if (GuardianNotifications.canPostNotifications(this@ScreenTimeMonitorService)) {
            val updatedNotification = GuardianNotifications.buildMonitorNotification(
                context = this@ScreenTimeMonitorService,
                elapsedSeconds = nextSeconds,
                maxMinutes = effectiveLimitMins,
                isScreenActive = isScreenInteractiveAndUnlocked,
                hasUsageAccess = usageSnapshot.hasPermission,
                currentForegroundApp = usageSnapshot.currentForegroundAppName
            )
            try {
                NotificationManagerCompat.from(this@ScreenTimeMonitorService)
                    .notify(GuardianNotifications.MONITOR_NOTIFICATION_ID, updatedNotification)
            } catch (_: SecurityException) {
            }
        }

        if (finalReason != null && usage.pendingAlertReason == null && now - lastNotifiedAlertAtMillis > 60_000L) {
            lastNotifiedAlertAtMillis = now
            repository.setPendingAlertReason(finalReason)
            val sourceTag = if (usageSnapshot.hasPermission) "UsageStatsManager" else "Bộ đếm nền"
            val noteText = when (finalReason) {
                AlertReason.TIME_LIMIT_EXCEEDED ->
                    "$sourceTag phát hiện đã dùng $elapsedMins/$effectiveLimitMins phút"
                AlertReason.APP_LIMIT_EXCEEDED ->
                    "Ứng dụng ${exceededAppRule?.appName.orEmpty()} vượt quá ${exceededAppRule?.dailyLimitMinutes ?: 30} phút"
                AlertReason.BEDTIME_CURFEW ->
                    "Phát hiện dùng máy trong khung giờ ngủ (${settings.formattedBedtimeWindow()})"
                AlertReason.FOCUS_MODE_LOCK ->
                    "Đang khóa theo chế độ ${usage.focusLockMode.label}"
                AlertReason.QUICK_LOCK ->
                    "Phụ huynh vừa bật Khóa ngay"
                AlertReason.CHILD_DETECTED ->
                    "Phát hiện trẻ em xem quá lâu"
                AlertReason.MANUAL_TEST -> "Kiểm tra thủ công"
            }
            repository.appendAlertLog(
                AlertLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = now,
                    reason = finalReason,
                    scareTheme = settings.scareTheme,
                    elapsedMinutes = elapsedMins,
                    note = noteText
                )
            )
            GuardianNotifications.showLimitExceededNotification(
                context = this@ScreenTimeMonitorService,
                reason = finalReason,
                scareTheme = settings.scareTheme,
                elapsedMinutes = elapsedMins,
                maxMinutes = effectiveLimitMins,
                detailOverride = noteText
            )
        }
    }

    private fun checkScreenInteractiveAndUnlocked(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isInteractive = powerManager?.isInteractive ?: true
        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        return isInteractive && !isLocked
    }

    private fun stopMonitoringAndSelf() {
        monitorJob?.cancel()
        monitorJob = null
        serviceScope.launch {
            repository.setBackgroundServiceRunning(false)
            ServiceCompat.stopForeground(this@ScreenTimeMonitorService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: Throwable) {
        }
        monitorJob?.cancel()
        serviceScope.launch {
            repository.setBackgroundServiceRunning(false)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
