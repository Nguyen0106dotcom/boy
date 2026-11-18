package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppLimitRuleEntity::class,
        AlertLogEntity::class,
        DailyUsageStatEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KidGuardDatabase : RoomDatabase() {

    abstract fun kidGuardDao(): KidGuardDao

    companion object {
        @Volatile
        private var INSTANCE: KidGuardDatabase? = null

        fun getInstance(context: Context): KidGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KidGuardDatabase::class.java,
                    "kidguard_room.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
