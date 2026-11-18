package com.example.background

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ScreenTimeWorkScheduler {

    private const val PERIODIC_WORK_NAME = "kidguard_screen_time_periodic_check"
    private const val IMMEDIATE_WORK_NAME = "kidguard_screen_time_immediate_check"

    fun schedulePeriodicCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<ScreenTimeCheckWorker>(
            15,
            TimeUnit.MINUTES
        )
            .addTag(PERIODIC_WORK_NAME)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun triggerImmediateCheck(context: Context) {
        val request = OneTimeWorkRequestBuilder<ScreenTimeCheckWorker>()
            .addTag(IMMEDIATE_WORK_NAME)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
