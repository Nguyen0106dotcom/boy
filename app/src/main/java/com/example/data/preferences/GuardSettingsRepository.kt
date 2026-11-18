package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AlertLogEntry
import com.example.domain.model.AlertReason
import com.example.domain.model.AppLimitRule
import com.example.domain.model.BackgroundUsageState
import com.example.domain.model.CameraScanPolicy
import com.example.domain.model.DemoTargetProfile
import com.example.domain.model.DetectionSensitivity
import com.example.domain.model.ExtraTimeRequest
import com.example.domain.model.ExtraTimeStatus
import com.example.domain.model.FocusLockMode
import com.example.domain.model.GuardSettings
import com.example.domain.model.LogEventType
import com.example.domain.model.ScareTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private val Context.kidGuardDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kidguard_preferences"
)

class GuardSettingsRepository(private val context: Context) {

    private object Keys {
        val MAX_MINUTES = intPreferencesKey("max_minutes")
        val WEEKEND_MAX_MINUTES = intPreferencesKey("weekend_max_minutes")
        val BEDTIME_ENABLED = booleanPreferencesKey("bedtime_enabled")
        val BEDTIME_START_HOUR = intPreferencesKey("bedtime_start_hour")
        val BEDTIME_START_MINUTE = intPreferencesKey("bedtime_start_minute")
        val BEDTIME_END_HOUR = intPreferencesKey("bedtime_end_hour")
        val BEDTIME_END_MINUTE = intPreferencesKey("bedtime_end_minute")
        val SCARE_THEME = stringPreferencesKey("scare_theme")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val PARENT_PIN = stringPreferencesKey("parent_pin")
        val GUARD_ARMED = booleanPreferencesKey("guard_armed")
        val DEMO_TARGET_PROFILE = stringPreferencesKey("demo_target_profile")
        val CAMERA_SCAN_POLICY = stringPreferencesKey("camera_scan_policy")
        val APP_LIMITS_JSON = stringPreferencesKey("app_limits_json")
        val ALERT_LOGS_JSON = stringPreferencesKey("alert_logs_json")

        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val AI_DETECTION_ENABLED = booleanPreferencesKey("ai_detection_enabled")
        val DETECTION_SENSITIVITY = stringPreferencesKey("detection_sensitivity")
        val WARN_AT_10_MIN = booleanPreferencesKey("warn_at_10_min")
        val WARN_AT_5_MIN = booleanPreferencesKey("warn_at_5_min")
        val WARN_AT_LIMIT = booleanPreferencesKey("warn_at_limit")
        val HAS_COMPLETED_INITIAL_PIN = booleanPreferencesKey("has_completed_initial_pin")
        val SAMPLE_DATA_SEEDED = booleanPreferencesKey("sample_data_seeded")

        val ACCUMULATED_USAGE_SECONDS = longPreferencesKey("accumulated_usage_seconds")
        val LAST_SCREEN_CHECK_MILLIS = longPreferencesKey("last_screen_check_millis")
        val BACKGROUND_SERVICE_RUNNING = booleanPreferencesKey("background_service_running")
        val LAST_BACKGROUND_CHECK_SUMMARY = stringPreferencesKey("last_background_check_summary")
        val PENDING_ALERT_REASON = stringPreferencesKey("pending_alert_reason")
        val CURFEW_SNOOZED_UNTIL_MILLIS = longPreferencesKey("curfew_snoozed_until_millis")
        val FOCUS_LOCK_MODE = stringPreferencesKey("focus_lock_mode")
        val FOCUS_LOCK_UNTIL_MILLIS = longPreferencesKey("focus_lock_until_millis")
        val EXTRA_MINUTES_GRANTED = intPreferencesKey("extra_minutes_granted")
        val EXTRA_TIME_REQUEST_JSON = stringPreferencesKey("extra_time_request_json")
        val PAUSE_MONITOR_UNTIL_MILLIS = longPreferencesKey("pause_monitor_until_millis")
        val SOFT_LOCK_ACTIVE = booleanPreferencesKey("soft_lock_active")
    }

    val settingsFlow: Flow<GuardSettings> = context.kidGuardDataStore.data.map { prefs ->
        parseSettings(prefs)
    }

    val usageStateFlow: Flow<BackgroundUsageState> = context.kidGuardDataStore.data.map { prefs ->
        val pendingName = prefs[Keys.PENDING_ALERT_REASON]
        val pendingReason = pendingName?.let { name ->
            AlertReason.entries.firstOrNull { it.name == name }
        }
        val focusMode = FocusLockMode.entries.firstOrNull {
            it.name == prefs[Keys.FOCUS_LOCK_MODE]
        } ?: FocusLockMode.NONE

        // Seed 06:28 (388s) on initial preview launch before UsageStats overwrites it
        val initialSeconds = if (prefs.contains(Keys.ACCUMULATED_USAGE_SECONDS)) {
            (prefs[Keys.ACCUMULATED_USAGE_SECONDS] ?: 0L).coerceAtLeast(0L)
        } else {
            388L
        }

        BackgroundUsageState(
            accumulatedUsageSeconds = initialSeconds,
            lastScreenCheckMillis = prefs[Keys.LAST_SCREEN_CHECK_MILLIS] ?: 0L,
            isBackgroundServiceRunning = prefs[Keys.BACKGROUND_SERVICE_RUNNING] ?: false,
            lastBackgroundCheckSummary = prefs[Keys.LAST_BACKGROUND_CHECK_SUMMARY]
                ?: "WorkManager sẵn sàng • Kiểm tra mỗi 15 phút",
            pendingAlertReason = pendingReason,
            curfewSnoozedUntilMillis = prefs[Keys.CURFEW_SNOOZED_UNTIL_MILLIS] ?: 0L,
            focusLockMode = focusMode,
            focusLockUntilMillis = prefs[Keys.FOCUS_LOCK_UNTIL_MILLIS] ?: 0L,
            extraMinutesGrantedToday = prefs[Keys.EXTRA_MINUTES_GRANTED] ?: 0,
            pendingExtraTimeRequest = parseExtraTimeRequest(prefs[Keys.EXTRA_TIME_REQUEST_JSON].orEmpty()),
            pauseMonitorUntilMillis = prefs[Keys.PAUSE_MONITOR_UNTIL_MILLIS] ?: 0L,
            softLockActive = prefs[Keys.SOFT_LOCK_ACTIVE] ?: false
        )
    }

    val alertLogsFlow: Flow<List<AlertLogEntry>> = context.kidGuardDataStore.data.map { prefs ->
        parseLogsJson(prefs[Keys.ALERT_LOGS_JSON].orEmpty())
    }

    suspend fun getSettingsSnapshot(): GuardSettings = settingsFlow.first()

    suspend fun getUsageSnapshot(): BackgroundUsageState = usageStateFlow.first()

    suspend fun isSampleDataSeeded(): Boolean {
        return context.kidGuardDataStore.data.first()[Keys.SAMPLE_DATA_SEEDED] ?: false
    }

    suspend fun markSampleDataSeeded() {
        context.kidGuardDataStore.edit { prefs ->
            prefs[Keys.SAMPLE_DATA_SEEDED] = true
            if (!prefs.contains(Keys.ACCUMULATED_USAGE_SECONDS)) {
                prefs[Keys.ACCUMULATED_USAGE_SECONDS] = 388L
            }
        }
    }

    suspend fun updateSettings(transform: (GuardSettings) -> GuardSettings) {
        context.kidGuardDataStore.edit { prefs ->
            val current = parseSettings(prefs)
            val updated = transform(current)
            prefs[Keys.MAX_MINUTES] = updated.maxMinutes
            prefs[Keys.WEEKEND_MAX_MINUTES] = updated.weekendMaxMinutes
            prefs[Keys.BEDTIME_ENABLED] = updated.bedtimeEnabled
            prefs[Keys.BEDTIME_START_HOUR] = updated.bedtimeStartHour
            prefs[Keys.BEDTIME_START_MINUTE] = updated.bedtimeStartMinute
            prefs[Keys.BEDTIME_END_HOUR] = updated.bedtimeEndHour
            prefs[Keys.BEDTIME_END_MINUTE] = updated.bedtimeEndMinute
            prefs[Keys.SCARE_THEME] = updated.scareTheme.name
            prefs[Keys.SOUND_ENABLED] = updated.soundEnabled
            prefs[Keys.VOICE_ENABLED] = updated.voiceEnabled
            prefs[Keys.VIBRATION_ENABLED] = updated.vibrationEnabled
            prefs[Keys.PARENT_PIN] = updated.parentPin
            prefs[Keys.GUARD_ARMED] = updated.isGuardArmed
            prefs[Keys.DEMO_TARGET_PROFILE] = updated.demoTargetProfile.name
            prefs[Keys.CAMERA_SCAN_POLICY] = updated.cameraScanPolicy.name
            prefs[Keys.APP_LIMITS_JSON] = encodeAppLimits(updated.appLimits)
            prefs[Keys.IS_DARK_THEME] = updated.isDarkTheme
            prefs[Keys.AI_DETECTION_ENABLED] = updated.aiDetectionEnabled
            prefs[Keys.DETECTION_SENSITIVITY] = updated.detectionSensitivity.name
            prefs[Keys.WARN_AT_10_MIN] = updated.warnAt10Min
            prefs[Keys.WARN_AT_5_MIN] = updated.warnAt5Min
            prefs[Keys.WARN_AT_LIMIT] = updated.warnAtLimit
            prefs[Keys.HAS_COMPLETED_INITIAL_PIN] = updated.hasCompletedInitialPinSetup
        }
    }

    suspend fun setAccumulatedUsageSeconds(seconds: Long, checkMillis: Long = System.currentTimeMillis()) {
        context.kidGuardDataStore.edit { prefs ->
            prefs[Keys.ACCUMULATED_USAGE_SECONDS] = seconds.coerceAtLeast(0L)
            prefs[Keys.LAST_SCREEN_CHECK_MILLIS] = checkMillis
        }
    }

    suspend fun incrementUsageSeconds(deltaSeconds: Long, checkMillis: Long = System.currentTimeMillis()): Long {
        var nextTotal = 0L
        context.kidGuardDataStore.edit { prefs ->
            val fallback = if (prefs.contains(Keys.ACCUMULATED_USAGE_SECONDS)) 0L else 388L
            val current = (prefs[Keys.ACCUMULATED_USAGE_SECONDS] ?: fallback).coerceAtLeast(0L)
            nextTotal = current + deltaSeconds.coerceAtLeast(0L)
            prefs[Keys.ACCUMULATED_USAGE_SECONDS] = nextTotal
            prefs[Keys.LAST_SCREEN_CHECK_MILLIS] = checkMillis
        }
        return nextTotal
    }

    suspend fun resetDailyCountersIfNewDay(nowMillis: Long = System.currentTimeMillis()): Boolean {
        var didReset = false
        context.kidGuardDataStore.edit { prefs ->
            val lastCheck = prefs[Keys.LAST_SCREEN_CHECK_MILLIS] ?: 0L
            if (lastCheck > 0L) {
                val lastCal = Calendar.getInstance().apply { timeInMillis = lastCheck }
                val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                val isNewDay = lastCal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR) ||
                    lastCal.get(Calendar.DAY_OF_YEAR) != nowCal.get(Calendar.DAY_OF_YEAR)
                if (isNewDay) {
                    prefs[Keys.ACCUMULATED_USAGE_SECONDS] = 0L
                    prefs[Keys.EXTRA_MINUTES_GRANTED] = 0
                    prefs.remove(Keys.PENDING_ALERT_REASON)
                    prefs[Keys.LAST_SCREEN_CHECK_MILLIS] = nowMillis
                    didReset = true
                }
            }
        }
        return didReset
    }

    suspend fun setBackgroundServiceRunning(running: Boolean) {
        context.kidGuardDataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_SERVICE_RUNNING] = running
        }
    }

    suspend fun updateBackgroundCheckSummary(summary: String, checkMillis: Long = System.currentTimeMillis()) {
        context.kidGuardDataStore.edit { prefs ->
            prefs[Keys.LAST_BACKGROUND_CHECK_SUMMARY] = summary
            prefs[Keys.LAST_SCREEN_CHECK_MILLIS] = checkMillis
        }
    }

    suspend fun setPendingAlertReason(reason: AlertReason?) {
        context.kidGuardDataStore.edit { prefs ->
            if (reason == null) {
                prefs.remove(Keys.PENDING_ALERT_REASON)
            } else {
                prefs[Keys.PENDING_ALERT_REASON] = reason.name
            }
        }
    }

    suspend fun setCurfewSnoozedUntil(untilMillis: Long) {
        context.kidGuardDataStore.edit { prefs ->
            prefs[Keys.CURFEW_SNOOZED_UNTIL_MILLIS] = untilMillis
        }
    }

    suspend fun setFocusLock(mode: FocusLockMode, untilMillis: Long) {
        context.kidGuardDataStore.edit { prefs ->
            prefs[Keys.FOCUS_LOCK_MODE] = mode.name
            prefs[Keys.FOCUS_LOCK_UNTIL_MILLIS] = untilMillis
        }
    }

    suspend fun setPauseMonitorUntil(untilMillis: Long) {
        context.kidGuardDataStore.edit { prefs ->
            prefs[Keys.PAUSE_MONITOR_UNTIL_MILLIS] = untilMillis
        }
    }

    suspend fun setSoftLockActive(active: Boolean) {
        context.kidGuardDataStore.edit { prefs ->
            prefs[Keys.SOFT_LOCK_ACTIVE] = active
        }
    }

    suspend fun setExtraMinutesGrantedToday(minutes: Int) {
        context.kidGuardDataStore.edit { prefs ->
            prefs[Keys.EXTRA_MINUTES_GRANTED] = minutes.coerceAtLeast(0)
        }
    }

    suspend fun saveExtraTimeRequest(request: ExtraTimeRequest?) {
        context.kidGuardDataStore.edit { prefs ->
            if (request == null) {
                prefs.remove(Keys.EXTRA_TIME_REQUEST_JSON)
            } else {
                val obj = JSONObject().apply {
                    put("id", request.id)
                    put("requestedAtMillis", request.requestedAtMillis)
                    put("minutesRequested", request.minutesRequested)
                    put("status", request.status.name)
                }
                prefs[Keys.EXTRA_TIME_REQUEST_JSON] = obj.toString()
            }
        }
    }

    suspend fun appendAlertLog(entry: AlertLogEntry) {
        context.kidGuardDataStore.edit { prefs ->
            val current = parseLogsJson(prefs[Keys.ALERT_LOGS_JSON].orEmpty())
            val next = (listOf(entry) + current).take(50)
            prefs[Keys.ALERT_LOGS_JSON] = encodeLogsJson(next)
        }
    }

    suspend fun clearAlertLogs() {
        context.kidGuardDataStore.edit { prefs ->
            prefs[Keys.ALERT_LOGS_JSON] = "[]"
        }
    }

    suspend fun clearUsageAndHistoryData() {
        context.kidGuardDataStore.edit { prefs ->
            prefs[Keys.ACCUMULATED_USAGE_SECONDS] = 0L
            prefs[Keys.EXTRA_MINUTES_GRANTED] = 0
            prefs[Keys.PAUSE_MONITOR_UNTIL_MILLIS] = 0L
            prefs[Keys.SOFT_LOCK_ACTIVE] = false
            prefs[Keys.FOCUS_LOCK_MODE] = FocusLockMode.NONE.name
            prefs[Keys.FOCUS_LOCK_UNTIL_MILLIS] = 0L
            prefs[Keys.ALERT_LOGS_JSON] = "[]"
            prefs.remove(Keys.PENDING_ALERT_REASON)
            prefs.remove(Keys.EXTRA_TIME_REQUEST_JSON)
        }
    }

    private fun parseSettings(prefs: Preferences): GuardSettings {
        val themeName = prefs[Keys.SCARE_THEME] ?: ScareTheme.GENTLE_EYECARE.name
        val theme = ScareTheme.entries.firstOrNull { it.name == themeName } ?: ScareTheme.GENTLE_EYECARE

        val profileName = prefs[Keys.DEMO_TARGET_PROFILE] ?: DemoTargetProfile.SIMULATE_CHILD.name
        val profile = DemoTargetProfile.entries.firstOrNull { it.name == profileName }
            ?: DemoTargetProfile.SIMULATE_CHILD

        val scanPolicyName = prefs[Keys.CAMERA_SCAN_POLICY] ?: CameraScanPolicy.SPARSE_30S.name
        val scanPolicy = CameraScanPolicy.entries.firstOrNull { it.name == scanPolicyName }
            ?: CameraScanPolicy.SPARSE_30S

        val sensitivityName = prefs[Keys.DETECTION_SENSITIVITY] ?: DetectionSensitivity.MEDIUM.name
        val sensitivity = DetectionSensitivity.entries.firstOrNull { it.name == sensitivityName }
            ?: DetectionSensitivity.MEDIUM

        val appLimits = parseAppLimits(prefs[Keys.APP_LIMITS_JSON].orEmpty())

        return GuardSettings(
            maxMinutes = (prefs[Keys.MAX_MINUTES] ?: 45).coerceIn(1, 240),
            weekendMaxMinutes = (prefs[Keys.WEEKEND_MAX_MINUTES] ?: 75).coerceIn(1, 300),
            bedtimeEnabled = prefs[Keys.BEDTIME_ENABLED] ?: true,
            bedtimeStartHour = (prefs[Keys.BEDTIME_START_HOUR] ?: 21).coerceIn(0, 23),
            bedtimeStartMinute = (prefs[Keys.BEDTIME_START_MINUTE] ?: 0).coerceIn(0, 59),
            bedtimeEndHour = (prefs[Keys.BEDTIME_END_HOUR] ?: 6).coerceIn(0, 23),
            bedtimeEndMinute = (prefs[Keys.BEDTIME_END_MINUTE] ?: 0).coerceIn(0, 59),
            scareTheme = theme,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            voiceEnabled = prefs[Keys.VOICE_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            parentPin = prefs[Keys.PARENT_PIN] ?: "1234",
            isGuardArmed = prefs[Keys.GUARD_ARMED] ?: true,
            demoTargetProfile = profile,
            cameraScanPolicy = scanPolicy,
            appLimits = appLimits,
            isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
            aiDetectionEnabled = prefs[Keys.AI_DETECTION_ENABLED] ?: true,
            detectionSensitivity = sensitivity,
            warnAt10Min = prefs[Keys.WARN_AT_10_MIN] ?: true,
            warnAt5Min = prefs[Keys.WARN_AT_5_MIN] ?: true,
            warnAtLimit = prefs[Keys.WARN_AT_LIMIT] ?: true,
            hasCompletedInitialPinSetup = prefs[Keys.HAS_COMPLETED_INITIAL_PIN] ?: false
        )
    }

    private fun parseAppLimits(raw: String): List<AppLimitRule> {
        if (raw.isBlank()) return AppLimitRule.defaults()
        return try {
            val array = JSONArray(raw)
            if (array.length() == 0) return AppLimitRule.defaults()
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pkgsArray = obj.optJSONArray("packageNames") ?: JSONArray()
                    val pkgs = (0 until pkgsArray.length()).map { pkgsArray.getString(it) }
                    add(
                        AppLimitRule(
                            id = obj.getString("id"),
                            appName = obj.getString("appName"),
                            packageNames = pkgs,
                            dailyLimitMinutes = obj.optInt("dailyLimitMinutes", 30).coerceIn(5, 180),
                            enabled = obj.optBoolean("enabled", true)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            AppLimitRule.defaults()
        }
    }

    private fun encodeAppLimits(rules: List<AppLimitRule>): String {
        val array = JSONArray()
        rules.forEach { rule ->
            val pkgs = JSONArray().apply {
                rule.packageNames.forEach { put(it) }
            }
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("appName", rule.appName)
                put("packageNames", pkgs)
                put("dailyLimitMinutes", rule.dailyLimitMinutes)
                put("enabled", rule.enabled)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseExtraTimeRequest(raw: String): ExtraTimeRequest? {
        if (raw.isBlank()) return null
        return try {
            val obj = JSONObject(raw)
            val status = ExtraTimeStatus.entries.firstOrNull {
                it.name == obj.optString("status")
            } ?: ExtraTimeStatus.PENDING
            ExtraTimeRequest(
                id = obj.optString("id"),
                requestedAtMillis = obj.optLong("requestedAtMillis"),
                minutesRequested = obj.optInt("minutesRequested", 15),
                status = status
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLogsJson(raw: String): List<AlertLogEntry> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val reason = AlertReason.entries.firstOrNull {
                        it.name == obj.optString("reason")
                    } ?: AlertReason.TIME_LIMIT_EXCEEDED
                    val theme = ScareTheme.entries.firstOrNull {
                        it.name == obj.optString("scareTheme")
                    } ?: ScareTheme.GENTLE_EYECARE
                    val eventType = LogEventType.entries.firstOrNull {
                        it.name == obj.optString("eventType")
                    } ?: reason.toLogEventType()
                    add(
                        AlertLogEntry(
                            id = obj.optString("id"),
                            timestampMillis = obj.optLong("timestampMillis"),
                            reason = reason,
                            scareTheme = theme,
                            elapsedMinutes = obj.optInt("elapsedMinutes"),
                            note = obj.optString("note"),
                            eventType = eventType
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeLogsJson(logs: List<AlertLogEntry>): String {
        val array = JSONArray()
        logs.forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("timestampMillis", entry.timestampMillis)
                put("reason", entry.reason.name)
                put("scareTheme", entry.scareTheme.name)
                put("elapsedMinutes", entry.elapsedMinutes)
                put("note", entry.note)
                put("eventType", entry.eventType.name)
            }
            array.put(obj)
        }
        return array.toString()
    }
}
