package com.example.domain.model

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import com.example.R
import java.util.Calendar

enum class AppMode {
    KID,
    PARENT
}

enum class ScareTheme(
    val displayName: String,
    val scareHeadline: String,
    val scareSubtitle: String,
    val spokenWarning: String,
    val isGentleMode: Boolean,
    @param:DrawableRes val drawableRes: Int
) {
    GENTLE_EYECARE(
        displayName = "Nhắc nghỉ mắt 20s (Dịu nhẹ)",
        scareHeadline = "ĐẾN GIỜ NGHỈ MẮT RỒI!",
        scareSubtitle = "Bé đã xem đủ thời gian quy định. Hãy đứng dậy vươn vai và nhìn ra xa 20 giây để bảo vệ đôi mắt sáng nhé!",
        spokenWarning = "Bé ơi, mình đã dùng điện thoại đủ giờ rồi. Hãy đặt máy xuống, đứng dậy vươn vai và nhìn ra xa hai mươi giây để bảo vệ đôi mắt nhé!",
        isGentleMode = true,
        drawableRes = R.drawable.img_owl_night
    ),
    NIGHT_OWL(
        displayName = "Cú Đêm giờ ngủ (Êm ái)",
        scareHeadline = "ĐẾN GIỜ ĐI NGỦ RỒI!",
        scareSubtitle = "Đã vào khung giờ nghỉ ngơi buổi tối. Cú Đêm chúc bé ngủ ngon, hẹn gặp lại vào sáng mai nhé!",
        spokenWarning = "Cú đêm nhắc nhẹ, đã đến khung giờ ngủ buổi tối rồi. Bé hãy tắt điện thoại và đi ngủ thật ngon nhé!",
        isGentleMode = true,
        drawableRes = R.drawable.img_owl_night
    ),
    SPOOKY_MONSTER(
        displayName = "Cảnh báo nghiêm (Ngáo Ộp)",
        scareHeadline = "HẾT GIỜ XEM ĐIỆN THOẠI!",
        scareSubtitle = "Đã vượt quá giới hạn quy định! Hãy cất điện thoại ngay lập tức để bảo vệ mắt.",
        spokenWarning = "Cảnh báo! Đã hết giờ sử dụng điện thoại. Bé hãy cất điện thoại xuống ngay lập tức!",
        isGentleMode = false,
        drawableRes = R.drawable.img_monster_spooky
    ),
    RED_SIREN(
        displayName = "Báo động hết giờ",
        scareHeadline = "ĐÃ HẾT THỜI GIAN CHO PHÉP!",
        scareSubtitle = "Thiết bị đang tạm khóa vì đã vượt quá thời lượng quy định.",
        spokenWarning = "Đã hết thời gian cho phép sử dụng điện thoại. Vui lòng đưa máy cho ba mẹ!",
        isGentleMode = false,
        drawableRes = R.drawable.img_siren_robot
    )
}

enum class CameraScanPolicy(
    val shortLabel: String,
    val label: String,
    val description: String,
    val intervalMillis: Long?
) {
    SPARSE_10S(
        shortLabel = "10s",
        label = "Làm mới mỗi 10 giây",
        description = "Kiểm tra khuôn mặt thưa mỗi 10 giây khi camera đang bật.",
        intervalMillis = 10_000L
    ),
    SPARSE_30S(
        shortLabel = "30s",
        label = "Làm mới mỗi 30 giây",
        description = "Chụp thưa 30 giây/lần để ước lượng mềm trẻ em hay người lớn.",
        intervalMillis = 30_000L
    ),
    SPARSE_60S(
        shortLabel = "60s",
        label = "Làm mới mỗi 60 giây",
        description = "Tiết kiệm pin cao: chỉ kiểm tra camera mỗi phút một lần.",
        intervalMillis = 60_000L
    ),
    THRESHOLD_ONLY(
        shortLabel = "Tạm dừng",
        label = "Tạm dừng (Chỉ chụp khi vượt ngưỡng)",
        description = "Tiết kiệm pin tối đa & bảo vệ riêng tư: chỉ chụp 1 khung hình khi hết giờ.",
        intervalMillis = null
    )
}

enum class DetectionSensitivity(val label: String, val confidenceThreshold: Int) {
    LOW("Thấp", 80),
    MEDIUM("Trung bình", 65),
    HIGH("Cao", 50)
}

enum class FocusLockMode(val label: String, val defaultMinutes: Int) {
    NONE("Bình thường", 0),
    STUDY("Giờ học bài", 45),
    MEAL("Giờ ăn cơm", 30)
}

enum class DetectedSubject(val label: String) {
    CHILD("Trẻ em"),
    ADULT("Người lớn"),
    NONE("Chưa phát hiện")
}

enum class DemoTargetProfile(val label: String) {
    SIMULATE_CHILD("Mẫu AI: Trẻ em"),
    SIMULATE_ADULT("Mẫu AI: Người lớn")
}

data class ChildDetectionResult(
    val subject: DetectedSubject = DetectedSubject.NONE,
    val faceCount: Int = 0,
    val confidencePercent: Int = 0,
    val summary: String = "Độ tuổi chỉ là ước lượng mềm — không lưu hình ảnh vào bộ nhớ.",
    val sourceModeLabel: String = "MÔ PHỎNG",
    val isPlaceholderAi: Boolean = true,
    val errorMessage: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

enum class LogEventType(val label: String) {
    ALL("Tất cả"),
    THRESHOLD_EXCEEDED("Vượt ngưỡng"),
    CHILD_DETECTED("Phát hiện trẻ em"),
    SCREEN_LOCKED("Khóa màn hình"),
    EXTRA_TIME("Xin thêm giờ")
}

enum class DateFilter(val label: String) {
    ALL("Tất cả"),
    TODAY("Hôm nay"),
    LAST_7_DAYS("7 ngày qua")
}

enum class AlertReason(val label: String) {
    TIME_LIMIT_EXCEEDED("Hết thời gian sử dụng hôm nay"),
    APP_LIMIT_EXCEEDED("Vượt giới hạn ứng dụng"),
    BEDTIME_CURFEW("Đang trong khung giờ ngủ"),
    FOCUS_MODE_LOCK("Đang khóa giờ học / giờ ăn"),
    QUICK_LOCK("Khóa màn hình ngay bởi phụ huynh"),
    CHILD_DETECTED("Phát hiện trẻ em đang xem quá lâu"),
    MANUAL_TEST("Kiểm tra thông báo nghỉ ngơi");

    fun toLogEventType(): LogEventType = when (this) {
        TIME_LIMIT_EXCEEDED, APP_LIMIT_EXCEEDED -> LogEventType.THRESHOLD_EXCEEDED
        CHILD_DETECTED -> LogEventType.CHILD_DETECTED
        BEDTIME_CURFEW, FOCUS_MODE_LOCK, QUICK_LOCK -> LogEventType.SCREEN_LOCKED
        MANUAL_TEST -> LogEventType.THRESHOLD_EXCEEDED
    }
}

data class ActiveAlert(
    val reason: AlertReason,
    val scareTheme: ScareTheme,
    val triggeredAtMillis: Long,
    val elapsedMinutes: Int,
    val detailText: String = "",
    val isSoftReminder: Boolean = false
)

data class AppLimitRule(
    val id: String,
    val appName: String,
    val packageNames: List<String>,
    val dailyLimitMinutes: Int,
    val enabled: Boolean = true
) {
    companion object {
        fun defaults(): List<AppLimitRule> = listOf(
            AppLimitRule(
                id = "youtube",
                appName = "YouTube",
                packageNames = listOf("com.google.android.youtube", "com.google.android.apps.youtube.kids"),
                dailyLimitMinutes = 30,
                enabled = true
            ),
            AppLimitRule(
                id = "tiktok",
                appName = "TikTok",
                packageNames = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
                dailyLimitMinutes = 20,
                enabled = true
            ),
            AppLimitRule(
                id = "games",
                appName = "Trò chơi (Roblox / Free Fire)",
                packageNames = listOf("com.roblox.client", "com.dts.freefireth", "com.mojang.minecraftpe"),
                dailyLimitMinutes = 30,
                enabled = true
            ),
            AppLimitRule(
                id = "reels",
                appName = "Facebook / Reels",
                packageNames = listOf("com.facebook.katana", "com.instagram.android"),
                dailyLimitMinutes = 15,
                enabled = false
            )
        )

        fun sampleCatalog(): List<AppLimitRule> = listOf(
            AppLimitRule(
                id = "yt_kids",
                appName = "YouTube Kids",
                packageNames = listOf("com.google.android.apps.youtube.kids"),
                dailyLimitMinutes = 25,
                enabled = true
            ),
            AppLimitRule(
                id = "netflix",
                appName = "Netflix",
                packageNames = listOf("com.netflix.mediaclient"),
                dailyLimitMinutes = 40,
                enabled = true
            ),
            AppLimitRule(
                id = "brawl_stars",
                appName = "Brawl Stars / Liên Quân",
                packageNames = listOf("com.supercell.brawlstars", "com.garena.game.kgvn"),
                dailyLimitMinutes = 25,
                enabled = true
            ),
            AppLimitRule(
                id = "chrome",
                appName = "Trình duyệt Chrome",
                packageNames = listOf("com.android.chrome"),
                dailyLimitMinutes = 30,
                enabled = true
            )
        )
    }
}

data class AppUsageEntry(
    val appName: String,
    val packageName: String,
    val usedMinutes: Int,
    val limitMinutes: Int? = null
)

data class DailyUsageBar(
    val dayLabel: String,
    val usedMinutes: Int,
    val limitMinutes: Int
)

enum class ExtraTimeStatus {
    PENDING,
    APPROVED,
    DENIED
}

data class ExtraTimeRequest(
    val id: String,
    val requestedAtMillis: Long,
    val minutesRequested: Int,
    val status: ExtraTimeStatus
)

data class GuardSettings(
    val maxMinutes: Int = 45,
    val weekendMaxMinutes: Int = 75,
    val bedtimeEnabled: Boolean = true,
    val bedtimeStartHour: Int = 21,
    val bedtimeStartMinute: Int = 0,
    val bedtimeEndHour: Int = 6,
    val bedtimeEndMinute: Int = 0,
    val scareTheme: ScareTheme = ScareTheme.GENTLE_EYECARE,
    val soundEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val parentPin: String = "1234",
    val isGuardArmed: Boolean = true,
    val demoTargetProfile: DemoTargetProfile = DemoTargetProfile.SIMULATE_CHILD,
    val cameraScanPolicy: CameraScanPolicy = CameraScanPolicy.SPARSE_30S,
    val appLimits: List<AppLimitRule> = AppLimitRule.defaults(),
    val isDarkTheme: Boolean = true,
    val aiDetectionEnabled: Boolean = true,
    val detectionSensitivity: DetectionSensitivity = DetectionSensitivity.MEDIUM,
    val warnAt10Min: Boolean = true,
    val warnAt5Min: Boolean = true,
    val warnAtLimit: Boolean = true,
    val hasCompletedInitialPinSetup: Boolean = false
) {
    fun activeLimitForDay(calendar: Calendar = Calendar.getInstance()): Int {
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = day == Calendar.SATURDAY || day == Calendar.SUNDAY
        return if (isWeekend) weekendMaxMinutes else maxMinutes
    }

    fun isWeekend(calendar: Calendar = Calendar.getInstance()): Boolean {
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY
    }

    fun formattedBedtimeWindow(): String {
        return "%02d:%02d – %02d:%02d".format(
            bedtimeStartHour,
            bedtimeStartMinute,
            bedtimeEndHour,
            bedtimeEndMinute
        )
    }

    fun isCurrentlyInBedtime(calendar: Calendar = Calendar.getInstance()): Boolean {
        if (!bedtimeEnabled) return false
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = bedtimeStartHour * 60 + bedtimeStartMinute
        val endMinutes = bedtimeEndHour * 60 + bedtimeEndMinute
        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }
}

data class AlertLogEntry(
    val id: String,
    val timestampMillis: Long,
    val reason: AlertReason,
    val scareTheme: ScareTheme,
    val elapsedMinutes: Int,
    val note: String,
    val eventType: LogEventType = reason.toLogEventType()
)

data class BackgroundUsageState(
    val accumulatedUsageSeconds: Long = 0L,
    val lastScreenCheckMillis: Long = 0L,
    val isBackgroundServiceRunning: Boolean = false,
    val lastBackgroundCheckSummary: String = "WorkManager sẵn sàng • Kiểm tra mỗi 15 phút",
    val pendingAlertReason: AlertReason? = null,
    val curfewSnoozedUntilMillis: Long = 0L,
    val focusLockMode: FocusLockMode = FocusLockMode.NONE,
    val focusLockUntilMillis: Long = 0L,
    val extraMinutesGrantedToday: Int = 0,
    val pendingExtraTimeRequest: ExtraTimeRequest? = null,
    val pauseMonitorUntilMillis: Long = 0L,
    val softLockActive: Boolean = false
)

data class ScreenTimeEvaluation(
    val updatedSeconds: Long,
    val limitSeconds: Long,
    val isTimeLimitExceeded: Boolean,
    val isBedtimeViolated: Boolean,
    val isFocusLocked: Boolean,
    val isQuickLocked: Boolean = false,
    val isTemporarilyPaused: Boolean = false,
    val approachingWarningMinutes: Int? = null,
    val triggeredReason: AlertReason?
)

object ScreenTimeLimitEvaluator {
    fun evaluate(
        accumulatedSeconds: Long,
        settings: GuardSettings,
        nowMillis: Long = System.currentTimeMillis(),
        curfewSnoozedUntilMillis: Long = 0L,
        focusLockMode: FocusLockMode = FocusLockMode.NONE,
        focusLockUntilMillis: Long = 0L,
        extraMinutesGrantedToday: Int = 0,
        pauseMonitorUntilMillis: Long = 0L,
        softLockActive: Boolean = false,
        calendar: Calendar = Calendar.getInstance()
    ): ScreenTimeEvaluation {
        val effectiveLimitMinutes = (settings.activeLimitForDay(calendar) + extraMinutesGrantedToday)
            .coerceAtLeast(1)
        val limitSeconds = effectiveLimitMinutes * 60L
        val remainingSeconds = (limitSeconds - accumulatedSeconds).coerceAtLeast(0L)

        val isTemporarilyPaused = nowMillis < pauseMonitorUntilMillis
        val isQuickLocked = softLockActive

        if (isTemporarilyPaused && !isQuickLocked) {
            return ScreenTimeEvaluation(
                updatedSeconds = accumulatedSeconds,
                limitSeconds = limitSeconds,
                isTimeLimitExceeded = false,
                isBedtimeViolated = false,
                isFocusLocked = false,
                isQuickLocked = false,
                isTemporarilyPaused = true,
                approachingWarningMinutes = null,
                triggeredReason = null
            )
        }

        val isFocusLocked = settings.isGuardArmed &&
            focusLockMode != FocusLockMode.NONE &&
            nowMillis < focusLockUntilMillis

        val isTimeLimitExceeded = settings.isGuardArmed &&
            settings.warnAtLimit &&
            accumulatedSeconds >= limitSeconds

        val isBedtimeViolated = settings.isGuardArmed &&
            settings.isCurrentlyInBedtime(calendar) &&
            nowMillis > curfewSnoozedUntilMillis

        val approachingWarningMinutes = if (settings.isGuardArmed && !isTimeLimitExceeded) {
            when {
                settings.warnAt5Min && remainingSeconds in 1L..(5 * 60L) -> 5
                settings.warnAt10Min && remainingSeconds in (5 * 60L + 1L)..(10 * 60L) -> 10
                else -> null
            }
        } else {
            null
        }

        val reason = when {
            isQuickLocked -> AlertReason.QUICK_LOCK
            isFocusLocked -> AlertReason.FOCUS_MODE_LOCK
            isBedtimeViolated -> AlertReason.BEDTIME_CURFEW
            isTimeLimitExceeded -> AlertReason.TIME_LIMIT_EXCEEDED
            else -> null
        }

        return ScreenTimeEvaluation(
            updatedSeconds = accumulatedSeconds,
            limitSeconds = limitSeconds,
            isTimeLimitExceeded = isTimeLimitExceeded,
            isBedtimeViolated = isBedtimeViolated,
            isFocusLocked = isFocusLocked,
            isQuickLocked = isQuickLocked,
            isTemporarilyPaused = false,
            approachingWarningMinutes = approachingWarningMinutes,
            triggeredReason = reason
        )
    }
}

enum class ThresholdVerificationPhase(val label: String) {
    IDLE("Sẵn sàng kiểm tra khi hết giờ"),
    CAPTURING_FRAME("Camera đang chụp 1 khung hình kiểm tra..."),
    VERIFIED_CHILD("Ước lượng mềm: Trẻ em — Hiển thị nhắc nghỉ mắt"),
    VERIFIED_ADULT_EXEMPT("Ước lượng mềm: Người lớn — Tạm hoãn nhắc nhở")
}

data class ThresholdCaptureState(
    val phase: ThresholdVerificationPhase = ThresholdVerificationPhase.IDLE,
    val capturedThumbnail: Bitmap? = null,
    val capturedAtMillis: Long? = null,
    val detailMessage: String = "Camera chỉ chụp khung hình thưa khi cần, không lưu ảnh vào bộ nhớ."
)
