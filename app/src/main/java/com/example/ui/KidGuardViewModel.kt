package com.example.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.background.ScreenTimeMonitorService
import com.example.background.ScreenTimeWorkScheduler
import com.example.data.ai.ChildDetectionRepository
import com.example.data.ai.HybridChildDetectionRepository
import com.example.data.local.KidGuardLocalRepository
import com.example.data.preferences.GuardSettingsRepository
import com.example.domain.model.ActiveAlert
import com.example.domain.model.AlertLogEntry
import com.example.domain.model.AlertReason
import com.example.domain.model.AppLimitRule
import com.example.domain.model.AppMode
import com.example.domain.model.AppUsageEntry
import com.example.domain.model.BackgroundUsageState
import com.example.domain.model.CameraScanPolicy
import com.example.domain.model.ChildDetectionResult
import com.example.domain.model.DailyUsageBar
import com.example.domain.model.DemoTargetProfile
import com.example.domain.model.DetectedSubject
import com.example.domain.model.DetectionSensitivity
import com.example.domain.model.ExtraTimeRequest
import com.example.domain.model.ExtraTimeStatus
import com.example.domain.model.FocusLockMode
import com.example.domain.model.GuardSettings
import com.example.domain.model.LogEventType
import com.example.domain.model.ScareTheme
import com.example.domain.model.ScreenTimeLimitEvaluator
import com.example.domain.model.ThresholdCaptureState
import com.example.domain.model.ThresholdVerificationPhase
import com.example.media.ScareAlertPlayer
import com.example.usage.SystemPermissionsStatus
import com.example.usage.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class KidGuardUiState(
    val appMode: AppMode = AppMode.PARENT,
    val settings: GuardSettings = GuardSettings(),
    val alertLogs: List<AlertLogEntry> = emptyList(),
    val detectionResult: ChildDetectionResult = ChildDetectionResult(
        subject = DetectedSubject.CHILD,
        faceCount = 1,
        confidencePercent = 86,
        summary = "Chế độ mô phỏng sẵn sàng — Ước lượng mềm: Trẻ em (không lưu ảnh).",
        sourceModeLabel = "MÔ PHỎNG",
        isPlaceholderAi = true
    ),
    val elapsedSeconds: Long = 388L,
    val isTimerPaused: Boolean = false,
    val isCameraActive: Boolean = false,
    val isInBedtimeWindow: Boolean = false,
    val activeAlert: ActiveAlert? = null,
    val isBackgroundServiceRunning: Boolean = false,
    val lastBackgroundCheckSummary: String = "WorkManager sẵn sàng • Kiểm tra mỗi 15 phút",
    val thresholdCaptureState: ThresholdCaptureState = ThresholdCaptureState(),
    val permissionsStatus: SystemPermissionsStatus = SystemPermissionsStatus(),
    val todayAppUsages: List<AppUsageEntry> = emptyList(),
    val topDeviceApps: List<AppUsageEntry> = emptyList(),
    val weeklyBars: List<DailyUsageBar> = emptyList(),
    val focusLockMode: FocusLockMode = FocusLockMode.NONE,
    val focusLockUntilMillis: Long = 0L,
    val extraMinutesGrantedToday: Int = 0,
    val pendingExtraTimeRequest: ExtraTimeRequest? = null,
    val isLimitsEditUnlocked: Boolean = false,
    val currentForegroundAppName: String? = null,
    val isLoadingUsage: Boolean = false,
    val usageErrorMessage: String? = null,
    val isAnalyzingAi: Boolean = false,
    val aiErrorMessage: String? = null,
    val approachingWarningBanner: String? = null,
    val pauseMonitorUntilMillis: Long = 0L,
    val pauseRemainingSeconds: Long = 0L,
    val isSoftLocked: Boolean = false,
    val isSampleAnalyticsData: Boolean = true
) {
    val activeDailyLimitMinutes: Int
        get() = (settings.activeLimitForDay() + extraMinutesGrantedToday).coerceAtLeast(1)

    val maxSeconds: Long
        get() = activeDailyLimitMinutes * 60L

    val remainingSeconds: Long
        get() = (maxSeconds - elapsedSeconds).coerceAtLeast(0L)

    val progressFraction: Float
        get() = (elapsedSeconds.toFloat() / maxSeconds.toFloat()).coerceIn(0f, 1f)

    val isFocusLockActive: Boolean
        get() = focusLockMode != FocusLockMode.NONE && System.currentTimeMillis() < focusLockUntilMillis

    val isMonitoringPaused15m: Boolean
        get() = pauseRemainingSeconds > 0L
}

class KidGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = GuardSettingsRepository(application)
    private val localRepository = KidGuardLocalRepository(application)
    private val detectionRepository: ChildDetectionRepository = HybridChildDetectionRepository(application)
    private val scarePlayer = ScareAlertPlayer(application)

    private val _appMode = MutableStateFlow(AppMode.PARENT)
    private val _isLimitsEditUnlocked = MutableStateFlow(false)
    private val _detectionResult = MutableStateFlow(
        ChildDetectionResult(
            subject = DetectedSubject.CHILD,
            faceCount = 1,
            confidencePercent = 86,
            summary = "Chế độ mô phỏng sẵn sàng — Ước lượng mềm: Trẻ em (không lưu ảnh).",
            sourceModeLabel = "MÔ PHỎNG",
            isPlaceholderAi = true
        )
    )
    private val _isTimerPaused = MutableStateFlow(false)
    private val _isCameraActive = MutableStateFlow(false)
    private val _isInBedtimeWindow = MutableStateFlow(false)
    private val _activeAlert = MutableStateFlow<ActiveAlert?>(null)
    private val _thresholdCaptureState = MutableStateFlow(ThresholdCaptureState())
    private val _permissionsStatus = MutableStateFlow(SystemPermissionsStatus())
    private val _todayAppUsages = MutableStateFlow<List<AppUsageEntry>>(emptyList())
    private val _topDeviceApps = MutableStateFlow<List<AppUsageEntry>>(emptyList())
    private val _weeklyBars = MutableStateFlow<List<DailyUsageBar>>(emptyList())
    private val _currentForegroundAppName = MutableStateFlow<String?>(null)
    private val _isLoadingUsage = MutableStateFlow(false)
    private val _usageErrorMessage = MutableStateFlow<String?>(null)
    private val _isAnalyzingAi = MutableStateFlow(false)
    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    private val _approachingWarningBanner = MutableStateFlow<String?>(null)
    private val _pauseRemainingSeconds = MutableStateFlow(0L)
    private val _isSampleAnalyticsData = MutableStateFlow(true)

    private val _thresholdCaptureSignal = MutableStateFlow(0L)
    val thresholdCaptureSignal: StateFlow<Long> = _thresholdCaptureSignal.asStateFlow()

    private val latestSettingsFlow: StateFlow<GuardSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, GuardSettings())

    private val latestUsageFlow: StateFlow<BackgroundUsageState> = settingsRepository.usageStateFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, BackgroundUsageState())

    private var pendingThresholdReason: AlertReason? = null
    private var thresholdFallbackJob: Job? = null
    private var lastThresholdExemptAtMillis = 0L
    private var alertGraceUntilMillis = 0L
    private var lastChildLogAtMillis = 0L
    private var lastApproachingWarnMinutes: Int? = null
    private var lastCapturedBitmapForManualAnalyze: Bitmap? = null

    private data class LocalRuntimeState(
        val appMode: AppMode,
        val detection: ChildDetectionResult,
        val isPaused: Boolean,
        val isCameraActive: Boolean,
        val isBedtime: Boolean,
        val activeAlert: ActiveAlert?,
        val thresholdState: ThresholdCaptureState,
        val isLimitsEditUnlocked: Boolean,
        val isAnalyzingAi: Boolean,
        val aiErrorMessage: String?,
        val approachingWarningBanner: String?,
        val pauseRemainingSeconds: Long
    )

    private data class AnalyticsRuntimeState(
        val permissions: SystemPermissionsStatus,
        val todayAppUsages: List<AppUsageEntry>,
        val topDeviceApps: List<AppUsageEntry>,
        val weeklyBars: List<DailyUsageBar>,
        val currentForegroundAppName: String?,
        val isLoadingUsage: Boolean,
        val usageErrorMessage: String?,
        val isSampleAnalyticsData: Boolean
    )

    private val localRuntimeCore = combine(
        _appMode,
        _detectionResult,
        _isTimerPaused,
        _isCameraActive,
        _isInBedtimeWindow
    ) { mode, detection, paused, camActive, bedtime ->
        LocalRuntimeState(
            appMode = mode,
            detection = detection,
            isPaused = paused,
            isCameraActive = camActive,
            isBedtime = bedtime,
            activeAlert = null,
            thresholdState = ThresholdCaptureState(),
            isLimitsEditUnlocked = false,
            isAnalyzingAi = false,
            aiErrorMessage = null,
            approachingWarningBanner = null,
            pauseRemainingSeconds = 0L
        )
    }

    private val localRuntimeAlerts = combine(
        _activeAlert,
        _thresholdCaptureState,
        _isLimitsEditUnlocked,
        _isAnalyzingAi,
        _aiErrorMessage
    ) { alert, thresholdState, limitsUnlocked, analyzing, aiErr ->
        Triple(alert, thresholdState, Triple(limitsUnlocked, analyzing, aiErr))
    }

    private val localRuntimeFlow = combine(
        localRuntimeCore,
        localRuntimeAlerts,
        _approachingWarningBanner,
        _pauseRemainingSeconds
    ) { base, alerts, warningBanner, pauseSecs ->
        base.copy(
            activeAlert = alerts.first,
            thresholdState = alerts.second,
            isLimitsEditUnlocked = alerts.third.first,
            isAnalyzingAi = alerts.third.second,
            aiErrorMessage = alerts.third.third,
            approachingWarningBanner = warningBanner,
            pauseRemainingSeconds = pauseSecs
        )
    }

    private val analyticsCore = combine(
        _permissionsStatus,
        _todayAppUsages,
        _topDeviceApps,
        _weeklyBars,
        _currentForegroundAppName
    ) { permissions, appUsages, topApps, weekly, activeApp ->
        AnalyticsRuntimeState(
            permissions = permissions,
            todayAppUsages = appUsages,
            topDeviceApps = topApps,
            weeklyBars = weekly,
            currentForegroundAppName = activeApp,
            isLoadingUsage = false,
            usageErrorMessage = null,
            isSampleAnalyticsData = true
        )
    }

    private val analyticsFlow = combine(
        analyticsCore,
        _isLoadingUsage,
        _usageErrorMessage,
        _isSampleAnalyticsData
    ) { base, loading, err, isSample ->
        base.copy(
            isLoadingUsage = loading,
            usageErrorMessage = err,
            isSampleAnalyticsData = isSample
        )
    }

    private val mergedLogsFlow = combine(
        localRepository.alertLogsFlow,
        settingsRepository.alertLogsFlow
    ) { roomLogs, dataStoreLogs ->
        (roomLogs + dataStoreLogs)
            .distinctBy { it.id }
            .sortedByDescending { it.timestampMillis }
    }

    val uiState: StateFlow<KidGuardUiState> = combine(
        settingsRepository.settingsFlow,
        mergedLogsFlow,
        settingsRepository.usageStateFlow,
        localRuntimeFlow,
        analyticsFlow
    ) { settings, logs, usage, runtime, analytics ->
        KidGuardUiState(
            appMode = runtime.appMode,
            settings = settings,
            alertLogs = logs,
            detectionResult = runtime.detection,
            elapsedSeconds = usage.accumulatedUsageSeconds,
            isTimerPaused = runtime.isPaused,
            isCameraActive = runtime.isCameraActive,
            isInBedtimeWindow = runtime.isBedtime,
            activeAlert = runtime.activeAlert,
            isBackgroundServiceRunning = usage.isBackgroundServiceRunning,
            lastBackgroundCheckSummary = usage.lastBackgroundCheckSummary,
            thresholdCaptureState = runtime.thresholdState,
            permissionsStatus = analytics.permissions,
            todayAppUsages = analytics.todayAppUsages,
            topDeviceApps = analytics.topDeviceApps,
            weeklyBars = analytics.weeklyBars,
            focusLockMode = usage.focusLockMode,
            focusLockUntilMillis = usage.focusLockUntilMillis,
            extraMinutesGrantedToday = usage.extraMinutesGrantedToday,
            pendingExtraTimeRequest = usage.pendingExtraTimeRequest,
            isLimitsEditUnlocked = runtime.isLimitsEditUnlocked,
            currentForegroundAppName = analytics.currentForegroundAppName,
            isLoadingUsage = analytics.isLoadingUsage,
            usageErrorMessage = analytics.usageErrorMessage,
            isAnalyzingAi = runtime.isAnalyzingAi,
            aiErrorMessage = runtime.aiErrorMessage,
            approachingWarningBanner = runtime.approachingWarningBanner,
            pauseMonitorUntilMillis = usage.pauseMonitorUntilMillis,
            pauseRemainingSeconds = runtime.pauseRemainingSeconds,
            isSoftLocked = usage.softLockActive,
            isSampleAnalyticsData = analytics.isSampleAnalyticsData
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), KidGuardUiState())

    init {
        initializeLocalDatabase()
        observePendingBackgroundAlerts()
        startGuardianTicker()
        refreshSystemUsageStats()
    }

    private fun initializeLocalDatabase() {
        viewModelScope.launch {
            val alreadySeeded = settingsRepository.isSampleDataSeeded()
            localRepository.seedInitialSampleDataIfNeeded(
                alreadySeeded = alreadySeeded,
                markSeeded = { settingsRepository.markSampleDataSeeded() }
            )
            localRepository.appRulesFlow.collectLatest { rules ->
                if (rules.isNotEmpty() && latestSettingsFlow.value.appLimits != rules) {
                    settingsRepository.updateSettings { it.copy(appLimits = rules) }
                }
            }
        }
    }

    fun refreshSystemUsageStats() {
        viewModelScope.launch {
            _isLoadingUsage.value = true
            _usageErrorMessage.value = null
            try {
                val app = getApplication<Application>()
                val settings = latestSettingsFlow.value
                val status = withContext(Dispatchers.IO) {
                    val hasCamera = ContextCompat.checkSelfPermission(
                        app,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            app,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }

                    SystemPermissionsStatus(
                        hasUsageAccess = UsageStatsHelper.hasUsageAccessPermission(app),
                        isIgnoringBatteryOptimizations = UsageStatsHelper.isIgnoringBatteryOptimizations(app),
                        hasCameraPermission = hasCamera,
                        hasNotificationPermission = hasNotif,
                        hasOverlayPermission = UsageStatsHelper.hasOverlayPermission(app)
                    )
                }
                _permissionsStatus.value = status

                settingsRepository.resetDailyCountersIfNewDay(System.currentTimeMillis())

                val todaySnapshot = withContext(Dispatchers.IO) {
                    UsageStatsHelper.queryTodayUsageSnapshot(app, settings.appLimits)
                }

                if (todaySnapshot.hasPermission) {
                    _isSampleAnalyticsData.value = false
                    _todayAppUsages.value = todaySnapshot.trackedAppUsages
                    _topDeviceApps.value = todaySnapshot.topDeviceApps
                    _currentForegroundAppName.value = todaySnapshot.currentForegroundAppName

                    val systemSeconds = todaySnapshot.totalForegroundSeconds
                    val currentSeconds = latestUsageFlow.value.accumulatedUsageSeconds
                    if (systemSeconds > currentSeconds) {
                        settingsRepository.setAccumulatedUsageSeconds(systemSeconds)
                    }

                    val weekly = withContext(Dispatchers.IO) {
                        UsageStatsHelper.queryWeeklyDailyUsage(
                            context = app,
                            weekdayLimit = settings.maxMinutes,
                            weekendLimit = settings.weekendMaxMinutes
                        )
                    }
                    _weeklyBars.value = weekly
                } else {
                    _isSampleAnalyticsData.value = true
                    val previewApps = KidGuardLocalRepository.sampleTopAppsPreview()
                    _todayAppUsages.value = settings.appLimits.map { rule ->
                        val match = previewApps.firstOrNull {
                            it.packageName in rule.packageNames ||
                                rule.appName.contains(it.appName, ignoreCase = true)
                        }
                        AppUsageEntry(
                            appName = rule.appName,
                            packageName = rule.packageNames.firstOrNull().orEmpty(),
                            usedMinutes = match?.usedMinutes ?: 8,
                            limitMinutes = if (rule.enabled) rule.dailyLimitMinutes else null
                        )
                    }
                    _topDeviceApps.value = previewApps
                    _currentForegroundAppName.value = "YouTube (Chế độ mô phỏng)"

                    val roomWeekly = withContext(Dispatchers.IO) {
                        localRepository.weeklyStatsFlow.first()
                    }
                    _weeklyBars.value = if (roomWeekly.isNotEmpty()) {
                        roomWeekly
                    } else {
                        localRepository.buildDefaultWeeklyStats(
                            settings.maxMinutes,
                            settings.weekendMaxMinutes
                        ).map { it.toDomain() }
                    }
                }
            } catch (e: Exception) {
                _usageErrorMessage.value = "Không thể đọc thống kê sử dụng: ${e.localizedMessage ?: "lỗi hệ thống"}"
            } finally {
                _isLoadingUsage.value = false
            }
        }
    }

    private fun observePendingBackgroundAlerts() {
        viewModelScope.launch {
            settingsRepository.usageStateFlow.collectLatest { usage ->
                val pending = usage.pendingAlertReason
                if (pending != null &&
                    _activeAlert.value == null &&
                    pendingThresholdReason == null &&
                    System.currentTimeMillis() > alertGraceUntilMillis
                ) {
                    val settings = latestSettingsFlow.value
                    initiateThresholdVerificationOrAlert(
                        reason = pending,
                        settings = settings,
                        elapsedSeconds = usage.accumulatedUsageSeconds
                    )
                }
            }
        }
    }

    private fun startGuardianTicker() {
        viewModelScope.launch {
            var usageRefreshTick = 0
            while (true) {
                delay(1000L)
                usageRefreshTick++
                val now = System.currentTimeMillis()
                val settings = latestSettingsFlow.value
                val usage = latestUsageFlow.value
                val inBedtime = settings.isCurrentlyInBedtime()
                _isInBedtimeWindow.value = inBedtime

                val pauseRemaining = ((usage.pauseMonitorUntilMillis - now) / 1000L).coerceAtLeast(0L)
                _pauseRemainingSeconds.value = pauseRemaining
                if (usage.pauseMonitorUntilMillis > 0L && pauseRemaining == 0L) {
                    settingsRepository.setPauseMonitorUntil(0L)
                }

                if (usageRefreshTick % 15 == 0) {
                    refreshSystemUsageStats()
                }

                if (usage.focusLockMode != FocusLockMode.NONE && now >= usage.focusLockUntilMillis) {
                    settingsRepository.setFocusLock(FocusLockMode.NONE, 0L)
                }

                if (!settings.isGuardArmed ||
                    _activeAlert.value != null ||
                    pendingThresholdReason != null ||
                    now < alertGraceUntilMillis
                ) {
                    continue
                }

                val currentSubject = _detectionResult.value.subject
                val isPausedTemporarily = pauseRemaining > 0L
                val countUsage = currentSubject != DetectedSubject.ADULT && !isPausedTemporarily

                if (countUsage && !_isTimerPaused.value && now - lastThresholdExemptAtMillis > 10_000L) {
                    val nextElapsed = if (!usage.isBackgroundServiceRunning) {
                        settingsRepository.incrementUsageSeconds(1L)
                    } else {
                        usage.accumulatedUsageSeconds
                    }

                    val evaluation = ScreenTimeLimitEvaluator.evaluate(
                        accumulatedSeconds = nextElapsed,
                        settings = settings,
                        nowMillis = now,
                        curfewSnoozedUntilMillis = usage.curfewSnoozedUntilMillis,
                        focusLockMode = usage.focusLockMode,
                        focusLockUntilMillis = usage.focusLockUntilMillis,
                        extraMinutesGrantedToday = usage.extraMinutesGrantedToday,
                        pauseMonitorUntilMillis = usage.pauseMonitorUntilMillis,
                        softLockActive = usage.softLockActive
                    )

                    val warnMin = evaluation.approachingWarningMinutes
                    if (warnMin != null && warnMin != lastApproachingWarnMinutes) {
                        lastApproachingWarnMinutes = warnMin
                        val msg = "Còn $warnMin phút trước giới hạn — Hãy chuẩn bị cho bé nghỉ mắt 20 giây."
                        _approachingWarningBanner.value = msg
                        logEventToBoth(
                            AlertLogEntry(
                                id = UUID.randomUUID().toString(),
                                timestampMillis = now,
                                reason = AlertReason.MANUAL_TEST,
                                scareTheme = settings.scareTheme,
                                elapsedMinutes = (nextElapsed / 60L).toInt(),
                                note = msg,
                                eventType = LogEventType.THRESHOLD_EXCEEDED
                            )
                        )
                    }

                    evaluation.triggeredReason?.let { reason ->
                        initiateThresholdVerificationOrAlert(reason, settings, nextElapsed)
                    }
                }
            }
        }
    }

    private fun initiateThresholdVerificationOrAlert(
        reason: AlertReason,
        settings: GuardSettings,
        elapsedSeconds: Long
    ) {
        if (pendingThresholdReason != null || _activeAlert.value != null) return

        val shouldCheckCamera = _isCameraActive.value &&
            settings.aiDetectionEnabled &&
            reason != AlertReason.FOCUS_MODE_LOCK &&
            reason != AlertReason.QUICK_LOCK

        if (shouldCheckCamera) {
            pendingThresholdReason = reason
            _thresholdCaptureState.value = _thresholdCaptureState.value.copy(
                phase = ThresholdVerificationPhase.CAPTURING_FRAME,
                detailMessage = "Đã chạm ngưỡng — Camera đang chụp 1 khung hình kiểm tra..."
            )
            _thresholdCaptureSignal.value = System.currentTimeMillis()

            thresholdFallbackJob?.cancel()
            thresholdFallbackJob = viewModelScope.launch {
                delay(2500L)
                val fallbackReason = pendingThresholdReason
                if (fallbackReason != null) {
                    pendingThresholdReason = null
                    triggerAlert(
                        fallbackReason,
                        latestSettingsFlow.value,
                        latestUsageFlow.value.accumulatedUsageSeconds
                    )
                }
            }
        } else {
            triggerAlert(reason, settings, elapsedSeconds)
        }
    }

    fun onCameraActiveChanged(active: Boolean) {
        _isCameraActive.value = active
        if (!active) {
            val profileSubject = when (latestSettingsFlow.value.demoTargetProfile) {
                DemoTargetProfile.SIMULATE_CHILD -> DetectedSubject.CHILD
                DemoTargetProfile.SIMULATE_ADULT -> DetectedSubject.ADULT
            }
            _detectionResult.value = ChildDetectionResult(
                subject = profileSubject,
                faceCount = if (profileSubject == DetectedSubject.NONE) 0 else 1,
                confidencePercent = 82,
                summary = "Camera tắt — Dùng bộ đếm hệ thống & chế độ mô phỏng để tiết kiệm pin.",
                sourceModeLabel = "MÔ PHỎNG",
                isPlaceholderAi = true
            )
        }
    }

    fun onCameraFrameCaptured(bitmap: Bitmap, isThresholdTriggered: Boolean) {
        viewModelScope.launch {
            try {
                // Keep a tiny ephemeral thumbnail in RAM for manual check button (never written to disk)
                lastCapturedBitmapForManualAnalyze?.let { old ->
                    if (!old.isRecycled) old.recycle()
                }
                lastCapturedBitmapForManualAnalyze = createThumbnail(bitmap)

                val settings = latestSettingsFlow.value
                val result = detectionRepository.analyzeFrame(
                    bitmap = bitmap,
                    demoProfile = settings.demoTargetProfile,
                    assumePresenceWithoutCamera = false,
                    isThresholdTriggered = isThresholdTriggered,
                    aiEnabled = settings.aiDetectionEnabled,
                    sensitivity = settings.detectionSensitivity
                )
                _detectionResult.value = result
                _aiErrorMessage.value = result.errorMessage

                val now = System.currentTimeMillis()
                if (result.subject == DetectedSubject.CHILD && now - lastChildLogAtMillis > 120_000L) {
                    lastChildLogAtMillis = now
                    logEventToBoth(
                        AlertLogEntry(
                            id = UUID.randomUUID().toString(),
                            timestampMillis = now,
                            reason = AlertReason.CHILD_DETECTED,
                            scareTheme = settings.scareTheme,
                            elapsedMinutes = (latestUsageFlow.value.accumulatedUsageSeconds / 60L).toInt(),
                            note = "${result.sourceModeLabel}: Phát hiện trẻ em (${result.confidencePercent}% tin cậy, không lưu ảnh).",
                            eventType = LogEventType.CHILD_DETECTED
                        )
                    )
                }

                if (isThresholdTriggered || pendingThresholdReason != null) {
                    val reason = pendingThresholdReason ?: AlertReason.TIME_LIMIT_EXCEEDED
                    pendingThresholdReason = null
                    thresholdFallbackJob?.cancel()
                    thresholdFallbackJob = null

                    val thumbnail = createThumbnail(bitmap)
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now))

                    if (result.subject == DetectedSubject.ADULT) {
                        lastThresholdExemptAtMillis = now
                        _thresholdCaptureState.value = ThresholdCaptureState(
                            phase = ThresholdVerificationPhase.VERIFIED_ADULT_EXEMPT,
                            capturedThumbnail = thumbnail,
                            capturedAtMillis = now,
                            detailMessage = "Kiểm tra lúc $timeStr • Ước lượng mềm: Người lớn — Tạm hoãn nhắc nhở."
                        )
                        settingsRepository.setPendingAlertReason(null)
                    } else {
                        _thresholdCaptureState.value = ThresholdCaptureState(
                            phase = ThresholdVerificationPhase.VERIFIED_CHILD,
                            capturedThumbnail = thumbnail,
                            capturedAtMillis = now,
                            detailMessage = "Kiểm tra lúc $timeStr • Ước lượng mềm: Trẻ em (${result.faceCount} khuôn mặt)."
                        )
                        triggerAlert(reason, settings, latestUsageFlow.value.accumulatedUsageSeconds)
                    }
                }
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    fun analyzeCurrentCameraNow() {
        viewModelScope.launch {
            _isAnalyzingAi.value = true
            _aiErrorMessage.value = null
            try {
                val settings = latestSettingsFlow.value
                val result = detectionRepository.analyzeFrame(
                    bitmap = lastCapturedBitmapForManualAnalyze,
                    demoProfile = settings.demoTargetProfile,
                    assumePresenceWithoutCamera = true,
                    isThresholdTriggered = false,
                    aiEnabled = settings.aiDetectionEnabled,
                    sensitivity = settings.detectionSensitivity
                )
                _detectionResult.value = result
                _aiErrorMessage.value = result.errorMessage

                if (result.subject == DetectedSubject.CHILD) {
                    logEventToBoth(
                        AlertLogEntry(
                            id = UUID.randomUUID().toString(),
                            timestampMillis = System.currentTimeMillis(),
                            reason = AlertReason.CHILD_DETECTED,
                            scareTheme = settings.scareTheme,
                            elapsedMinutes = (latestUsageFlow.value.accumulatedUsageSeconds / 60L).toInt(),
                            note = "${result.sourceModeLabel}: Phân tích thủ công — ${result.summary}",
                            eventType = LogEventType.CHILD_DETECTED
                        )
                    )
                }
            } catch (e: Exception) {
                _aiErrorMessage.value = "Không thể phân tích khung hình: ${e.localizedMessage ?: "lỗi AI"}"
            } finally {
                _isAnalyzingAi.value = false
            }
        }
    }

    private fun createThumbnail(source: Bitmap): Bitmap? {
        return try {
            val maxDim = 220f
            val scaleFactor = minOf(maxDim / source.width, maxDim / source.height, 1f)
            val width = (source.width * scaleFactor).toInt().coerceAtLeast(1)
            val height = (source.height * scaleFactor).toInt().coerceAtLeast(1)
            source.scale(width, height, true)
        } catch (_: Throwable) {
            null
        }
    }

    fun completeInitialPinSetup(pin: String) {
        if (pin.length == 4 && pin.all { it.isDigit() }) {
            viewModelScope.launch {
                settingsRepository.updateSettings {
                    it.copy(
                        parentPin = pin,
                        hasCompletedInitialPinSetup = true
                    )
                }
            }
        }
    }

    fun enterParentModeWithPin(pin: String): Boolean {
        val matches = pin == latestSettingsFlow.value.parentPin
        if (matches) {
            _appMode.value = AppMode.PARENT
            refreshSystemUsageStats()
        }
        return matches
    }

    fun switchToKidMode() {
        _isLimitsEditUnlocked.value = false
        _appMode.value = AppMode.KID
    }

    fun unlockLimitsEditWithPin(pin: String): Boolean {
        val matches = pin == latestSettingsFlow.value.parentPin
        if (matches) {
            _isLimitsEditUnlocked.value = true
        }
        return matches
    }

    fun lockLimitsEdit() {
        _isLimitsEditUnlocked.value = false
    }

    fun requestExtraTimeFromKid(minutes: Int = 15) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val request = ExtraTimeRequest(
                id = UUID.randomUUID().toString(),
                requestedAtMillis = now,
                minutesRequested = minutes,
                status = ExtraTimeStatus.PENDING
            )
            settingsRepository.saveExtraTimeRequest(request)
            logEventToBoth(
                AlertLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = now,
                    reason = AlertReason.MANUAL_TEST,
                    scareTheme = latestSettingsFlow.value.scareTheme,
                    elapsedMinutes = (latestUsageFlow.value.accumulatedUsageSeconds / 60L).toInt(),
                    note = "Bé gửi yêu cầu xin thêm +$minutes phút (Đang chờ phụ huynh duyệt).",
                    eventType = LogEventType.EXTRA_TIME
                )
            )
        }
    }

    fun approveExtraTimeWithPin(pin: String): Boolean {
        if (pin != latestSettingsFlow.value.parentPin) return false
        resolveExtraTimeRequest(true)
        return true
    }

    fun resolveExtraTimeRequest(approve: Boolean) {
        viewModelScope.launch {
            val currentReq = latestUsageFlow.value.pendingExtraTimeRequest ?: return@launch
            val now = System.currentTimeMillis()
            if (approve) {
                val updatedGranted = latestUsageFlow.value.extraMinutesGrantedToday + currentReq.minutesRequested
                settingsRepository.setExtraMinutesGrantedToday(updatedGranted)
                settingsRepository.saveExtraTimeRequest(
                    currentReq.copy(status = ExtraTimeStatus.APPROVED)
                )
                if (_activeAlert.value != null) {
                    scarePlayer.stopActiveAlarm()
                    _activeAlert.value = null
                    settingsRepository.setPendingAlertReason(null)
                    settingsRepository.setSoftLockActive(false)
                }
                logEventToBoth(
                    AlertLogEntry(
                        id = UUID.randomUUID().toString(),
                        timestampMillis = now,
                        reason = AlertReason.MANUAL_TEST,
                        scareTheme = latestSettingsFlow.value.scareTheme,
                        elapsedMinutes = (latestUsageFlow.value.accumulatedUsageSeconds / 60L).toInt(),
                        note = "Phụ huynh đã duyệt thêm +${currentReq.minutesRequested} phút cho bé.",
                        eventType = LogEventType.EXTRA_TIME
                    )
                )
            } else {
                settingsRepository.saveExtraTimeRequest(
                    currentReq.copy(status = ExtraTimeStatus.DENIED)
                )
                logEventToBoth(
                    AlertLogEntry(
                        id = UUID.randomUUID().toString(),
                        timestampMillis = now,
                        reason = AlertReason.MANUAL_TEST,
                        scareTheme = latestSettingsFlow.value.scareTheme,
                        elapsedMinutes = (latestUsageFlow.value.accumulatedUsageSeconds / 60L).toInt(),
                        note = "Phụ huynh từ chối yêu cầu xin thêm +${currentReq.minutesRequested} phút.",
                        eventType = LogEventType.EXTRA_TIME
                    )
                )
            }
        }
    }

    fun activateFocusLock(mode: FocusLockMode, durationMinutes: Int = mode.defaultMinutes) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val until = now + durationMinutes.coerceAtLeast(1) * 60_000L
            settingsRepository.setFocusLock(mode, until)
            logEventToBoth(
                AlertLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = now,
                    reason = AlertReason.FOCUS_MODE_LOCK,
                    scareTheme = latestSettingsFlow.value.scareTheme,
                    elapsedMinutes = (latestUsageFlow.value.accumulatedUsageSeconds / 60L).toInt(),
                    note = "Bật khóa nhanh ${mode.label} trong $durationMinutes phút.",
                    eventType = LogEventType.SCREEN_LOCKED
                )
            )
        }
    }

    fun clearFocusLockWithPin(pin: String): Boolean {
        if (pin != latestSettingsFlow.value.parentPin) return false
        viewModelScope.launch {
            settingsRepository.setFocusLock(FocusLockMode.NONE, 0L)
            if (_activeAlert.value?.reason == AlertReason.FOCUS_MODE_LOCK) {
                scarePlayer.stopActiveAlarm()
                _activeAlert.value = null
                settingsRepository.setPendingAlertReason(null)
            }
        }
        return true
    }

    fun activateQuickLockNow() {
        viewModelScope.launch {
            settingsRepository.setSoftLockActive(true)
            triggerAlert(
                reason = AlertReason.QUICK_LOCK,
                settings = latestSettingsFlow.value,
                elapsedSeconds = latestUsageFlow.value.accumulatedUsageSeconds
            )
        }
    }

    fun pauseMonitoringFor15Minutes() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val until = now + 15 * 60_000L
            settingsRepository.setPauseMonitorUntil(until)
            settingsRepository.setPendingAlertReason(null)
            settingsRepository.setSoftLockActive(false)
            scarePlayer.stopActiveAlarm()
            _activeAlert.value = null
            _approachingWarningBanner.value = null
            logEventToBoth(
                AlertLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = now,
                    reason = AlertReason.MANUAL_TEST,
                    scareTheme = latestSettingsFlow.value.scareTheme,
                    elapsedMinutes = (latestUsageFlow.value.accumulatedUsageSeconds / 60L).toInt(),
                    note = "Phụ huynh tạm dừng giám sát trong 15 phút.",
                    eventType = LogEventType.SCREEN_LOCKED
                )
            )
        }
    }

    fun resumeMonitoringNow() {
        viewModelScope.launch {
            settingsRepository.setPauseMonitorUntil(0L)
            _pauseRemainingSeconds.value = 0L
        }
    }

    fun acknowledgeAlertUnderstood() {
        val currentReason = _activeAlert.value?.reason
        scarePlayer.stopActiveAlarm()
        _activeAlert.value = null
        pendingThresholdReason = null
        thresholdFallbackJob?.cancel()
        alertGraceUntilMillis = System.currentTimeMillis() + 60_000L
        lastThresholdExemptAtMillis = System.currentTimeMillis()

        viewModelScope.launch {
            settingsRepository.setPendingAlertReason(null)
            settingsRepository.setSoftLockActive(false)
            if (currentReason == AlertReason.BEDTIME_CURFEW) {
                settingsRepository.setCurfewSnoozedUntil(System.currentTimeMillis() + 5 * 60_000L)
            }
        }
    }

    fun toggleTimerPaused() {
        _isTimerPaused.value = !_isTimerPaused.value
    }

    fun resetTimer() {
        viewModelScope.launch {
            pendingThresholdReason = null
            thresholdFallbackJob?.cancel()
            alertGraceUntilMillis = 0L
            lastApproachingWarnMinutes = null
            _approachingWarningBanner.value = null
            settingsRepository.setAccumulatedUsageSeconds(0L)
            settingsRepository.setExtraMinutesGrantedToday(0)
            settingsRepository.setPendingAlertReason(null)
            settingsRepository.setSoftLockActive(false)
            _isTimerPaused.value = false
            _thresholdCaptureState.value = ThresholdCaptureState()
        }
    }

    fun fastForwardNearLimit() {
        viewModelScope.launch {
            val limitSeconds = uiState.value.maxSeconds
            settingsRepository.setAccumulatedUsageSeconds((limitSeconds - 5L).coerceAtLeast(0L))
            _isTimerPaused.value = false
        }
    }

    fun triggerThresholdCaptureCheckNow() {
        val settings = latestSettingsFlow.value
        initiateThresholdVerificationOrAlert(
            reason = AlertReason.MANUAL_TEST,
            settings = settings,
            elapsedSeconds = latestUsageFlow.value.accumulatedUsageSeconds
        )
    }

    fun triggerManualScareTest() {
        triggerAlert(
            reason = AlertReason.MANUAL_TEST,
            settings = latestSettingsFlow.value,
            elapsedSeconds = latestUsageFlow.value.accumulatedUsageSeconds
        )
    }

    fun toggleBackgroundService() {
        val app = getApplication<Application>()
        if (latestUsageFlow.value.isBackgroundServiceRunning) {
            ScreenTimeMonitorService.stop(app)
        } else {
            ScreenTimeMonitorService.start(app)
        }
    }

    fun runWorkManagerCheckNow() {
        ScreenTimeWorkScheduler.triggerImmediateCheck(getApplication())
    }

    private fun triggerAlert(
        reason: AlertReason,
        settings: GuardSettings,
        elapsedSeconds: Long
    ) {
        val elapsedMins = (elapsedSeconds / 60L).toInt()
        val now = System.currentTimeMillis()
        val effectiveLimit = uiState.value.activeDailyLimitMinutes
        val detail = when (reason) {
            AlertReason.BEDTIME_CURFEW -> "Khung giờ ngủ (${settings.formattedBedtimeWindow()}) — Hãy nhắc bé nghỉ ngơi."
            AlertReason.TIME_LIMIT_EXCEEDED -> "Đã dùng $elapsedMins / $effectiveLimit phút hôm nay — Nhắc bé nghỉ mắt, nhìn xa 20 giây."
            AlertReason.APP_LIMIT_EXCEEDED -> "Ứng dụng đã chạm giới hạn hàng ngày — Hãy tạm dừng cho mắt nghỉ."
            AlertReason.FOCUS_MODE_LOCK -> "Đang bật ${latestUsageFlow.value.focusLockMode.label}"
            AlertReason.QUICK_LOCK -> "Phụ huynh vừa bật Khóa ngay màn hình."
            AlertReason.CHILD_DETECTED -> "Nhận diện trẻ em đang sử dụng thiết bị quá giờ."
            AlertReason.MANUAL_TEST -> "Nhắc nghỉ mắt, nhìn xa 20 giây để bảo vệ thị lực."
        }

        val alert = ActiveAlert(
            reason = reason,
            scareTheme = settings.scareTheme,
            triggeredAtMillis = now,
            elapsedMinutes = elapsedMins,
            detailText = detail,
            isSoftReminder = settings.scareTheme.isGentleMode
        )
        _activeAlert.value = alert

        scarePlayer.triggerScare(
            theme = settings.scareTheme,
            playSound = settings.soundEnabled,
            speakVoice = settings.voiceEnabled,
            vibrate = settings.vibrationEnabled
        )

        viewModelScope.launch {
            settingsRepository.setPendingAlertReason(reason)
            logEventToBoth(
                AlertLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = now,
                    reason = reason,
                    scareTheme = settings.scareTheme,
                    elapsedMinutes = elapsedMins,
                    note = detail,
                    eventType = reason.toLogEventType()
                )
            )
        }
    }

    fun dismissAlertWithPin(pin: String, addExtraMinutes: Int? = null): Boolean {
        val settings = latestSettingsFlow.value
        if (pin != settings.parentPin) return false

        val alertReason = _activeAlert.value?.reason
        scarePlayer.stopActiveAlarm()
        _activeAlert.value = null
        pendingThresholdReason = null
        thresholdFallbackJob?.cancel()
        alertGraceUntilMillis = System.currentTimeMillis() + 30_000L

        viewModelScope.launch {
            settingsRepository.setPendingAlertReason(null)
            settingsRepository.setSoftLockActive(false)
            if (alertReason == AlertReason.BEDTIME_CURFEW) {
                settingsRepository.setCurfewSnoozedUntil(System.currentTimeMillis() + 5 * 60_000L)
            }
            if (alertReason == AlertReason.FOCUS_MODE_LOCK) {
                settingsRepository.setFocusLock(FocusLockMode.NONE, 0L)
            }

            if (addExtraMinutes != null) {
                val updatedExtra = latestUsageFlow.value.extraMinutesGrantedToday + addExtraMinutes
                settingsRepository.setExtraMinutesGrantedToday(updatedExtra)
                logEventToBoth(
                    AlertLogEntry(
                        id = UUID.randomUUID().toString(),
                        timestampMillis = System.currentTimeMillis(),
                        reason = AlertReason.MANUAL_TEST,
                        scareTheme = settings.scareTheme,
                        elapsedMinutes = (latestUsageFlow.value.accumulatedUsageSeconds / 60L).toInt(),
                        note = "Phụ huynh duyệt thêm +$addExtraMinutes phút khi mở khóa.",
                        eventType = LogEventType.EXTRA_TIME
                    )
                )
            } else if (alertReason == AlertReason.TIME_LIMIT_EXCEEDED) {
                settingsRepository.setAccumulatedUsageSeconds(0L)
            }
        }
        return true
    }

    fun setGuardArmed(armed: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(isGuardArmed = armed) }
        }
    }

    fun setDemoProfile(profile: DemoTargetProfile) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(demoTargetProfile = profile) }
            val nextSubject = when (profile) {
                DemoTargetProfile.SIMULATE_CHILD -> DetectedSubject.CHILD
                DemoTargetProfile.SIMULATE_ADULT -> DetectedSubject.ADULT
            }
            _detectionResult.value = ChildDetectionResult(
                subject = nextSubject,
                faceCount = 1,
                confidencePercent = 90,
                summary = if (nextSubject == DetectedSubject.CHILD) {
                    "Đã chọn mẫu thử: Trẻ em — Cảnh báo sẽ hiển thị khi vượt ngưỡng."
                } else {
                    "Đã chọn mẫu thử: Người lớn — Tạm hoãn cảnh báo giới hạn trẻ em."
                },
                sourceModeLabel = "MÔ PHỎNG",
                isPlaceholderAi = true
            )
        }
    }

    fun setCameraScanPolicy(policy: CameraScanPolicy) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(cameraScanPolicy = policy) }
        }
    }

    fun setAiDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(aiDetectionEnabled = enabled) }
        }
    }

    fun setDetectionSensitivity(sensitivity: DetectionSensitivity) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(detectionSensitivity = sensitivity) }
        }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(isDarkTheme = dark) }
        }
    }

    fun setWarningThresholds(warnAt10Min: Boolean, warnAt5Min: Boolean, warnAtLimit: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(
                    warnAt10Min = warnAt10Min,
                    warnAt5Min = warnAt5Min,
                    warnAtLimit = warnAtLimit
                )
            }
        }
    }

    fun setMaxMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(maxMinutes = minutes.coerceIn(5, 240)) }
        }
    }

    fun setWeekendMaxMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(weekendMaxMinutes = minutes.coerceIn(5, 300)) }
        }
    }

    fun updateAppLimitRule(ruleId: String, enabled: Boolean, limitMinutes: Int) {
        viewModelScope.launch {
            var updatedTarget: AppLimitRule? = null
            settingsRepository.updateSettings { current ->
                val updatedRules = current.appLimits.map { rule ->
                    if (rule.id == ruleId) {
                        rule.copy(enabled = enabled, dailyLimitMinutes = limitMinutes.coerceIn(5, 180))
                            .also { updatedTarget = it }
                    } else {
                        rule
                    }
                }
                current.copy(appLimits = updatedRules)
            }
            updatedTarget?.let { localRepository.upsertAppRule(it) }
            refreshSystemUsageStats()
        }
    }

    fun addAppRule(appName: String, packageNames: List<String>, limitMinutes: Int) {
        val cleanName = appName.trim()
        if (cleanName.isBlank()) return
        val newRule = AppLimitRule(
            id = "rule_${UUID.randomUUID().toString().take(8)}",
            appName = cleanName,
            packageNames = packageNames.ifEmpty {
                listOf("com.custom.${cleanName.lowercase(Locale.ROOT).replace(" ", "")}")
            },
            dailyLimitMinutes = limitMinutes.coerceIn(5, 180),
            enabled = true
        )
        viewModelScope.launch {
            localRepository.upsertAppRule(newRule, sortOrder = latestSettingsFlow.value.appLimits.size + 1)
            settingsRepository.updateSettings { current ->
                current.copy(appLimits = current.appLimits + newRule)
            }
            refreshSystemUsageStats()
        }
    }

    fun updateAppRuleFull(
        ruleId: String,
        appName: String,
        packageNames: List<String>,
        enabled: Boolean,
        limitMinutes: Int
    ) {
        val cleanName = appName.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            var updatedTarget: AppLimitRule? = null
            settingsRepository.updateSettings { current ->
                val updated = current.appLimits.map { rule ->
                    if (rule.id == ruleId) {
                        rule.copy(
                            appName = cleanName,
                            packageNames = packageNames.ifEmpty { rule.packageNames },
                            enabled = enabled,
                            dailyLimitMinutes = limitMinutes.coerceIn(5, 180)
                        ).also { updatedTarget = it }
                    } else {
                        rule
                    }
                }
                current.copy(appLimits = updated)
            }
            updatedTarget?.let { localRepository.upsertAppRule(it) }
            refreshSystemUsageStats()
        }
    }

    fun deleteAppRule(ruleId: String) {
        viewModelScope.launch {
            localRepository.deleteAppRule(ruleId)
            settingsRepository.updateSettings { current ->
                current.copy(appLimits = current.appLimits.filterNot { it.id == ruleId })
            }
            refreshSystemUsageStats()
        }
    }

    fun setBedtimeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(bedtimeEnabled = enabled) }
        }
    }

    fun setBedtimeStart(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(bedtimeStartHour = hour.coerceIn(0, 23), bedtimeStartMinute = minute.coerceIn(0, 59))
            }
        }
    }

    fun setBedtimeEnd(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(bedtimeEndHour = hour.coerceIn(0, 23), bedtimeEndMinute = minute.coerceIn(0, 59))
            }
        }
    }

    fun setScareTheme(theme: ScareTheme) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(scareTheme = theme) }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(soundEnabled = enabled) }
        }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(voiceEnabled = enabled) }
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(vibrationEnabled = enabled) }
        }
    }

    fun updateParentPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            viewModelScope.launch {
                settingsRepository.updateSettings {
                    it.copy(
                        parentPin = newPin,
                        hasCompletedInitialPinSetup = true
                    )
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            localRepository.clearLogs()
            settingsRepository.clearAlertLogs()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            scarePlayer.stopActiveAlarm()
            _activeAlert.value = null
            _approachingWarningBanner.value = null
            _thresholdCaptureState.value = ThresholdCaptureState()
            localRepository.clearAllRoomData()
            localRepository.restoreDefaultAppRules()
            settingsRepository.clearUsageAndHistoryData()
            refreshSystemUsageStats()
        }
    }

    fun dismissErrorBanner() {
        _usageErrorMessage.value = null
        _aiErrorMessage.value = null
    }

    fun dismissApproachingWarning() {
        _approachingWarningBanner.value = null
    }

    private suspend fun logEventToBoth(entry: AlertLogEntry) {
        localRepository.appendLog(entry)
        settingsRepository.appendAlertLog(entry)
    }

    override fun onCleared() {
        lastCapturedBitmapForManualAnalyze?.let {
            if (!it.isRecycled) it.recycle()
        }
        scarePlayer.release()
        super.onCleared()
    }
}
