package com.example.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import com.example.domain.model.AppLimitRule
import com.example.domain.model.AppUsageEntry
import com.example.domain.model.DailyUsageBar
import java.util.Calendar

data class SystemPermissionsStatus(
    val hasUsageAccess: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasOverlayPermission: Boolean = false
)

data class TodayUsageSnapshot(
    val hasPermission: Boolean,
    val totalForegroundSeconds: Long,
    val totalForegroundMinutes: Int,
    val currentForegroundPackage: String? = null,
    val currentForegroundAppName: String? = null,
    val trackedAppUsages: List<AppUsageEntry>,
    val topDeviceApps: List<AppUsageEntry>,
    val exceededRule: AppLimitRule? = null
)

data class UsageEventSample(
    val packageName: String,
    val eventType: Int,
    val timestampMillis: Long
)

data class ForegroundUsageCalculation(
    val totalForegroundSeconds: Long,
    val perPackageMillis: Map<String, Long>,
    val activePackageName: String?
)

/**
 * Pure Kotlin calculator over UsageEvents intervals to compute exact foreground duration
 * (down to seconds, including currently open foreground app sessions up to [nowMillis]).
 */
object ForegroundUsageCalculator {
    private const val EVENT_MOVE_TO_FOREGROUND = 1 // UsageEvents.Event.ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
    private const val EVENT_MOVE_TO_BACKGROUND = 2 // UsageEvents.Event.ACTIVITY_PAUSED / MOVE_TO_BACKGROUND
    private const val EVENT_SCREEN_INTERACTIVE = 15 // UsageEvents.Event.SCREEN_INTERACTIVE
    private const val EVENT_SCREEN_NON_INTERACTIVE = 16 // UsageEvents.Event.SCREEN_NON_INTERACTIVE
    private const val EVENT_KEYGUARD_SHOWN = 17 // UsageEvents.Event.KEYGUARD_SHOWN
    private const val EVENT_ACTIVITY_STOPPED = 23 // UsageEvents.Event.ACTIVITY_STOPPED

    private val IGNORED_PACKAGES = setOf(
        "com.android.systemui"
    )

    fun compute(
        events: List<UsageEventSample>,
        windowStartMillis: Long,
        nowMillis: Long,
        ignoredPackages: Set<String> = emptySet()
    ): ForegroundUsageCalculation {
        if (nowMillis <= windowStartMillis || events.isEmpty()) {
            return ForegroundUsageCalculation(
                totalForegroundSeconds = 0L,
                perPackageMillis = emptyMap(),
                activePackageName = null
            )
        }

        val excluded = IGNORED_PACKAGES + ignoredPackages
        val sortedEvents = events.sortedBy { it.timestampMillis }
        val openSessions = linkedMapOf<String, Long>()
        val perPackageMs = mutableMapOf<String, Long>()
        var screenNonInteractive = false

        fun closeSession(pkg: String, endMillis: Long) {
            val start = openSessions.remove(pkg) ?: return
            val clampedEnd = endMillis.coerceIn(windowStartMillis, nowMillis)
            val delta = (clampedEnd - start).coerceAtLeast(0L)
            if (delta > 0L) {
                perPackageMs[pkg] = (perPackageMs[pkg] ?: 0L) + delta
            }
        }

        fun closeAllSessions(endMillis: Long) {
            val openKeys = openSessions.keys.toList()
            openKeys.forEach { pkg -> closeSession(pkg, endMillis) }
        }

        for (event in sortedEvents) {
            val ts = event.timestampMillis.coerceIn(windowStartMillis, nowMillis)
            when (event.eventType) {
                EVENT_SCREEN_NON_INTERACTIVE,
                EVENT_KEYGUARD_SHOWN -> {
                    screenNonInteractive = true
                    closeAllSessions(ts)
                }

                EVENT_SCREEN_INTERACTIVE -> {
                    screenNonInteractive = false
                }

                EVENT_MOVE_TO_FOREGROUND -> {
                    screenNonInteractive = false
                    val pkg = event.packageName
                    if (pkg.isNotBlank() && pkg !in excluded) {
                        // Close other active sessions first so overlapping resumes don't double count
                        val others = openSessions.keys.filter { it != pkg }
                        others.forEach { other -> closeSession(other, ts) }
                        if (!openSessions.containsKey(pkg)) {
                            openSessions[pkg] = ts
                        }
                    }
                }

                EVENT_MOVE_TO_BACKGROUND,
                EVENT_ACTIVITY_STOPPED -> {
                    closeSession(event.packageName, ts)
                }
            }
        }

        var activePkg: String? = null
        if (!screenNonInteractive && openSessions.isNotEmpty()) {
            val lastOpen = openSessions.entries.last()
            activePkg = lastOpen.key
            closeAllSessions(nowMillis)
        }

        val totalMs = perPackageMs.values.sum()
        return ForegroundUsageCalculation(
            totalForegroundSeconds = totalMs / 1000L,
            perPackageMillis = perPackageMs,
            activePackageName = activePkg
        )
    }
}

object UsageStatsHelper {

    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openUsageAccessSettings(context: Context) {
        val packageSpecificIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launchedSpecific = runCatching {
            context.startActivity(packageSpecificIntent)
        }.isSuccess

        if (!launchedSpecific) {
            val genericIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(genericIntent) }
        }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(appDetails) }
        }
    }

    fun openSecurityOrPinningSettings(context: Context) {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlayPermissionSettings(context: Context) {
        val packageIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val opened = runCatching { context.startActivity(packageIntent) }.isSuccess
        if (!opened) {
            val generic = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(generic) }
        }
    }

    fun queryTodayUsageSnapshot(
        context: Context,
        rules: List<AppLimitRule>
    ): TodayUsageSnapshot {
        if (!hasUsageAccessPermission(context)) {
            return TodayUsageSnapshot(
                hasPermission = false,
                totalForegroundSeconds = 0L,
                totalForegroundMinutes = 0,
                currentForegroundPackage = null,
                currentForegroundAppName = null,
                trackedAppUsages = rules.map { rule ->
                    AppUsageEntry(
                        appName = rule.appName,
                        packageName = rule.packageNames.firstOrNull().orEmpty(),
                        usedMinutes = 0,
                        limitMinutes = if (rule.enabled) rule.dailyLimitMinutes else null
                    )
                },
                topDeviceApps = emptyList(),
                exceededRule = null
            )
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return TodayUsageSnapshot(false, 0L, 0, null, null, emptyList(), emptyList(), null)

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis

        // 1. Primary: precise interval tracking via UsageEvents
        val eventSamples = mutableListOf<UsageEventSample>()
        val usageEvents = usm.queryEvents(startOfDay, now)
        if (usageEvents != null) {
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val pkg = event.packageName ?: continue
                eventSamples.add(
                    UsageEventSample(
                        packageName = pkg,
                        eventType = event.eventType,
                        timestampMillis = event.timeStamp
                    )
                )
            }
        }

        val eventCalc = ForegroundUsageCalculator.compute(
            events = eventSamples,
            windowStartMillis = startOfDay,
            nowMillis = now,
            ignoredPackages = setOf(context.packageName)
        )

        // 2. Fallback to aggregated stats if UsageEvents returned nothing yet
        val byPackageMs: Map<String, Long>
        val totalSeconds: Long
        if (eventCalc.perPackageMillis.isNotEmpty() || eventCalc.totalForegroundSeconds > 0L) {
            byPackageMs = eventCalc.perPackageMillis
            totalSeconds = eventCalc.totalForegroundSeconds
        } else {
            val aggregated = usm.queryAndAggregateUsageStats(startOfDay, now).orEmpty()
            val fallbackMap = aggregated
                .filterKeys { it != context.packageName && it != "com.android.systemui" }
                .mapValues { (_, stat) -> stat.totalTimeInForeground.coerceAtLeast(0L) }
                .filterValues { it > 0L }
            byPackageMs = fallbackMap
            totalSeconds = fallbackMap.values.sum() / 1000L
        }

        val totalMinutes = (totalSeconds / 60L).toInt()
        val pm = context.packageManager
        val activePkg = eventCalc.activePackageName
        val activeAppName = activePkg?.let { resolveAppLabel(pm, it) }

        var firstExceeded: AppLimitRule? = null
        val trackedEntries = rules.map { rule ->
            val usedMs = rule.packageNames.sumOf { pkg -> byPackageMs[pkg] ?: 0L }
            val usedMin = (usedMs / 60_000L).toInt()
            if (rule.enabled && usedMin >= rule.dailyLimitMinutes && firstExceeded == null) {
                firstExceeded = rule
            }
            AppUsageEntry(
                appName = rule.appName,
                packageName = rule.packageNames.firstOrNull().orEmpty(),
                usedMinutes = usedMin,
                limitMinutes = if (rule.enabled) rule.dailyLimitMinutes else null
            )
        }

        val topApps = byPackageMs.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (pkg, ms) ->
                AppUsageEntry(
                    appName = resolveAppLabel(pm, pkg),
                    packageName = pkg,
                    usedMinutes = (ms / 60_000L).toInt()
                )
            }

        return TodayUsageSnapshot(
            hasPermission = true,
            totalForegroundSeconds = totalSeconds,
            totalForegroundMinutes = totalMinutes,
            currentForegroundPackage = activePkg,
            currentForegroundAppName = activeAppName,
            trackedAppUsages = trackedEntries,
            topDeviceApps = topApps,
            exceededRule = firstExceeded
        )
    }

    fun queryWeeklyDailyUsage(
        context: Context,
        weekdayLimit: Int,
        weekendLimit: Int
    ): List<DailyUsageBar> {
        val dayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val hasAccess = hasUsageAccessPermission(context)
        val usm = if (hasAccess) {
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        } else null

        val result = mutableListOf<DailyUsageBar>()
        val cal = Calendar.getInstance()
        for (offset in -6..0) {
            val dayCal = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = dayCal.timeInMillis
            val dayEnd = if (offset == 0) {
                System.currentTimeMillis()
            } else {
                dayStart + 24 * 60 * 60_000L - 1L
            }

            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
            val labelIdx = when (dayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                else -> 6
            }
            val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
            val limit = if (isWeekend) weekendLimit else weekdayLimit

            val usedMins = if (usm != null) {
                if (offset == 0) {
                    queryTodayUsageSnapshot(context, emptyList()).totalForegroundMinutes
                } else {
                    val aggregated = usm.queryAndAggregateUsageStats(dayStart, dayEnd).orEmpty()
                    val totalMs = aggregated
                        .filterKeys { it != context.packageName && it != "com.android.systemui" }
                        .values
                        .sumOf { it.totalTimeInForeground.coerceAtLeast(0L) }
                    (totalMs / 60_000L).toInt()
                }
            } else {
                0
            }

            result.add(
                DailyUsageBar(
                    dayLabel = dayNames[labelIdx],
                    usedMinutes = usedMins,
                    limitMinutes = limit
                )
            )
        }
        return result
    }

    private fun resolveAppLabel(pm: PackageManager, packageName: String): String {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }
}
