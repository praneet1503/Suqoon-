package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "health_data")
data class HealthDataEntity(
    @PrimaryKey val dateTimestamp: Long, // Start of day timestamp
    val steps: Int,
    val totalSleepMinutes: Int,
    val deepSleepMinutes: Int,
    val lightSleepMinutes: Int,
    val remSleepMinutes: Int,
    val avgHeartRate: Int,
    val bloodOxygen: Int,
    val stressScore: Int
)

@Dao
interface HealthDataDao {
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertData(data: HealthDataEntity)

    @Query("SELECT * FROM health_data ORDER BY dateTimestamp DESC LIMIT 365")
    fun getYearlyData(): Flow<List<HealthDataEntity>>

    @Query("SELECT * FROM health_data ORDER BY dateTimestamp DESC LIMIT 1")
    suspend fun getLatestData(): HealthDataEntity?
}

@Database(entities = [HealthDataEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthDataDao(): HealthDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
