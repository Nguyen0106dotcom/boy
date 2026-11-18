package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.background.GuardianNotifications
import com.example.background.ScreenTimeWorkScheduler
import com.example.camera.ThresholdCameraAnalysisController
import com.example.data.ai.configureAppCheck
import com.example.domain.model.AppMode
import com.example.ui.KidGuardViewModel
import com.example.ui.components.InitialPinSetupDialog
import com.example.ui.components.PinDialog
import com.example.ui.components.ScareAlertOverlay
import com.example.ui.components.UsageAccessPermissionDialog
import com.example.ui.screens.KidModeScreen
import com.example.ui.screens.ParentOverviewScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.KidGuardTheme
import com.example.usage.UsageStatsHelper

object KidGuardRoutes {
    const val MONITOR = "parent_monitor"
    const val RULES = "parent_rules"
    const val LOGS = "parent_logs"
    const val SETTINGS = "parent_settings"
}

private data class ParentNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    private var usageAccessPromptToken by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must configure App Check before ViewModel/Compose initialization
        configureAppCheck(this, intent)

        ScreenTimeWorkScheduler.schedulePeriodicCheck(this)
        if (intent?.getBooleanExtra(GuardianNotifications.EXTRA_REQUEST_USAGE_ACCESS, false) == true) {
            usageAccessPromptToken++
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        setContent {
            KidGuardApp(usageAccessPromptToken = usageAccessPromptToken)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(GuardianNotifications.EXTRA_REQUEST_USAGE_ACCESS, false)) {
            usageAccessPromptToken++
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidGuardApp(
    viewModel: KidGuardViewModel = viewModel(),
    usageAccessPromptToken: Int = 0
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val thresholdCaptureSignal by viewModel.thresholdCaptureSignal.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showParentPinDialog by remember { mutableStateOf(false) }
    var showUsageAccessPromptDialog by remember { mutableStateOf(false) }
    var pendingUsageAccessAfterPin by remember { mutableStateOf(false) }

    LaunchedEffect(usageAccessPromptToken) {
        if (usageAccessPromptToken > 0 && !uiState.permissionsStatus.hasUsageAccess) {
            if (uiState.appMode == AppMode.PARENT) {
                showUsageAccessPromptDialog = true
            } else {
                pendingUsageAccessAfterPin = true
                showParentPinDialog = true
            }
        }
    }

    LaunchedEffect(uiState.approachingWarningBanner) {
        val banner = uiState.approachingWarningBanner
        if (!banner.isNullOrBlank()) {
            snackbarHostState.showSnackbar(banner)
            viewModel.dismissApproachingWarning()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraBindFailed by remember { mutableStateOf(false) }
    var activePreviewView by remember { mutableStateOf<PreviewView?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        viewModel.refreshSystemUsageStats()
    }

    val cameraController = remember(context) {
        ThresholdCameraAnalysisController(context.applicationContext)
    }

    DisposableEffect(cameraController) {
        onDispose {
            cameraController.release()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemUsageStats()
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.settings.cameraScanPolicy) {
        cameraController.updateScanPolicy(uiState.settings.cameraScanPolicy)
    }

    DisposableEffect(lifecycleOwner, hasCameraPermission, activePreviewView) {
        if (hasCameraPermission) {
            cameraController.bindCameraUseCases(
                lifecycleOwner = lifecycleOwner,
                previewView = activePreviewView,
                onFrameCaptured = { bitmap, isThresholdTriggered ->
                    viewModel.onCameraFrameCaptured(bitmap, isThresholdTriggered)
                },
                onBindSuccess = {
                    cameraBindFailed = false
                    viewModel.onCameraActiveChanged(true)
                },
                onBindError = {
                    cameraBindFailed = true
                    viewModel.onCameraActiveChanged(false)
                }
            )
        } else {
            cameraController.unbindAll()
            viewModel.onCameraActiveChanged(false)
        }
        onDispose { }
    }

    LaunchedEffect(thresholdCaptureSignal) {
        if (thresholdCaptureSignal > 0L) {
            cameraController.requestThresholdCapture()
        }
    }

    KidGuardTheme(darkTheme = uiState.settings.isDarkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.appMode == AppMode.KID) {
                KidModeScreen(
                    uiState = uiState,
                    hasCameraPermission = hasCameraPermission,
                    onRequestExtraTime = { viewModel.requestExtraTimeFromKid(15) },
                    onApproveExtraTimeWithPin = viewModel::approveExtraTimeWithPin,
                    onOpenParentMode = { showParentPinDialog = true }
                )
            } else {
                // Back button in Parent Mode switches to Kid Mode cleanly
                BackHandler(enabled = true) {
                    viewModel.switchToKidMode()
                }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val destinations = listOf(
                    ParentNavItem(
                        route = KidGuardRoutes.MONITOR,
                        labelRes = R.string.nav_monitor,
                        selectedIcon = Icons.Filled.Shield,
                        unselectedIcon = Icons.Outlined.Shield,
                        testTag = "nav_monitor"
                    ),
                    ParentNavItem(
                        route = KidGuardRoutes.RULES,
                        labelRes = R.string.nav_rules,
                        selectedIcon = Icons.AutoMirrored.Filled.Rule,
                        unselectedIcon = Icons.AutoMirrored.Outlined.Rule,
                        testTag = "nav_rules"
                    ),
                    ParentNavItem(
                        route = KidGuardRoutes.LOGS,
                        labelRes = R.string.nav_logs,
                        selectedIcon = Icons.Filled.BarChart,
                        unselectedIcon = Icons.Outlined.BarChart,
                        testTag = "nav_logs"
                    ),
                    ParentNavItem(
                        route = KidGuardRoutes.SETTINGS,
                        labelRes = R.string.nav_settings,
                        selectedIcon = Icons.Filled.Settings,
                        unselectedIcon = Icons.Outlined.Settings,
                        testTag = "nav_settings"
                    )
                )

                fun navigateTo(route: String) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 600.dp

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "KidGuard • Phụ huynh",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                actions = {
                                    TextButton(
                                        onClick = { viewModel.switchToKidMode() },
                                        modifier = Modifier.testTag("switch_to_kid_mode_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ChildCare,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Màn hình Bé",
                                            color = MaterialTheme.colorScheme.tertiary,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        },
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp
                                ) {
                                    destinations.forEach { item ->
                                        val selected = currentDestination?.hierarchy?.any {
                                            it.route == item.route
                                        } == true
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTo(item.route) },
                                            modifier = Modifier.testTag(item.testTag),
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                    contentDescription = stringResource(item.labelRes)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(item.labelRes),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail(
                                    modifier = Modifier.fillMaxHeight(),
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    destinations.forEach { item ->
                                        val selected = currentDestination?.hierarchy?.any {
                                            it.route == item.route
                                        } == true
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { navigateTo(item.route) },
                                            modifier = Modifier.testTag(item.testTag),
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                    contentDescription = stringResource(item.labelRes)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(item.labelRes),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = KidGuardRoutes.MONITOR,
                                modifier = Modifier.weight(1f)
                            ) {
                                composable(KidGuardRoutes.MONITOR) {
                                    ParentOverviewScreen(
                                        uiState = uiState,
                                        hasCameraPermission = hasCameraPermission,
                                        cameraBindFailed = cameraBindFailed,
                                        onRequestCameraPermission = {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        },
                                        onPreviewViewChanged = { activePreviewView = it },
                                        onSelectDemoProfile = viewModel::setDemoProfile,
                                        onAnalyzeAiNow = viewModel::analyzeCurrentCameraNow,
                                        onToggleGuardArmed = viewModel::setGuardArmed,
                                        onResolveExtraTimeRequest = viewModel::resolveExtraTimeRequest,
                                        onActivateFocusLock = viewModel::activateFocusLock,
                                        onClearFocusLock = {
                                            viewModel.clearFocusLockWithPin(uiState.settings.parentPin)
                                        },
                                        onQuickLockNow = viewModel::activateQuickLockNow,
                                        onPause15Minutes = viewModel::pauseMonitoringFor15Minutes,
                                        onResumeMonitoring = viewModel::resumeMonitoringNow,
                                        onTogglePause = viewModel::toggleTimerPaused,
                                        onResetTimer = viewModel::resetTimer,
                                        onFastForwardNearLimit = viewModel::fastForwardNearLimit,
                                        onCaptureAndVerifyThresholdNow = viewModel::triggerThresholdCaptureCheckNow,
                                        onTriggerScareTest = viewModel::triggerManualScareTest,
                                        onToggleBackgroundService = viewModel::toggleBackgroundService,
                                        onRunWorkManagerNow = viewModel::runWorkManagerCheckNow
                                    )
                                }
                                composable(KidGuardRoutes.RULES) {
                                    RulesScreen(
                                        uiState = uiState,
                                        onUnlockLimitsWithPin = viewModel::unlockLimitsEditWithPin,
                                        onLockLimitsEdit = viewModel::lockLimitsEdit,
                                        onMaxMinutesChanged = viewModel::setMaxMinutes,
                                        onWeekendMaxMinutesChanged = viewModel::setWeekendMaxMinutes,
                                        onUpdateAppLimit = viewModel::updateAppLimitRule,
                                        onAddAppRule = viewModel::addAppRule,
                                        onUpdateAppRuleFull = viewModel::updateAppRuleFull,
                                        onDeleteAppRule = viewModel::deleteAppRule,
                                        onWarningThresholdsChanged = viewModel::setWarningThresholds,
                                        onBedtimeEnabledChanged = viewModel::setBedtimeEnabled,
                                        onBedtimeStartChanged = viewModel::setBedtimeStart,
                                        onBedtimeEndChanged = viewModel::setBedtimeEnd
                                    )
                                }
                                composable(KidGuardRoutes.LOGS) {
                                    ReportsScreen(
                                        uiState = uiState,
                                        onClearHistory = viewModel::clearHistory,
                                        onRetryLoadUsage = viewModel::refreshSystemUsageStats
                                    )
                                }
                                composable(KidGuardRoutes.SETTINGS) {
                                    SettingsScreen(
                                        uiState = uiState,
                                        onUnlockLimitsWithPin = viewModel::unlockLimitsEditWithPin,
                                        onUpdatePin = viewModel::updateParentPin,
                                        onCameraScanPolicyChanged = viewModel::setCameraScanPolicy,
                                        onDetectionSensitivityChanged = viewModel::setDetectionSensitivity,
                                        onAiDetectionEnabledChanged = viewModel::setAiDetectionEnabled,
                                        onScareThemeSelected = viewModel::setScareTheme,
                                        onSoundEnabledChanged = viewModel::setSoundEnabled,
                                        onVoiceEnabledChanged = viewModel::setVoiceEnabled,
                                        onVibrationEnabledChanged = viewModel::setVibrationEnabled,
                                        onDarkThemeChanged = viewModel::setDarkTheme,
                                        onClearAllData = viewModel::clearAllData
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Full-screen Reminder / Break Overlay
            AnimatedVisibility(
                visible = uiState.activeAlert != null,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.92f)
            ) {
                uiState.activeAlert?.let { activeAlert ->
                    ScareAlertOverlay(
                        alert = activeAlert,
                        capturedThumbnail = uiState.thresholdCaptureState.capturedThumbnail,
                        onAcknowledgeUnderstood = viewModel::acknowledgeAlertUnderstood,
                        onRequestExtraTime = { viewModel.requestExtraTimeFromKid(15) },
                        onDismissWithPin = viewModel::dismissAlertWithPin
                    )
                }
            }
        }

        if (!uiState.settings.hasCompletedInitialPinSetup) {
            InitialPinSetupDialog(
                onPinCreated = viewModel::completeInitialPinSetup
            )
        }

        if (showParentPinDialog) {
            PinDialog(
                title = "Khu vực Phụ huynh",
                subtitle = "Nhập mã PIN 4 số để mở Giám sát, Quy tắc, Nhật ký & Cài đặt",
                onDismiss = {
                    showParentPinDialog = false
                    pendingUsageAccessAfterPin = false
                },
                onPinVerified = { pin ->
                    val ok = viewModel.enterParentModeWithPin(pin)
                    if (ok) {
                        showParentPinDialog = false
                        if (pendingUsageAccessAfterPin) {
                            pendingUsageAccessAfterPin = false
                            if (!uiState.permissionsStatus.hasUsageAccess) {
                                showUsageAccessPromptDialog = true
                            }
                        }
                    }
                    ok
                }
            )
        }

        if (showUsageAccessPromptDialog) {
            UsageAccessPermissionDialog(
                hasPermission = uiState.permissionsStatus.hasUsageAccess,
                onDismiss = { showUsageAccessPromptDialog = false },
                onOpenUsageAccessSettings = {
                    showUsageAccessPromptDialog = false
                    UsageStatsHelper.openUsageAccessSettings(context)
                }
            )
        }
    }
}
