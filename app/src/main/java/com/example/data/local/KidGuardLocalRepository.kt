package com.example.data.local

import android.content.Context
import com.example.domain.model.AlertLogEntry
import com.example.domain.model.AlertReason
import com.example.domain.model.AppLimitRule
import com.example.domain.model.AppUsageEntry
import com.example.domain.model.DailyUsageBar
import com.example.domain.model.LogEventType
import com.example.domain.model.ScareTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Room-backed Repository for app rules, alert/activity history logs, and weekly statistics.
 * Pre-populates rich sample data on first run so all screens are immediately populated.
 */
class KidGuardLocalRepository(context: Context) {

    private val dao: KidGuardDao = KidGuardDatabase.getInstance(context).kidGuardDao()

    val appRulesFlow: Flow<List<AppLimitRule>> = dao.getAppRulesFlow().map { list ->
        list.map { it.toDomain() }
    }

    val alertLogsFlow: Flow<List<AlertLogEntry>> = dao.getAlertLogsFlow().map { list ->
        list.map { it.toDomain() }
    }

    val weeklyStatsFlow: Flow<List<DailyUsageBar>> = dao.getWeeklyStatsFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun seedInitialSampleDataIfNeeded(
        alreadySeeded: Boolean,
        markSeeded: suspend () -> Unit
    ) {
        if (alreadySeeded) return

        val ruleEntities = AppLimitRule.defaults().mapIndexed { idx, rule ->
            AppLimitRuleEntity.fromDomain(rule, sortOrder = idx)
        }
        dao.upsertAppRules(ruleEntities)

        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val day = 24 * hour
        val sampleLogs = listOf(
            AlertLogEntry(
                id = "sample_1",
                timestampMillis = now - 25 * 60_000L,
                reason = AlertReason.CHILD_DETECTED,
                scareTheme = ScareTheme.GENTLE_EYECARE,
                elapsedMinutes = 6,
                note = "Camera trước ước lượng mềm: Trẻ em đang xem YouTube (không lưu ảnh).",
                eventType = LogEventType.CHILD_DETECTED
            ),
            AlertLogEntry(
                id = "sample_2",
                timestampMillis = now - 2 * hour,
                reason = AlertReason.MANUAL_TEST,
                scareTheme = ScareTheme.GENTLE_EYECARE,
                elapsedMinutes = 40,
                note = "Mốc cảnh báo còn 5 phút • Nhắc bé chuẩn bị nghỉ mắt 20 giây.",
                eventType = LogEventType.THRESHOLD_EXCEEDED
            ),
            AlertLogEntry(
                id = "sample_3",
                timestampMillis = now - 3 * hour,
                reason = AlertReason.MANUAL_TEST,
                scareTheme = ScareTheme.GENTLE_EYECARE,
                elapsedMinutes = 15,
                note = "Bé gửi yêu cầu xin thêm +15 phút (Ba mẹ đã duyệt).",
                eventType = LogEventType.EXTRA_TIME
            ),
            AlertLogEntry(
                id = "sample_4",
                timestampMillis = now - 1 * day,
                reason = AlertReason.TIME_LIMIT_EXCEEDED,
                scareTheme = ScareTheme.GENTLE_EYECARE,
                elapsedMinutes = 45,
                note = "Chạm ngưỡng 45/45 phút ngày học • Hiển thị màn hình nhắc nghỉ mắt.",
                eventType = LogEventType.THRESHOLD_EXCEEDED
            ),
            AlertLogEntry(
                id = "sample_5",
                timestampMillis = now - 1 * day - 3 * hour,
                reason = AlertReason.BEDTIME_CURFEW,
                scareTheme = ScareTheme.NIGHT_OWL,
                elapsedMinutes = 32,
                note = "Mở máy trong khung giờ ngủ (21:00 – 06:00) • Bật khóa mềm ban đêm.",
                eventType = LogEventType.SCREEN_LOCKED
            ),
            AlertLogEntry(
                id = "sample_6",
                timestampMillis = now - 2 * day,
                reason = AlertReason.APP_LIMIT_EXCEEDED,
                scareTheme = ScareTheme.GENTLE_EYECARE,
                elapsedMinutes = 20,
                note = "TikTok đạt giới hạn 20 phút/ngày.",
                eventType = LogEventType.THRESHOLD_EXCEEDED
            )
        )
        dao.upsertAlertLogs(sampleLogs.map { AlertLogEntity.fromDomain(it) })

        val sampleWeekly = buildDefaultWeeklyStats(45, 75)
        dao.upsertWeeklyStats(sampleWeekly)

        markSeeded()
    }

    suspend fun upsertAppRule(rule: AppLimitRule, sortOrder: Int = 99) {
        dao.upsertAppRule(AppLimitRuleEntity.fromDomain(rule, sortOrder))
    }

    suspend fun deleteAppRule(ruleId: String) {
        dao.deleteAppRuleById(ruleId)
    }

    suspend fun appendLog(entry: AlertLogEntry) {
        dao.upsertAlertLog(AlertLogEntity.fromDomain(entry))
    }

    suspend fun clearLogs() {
        dao.clearAlertLogs()
    }

    suspend fun clearAllRoomData() {
        dao.clearAlertLogs()
        dao.clearWeeklyStats()
    }

    suspend fun restoreDefaultAppRules() {
        dao.clearAppRules()
        val ruleEntities = AppLimitRule.defaults().mapIndexed { idx, rule ->
            AppLimitRuleEntity.fromDomain(rule, sortOrder = idx)
        }
        dao.upsertAppRules(ruleEntities)
    }

    fun buildDefaultWeeklyStats(weekdayLimit: Int, weekendLimit: Int): List<DailyUsageStatEntity> {
        val dayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val sampleMinutes = listOf(34, 48, 39, 55, 42, 68, 29)
        val cal = Calendar.getInstance()
        return (-6..0).mapIndexed { index, offset ->
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val dow = dayCal.get(Calendar.DAY_OF_WEEK)
            val labelIdx = when (dow) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                else -> 6
            }
            val isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
            DailyUsageStatEntity(
                dayIndex = index,
                dayLabel = dayNames[labelIdx],
                usedMinutes = sampleMinutes[index],
                limitMinutes = if (isWeekend) weekendLimit else weekdayLimit
            )
        }
    }

    companion object {
        fun sampleTopAppsPreview(): List<AppUsageEntry> = listOf(
            AppUsageEntry("YouTube", "com.google.android.youtube", usedMinutes = 18, limitMinutes = 30),
            AppUsageEntry("TikTok", "com.zhiliaoapp.musically", usedMinutes = 12, limitMinutes = 20),
            AppUsageEntry("Roblox", "com.roblox.client", usedMinutes = 14, limitMinutes = 30),
            AppUsageEntry("Duolingo", "com.duolingo", usedMinutes = 9, limitMinutes = null)
        )
    }
}
