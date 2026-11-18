package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KidGuardDao {

    @Query("SELECT * FROM app_limit_rules ORDER BY sortOrder ASC, appName ASC")
    fun getAppRulesFlow(): Flow<List<AppLimitRuleEntity>>

    @Query("SELECT COUNT(*) FROM app_limit_rules")
    suspend fun getAppRulesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppRule(entity: AppLimitRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppRules(entities: List<AppLimitRuleEntity>)

    @Query("DELETE FROM app_limit_rules WHERE id = :id")
    suspend fun deleteAppRuleById(id: String)

    @Query("DELETE FROM app_limit_rules")
    suspend fun clearAppRules()

    @Query("SELECT * FROM alert_logs ORDER BY timestampMillis DESC LIMIT 100")
    fun getAlertLogsFlow(): Flow<List<AlertLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlertLog(entity: AlertLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlertLogs(entities: List<AlertLogEntity>)

    @Query("DELETE FROM alert_logs")
    suspend fun clearAlertLogs()

    @Query("SELECT * FROM daily_usage_stats ORDER BY dayIndex ASC")
    fun getWeeklyStatsFlow(): Flow<List<DailyUsageStatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyStats(entities: List<DailyUsageStatEntity>)

    @Query("DELETE FROM daily_usage_stats")
    suspend fun clearWeeklyStats()
}
