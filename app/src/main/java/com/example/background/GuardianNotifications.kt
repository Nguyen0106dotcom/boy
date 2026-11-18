package com.example.background

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.AlertReason
import com.example.domain.model.ScareTheme

object GuardianNotifications {

    const val MONITOR_CHANNEL_ID = "kidguard_monitor_channel"
    const val ALERT_CHANNEL_ID = "kidguard_alert_channel"
    const val MONITOR_NOTIFICATION_ID = 1001
    const val ALERT_NOTIFICATION_ID = 1002
    const val EXTRA_ALERT_REASON = "extra_alert_reason"
    const val EXTRA_REQUEST_USAGE_ACCESS = "extra_request_usage_access"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val monitorChannel = NotificationChannel(
            MONITOR_CHANNEL_ID,
            "Giám sát thời gian màn hình",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Thông báo cố định giúp KidGuard đo thời gian nền qua UsageStatsManager."
            setShowBadge(false)
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Nhắc nghỉ mắt & giới hạn giờ",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thông báo khi vượt giới hạn thời gian, giới hạn ứng dụng hoặc giờ ngủ."
            enableVibration(true)
        }

        manager.createNotificationChannel(monitorChannel)
        manager.createNotificationChannel(alertChannel)
    }

    fun buildMonitorNotification(
        context: Context,
        elapsedSeconds: Long,
        maxMinutes: Int,
        isScreenActive: Boolean,
        hasUsageAccess: Boolean = true,
        currentForegroundApp: String? = null
    ): Notification {
        ensureChannels(context)
        val mins = elapsedSeconds / 60L
        val secs = elapsedSeconds % 60L
        val formatted = "%02d:%02d".format(mins, secs)

        val statusText = when {
            !hasUsageAccess ->
                "Chưa có quyền Usage Access • Chạm để cấp quyền đo giờ chính xác ($formatted)"
            !isScreenActive ->
                "Màn hình tắt • Đã dùng $formatted / $maxMinutes phút"
            !currentForegroundApp.isNullOrBlank() ->
                "Đang dùng: $currentForegroundApp • $formatted / $maxMinutes phút"
            else ->
                "UsageStatsManager đang đếm: $formatted / $maxMinutes phút"
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!hasUsageAccess) {
                putExtra(EXTRA_REQUEST_USAGE_ACCESS, true)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopServiceIntent = Intent(context, ScreenTimeMonitorService::class.java).apply {
            action = ScreenTimeMonitorService.ACTION_STOP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopServiceIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(
                if (hasUsageAccess) "KidGuard • Đang theo dõi thời gian" else "KidGuard • Cần quyền Usage Access"
            )
            .setContentText(statusText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Dừng nền", stopPendingIntent)
            .build()
    }

    fun showLimitExceededNotification(
        context: Context,
        reason: AlertReason,
        scareTheme: ScareTheme,
        elapsedMinutes: Int,
        maxMinutes: Int,
        detailOverride: String? = null
    ) {
        ensureChannels(context)
        if (!canPostNotifications(context)) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALERT_REASON, reason.name)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val detail = detailOverride ?: when (reason) {
            AlertReason.TIME_LIMIT_EXCEEDED ->
                "Đã dùng $elapsedMinutes/$maxMinutes phút hôm nay. Hãy nghỉ mắt 20 giây nhé!"
            AlertReason.APP_LIMIT_EXCEEDED ->
                "Ứng dụng giải trí đã chạm giới hạn hàng ngày."
            AlertReason.BEDTIME_CURFEW ->
                "Đã đến khung giờ ngủ buổi tối. Chạm để mở màn hình nhắc nhở."
            AlertReason.FOCUS_MODE_LOCK ->
                "Đang trong chế độ Giờ học / Giờ ăn cơm."
            AlertReason.QUICK_LOCK ->
                "Ba mẹ đã bật khóa màn hình mềm. Chạm để xem chi tiết."
            AlertReason.CHILD_DETECTED ->
                "Phát hiện bé đang xem điện thoại quá lâu. Hãy nghỉ mắt 20 giây!"
            AlertReason.MANUAL_TEST ->
                "Kiểm tra thông báo nghỉ mắt từ KidGuard."
        }

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(scareTheme.scareHeadline)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$detail\n${scareTheme.scareSubtitle}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ALERT_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
