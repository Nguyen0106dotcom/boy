package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.AlertLogEntry
import com.example.domain.model.AlertReason
import com.example.domain.model.AppLimitRule
import com.example.domain.model.DailyUsageBar
import com.example.domain.model.LogEventType
import com.example.domain.model.ScareTheme

@Entity(tableName = "app_limit_rules")
data class AppLimitRuleEntity(
    @PrimaryKey val id: String,
    val appName: String,
    val packageNamesCsv: String,
    val dailyLimitMinutes: Int,
    val enabled: Boolean,
    val sortOrder: Int = 0
) {
    fun toDomain(): AppLimitRule = AppLimitRule(
        id = id,
        appName = appName,
        packageNames = packageNamesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        dailyLimitMinutes = dailyLimitMinutes,
        enabled = enabled
    )

    companion object {
        fun fromDomain(rule: AppLimitRule, sortOrder: Int = 0): AppLimitRuleEntity = AppLimitRuleEntity(
            id = rule.id,
            appName = rule.appName,
            packageNamesCsv = rule.packageNames.joinToString(","),
            dailyLimitMinutes = rule.dailyLimitMinutes,
            enabled = rule.enabled,
            sortOrder = sortOrder
        )
    }
}

@Entity(tableName = "alert_logs")
data class AlertLogEntity(
    @PrimaryKey val id: String,
    val timestampMillis: Long,
    val reasonName: String,
    val scareThemeName: String,
    val elapsedMinutes: Int,
    val note: String,
    val eventTypeName: String
) {
    fun toDomain(): AlertLogEntry {
        val reason = AlertReason.entries.firstOrNull { it.name == reasonName }
            ?: AlertReason.TIME_LIMIT_EXCEEDED
        val theme = ScareTheme.entries.firstOrNull { it.name == scareThemeName }
            ?: ScareTheme.GENTLE_EYECARE
        val eventType = LogEventType.entries.firstOrNull { it.name == eventTypeName }
            ?: reason.toLogEventType()
        return AlertLogEntry(
            id = id,
            timestampMillis = timestampMillis,
            reason = reason,
            scareTheme = theme,
            elapsedMinutes = elapsedMinutes,
            note = note,
            eventType = eventType
        )
    }

    companion object {
        fun fromDomain(entry: AlertLogEntry): AlertLogEntity = AlertLogEntity(
            id = entry.id,
            timestampMillis = entry.timestampMillis,
            reasonName = entry.reason.name,
            scareThemeName = entry.scareTheme.name,
            elapsedMinutes = entry.elapsedMinutes,
            note = entry.note,
            eventTypeName = entry.eventType.name
        )
    }
}

@Entity(tableName = "daily_usage_stats")
data class DailyUsageStatEntity(
    @PrimaryKey val dayIndex: Int,
    val dayLabel: String,
    val usedMinutes: Int,
    val limitMinutes: Int
) {
    fun toDomain(): DailyUsageBar = DailyUsageBar(
        dayLabel = dayLabel,
        usedMinutes = usedMinutes,
        limitMinutes = limitMinutes
    )
}
