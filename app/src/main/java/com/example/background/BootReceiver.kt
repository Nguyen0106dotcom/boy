package com.example.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.GuardSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restarts periodic WorkManager checks and Foreground Service after device boot or app update
 * whenever parental protection is armed.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ScreenTimeWorkScheduler.schedulePeriodicCheck(appContext)
                val repository = GuardSettingsRepository(appContext)
                val settings = repository.getSettingsSnapshot()
                if (settings.isGuardArmed) {
                    runCatching {
                        ScreenTimeMonitorService.start(appContext)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
