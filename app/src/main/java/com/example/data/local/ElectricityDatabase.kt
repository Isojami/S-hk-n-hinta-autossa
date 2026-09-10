package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "spot_prices")
data class ElectricityPriceEntity(
    @PrimaryKey val id: String, // e.g. "2026-09-10T14"
    val dateString: String,      // "2026-09-10"
    val hour: Int,               // 0..23
    val priceCentPerKwh: Double, // in snt / kWh
    val timestampMillis: Long
)

@Dao
interface ElectricityPriceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<ElectricityPriceEntity>)

    @Query("SELECT * FROM spot_prices WHERE dateString = :date ORDER BY hour ASC")
    fun getPricesForDate(date: String): Flow<List<ElectricityPriceEntity>>

    @Query("SELECT * FROM spot_prices WHERE dateString = :date ORDER BY hour ASC")
    suspend fun getPricesForDateDirect(date: String): List<ElectricityPriceEntity>

    @Query("SELECT * FROM spot_prices ORDER BY timestampMillis DESC")
    fun getAllPrices(): Flow<List<ElectricityPriceEntity>>

    @Query("SELECT * FROM spot_prices ORDER BY timestampMillis DESC")
    suspend fun getAllPricesDirect(): List<ElectricityPriceEntity>

    @Query("DELETE FROM spot_prices WHERE timestampMillis < :olderThanMillis")
    suspend fun deleteOldPrices(olderThanMillis: Long)
}

@Database(entities = [ElectricityPriceEntity::class], version = 1, exportSchema = false)
abstract class ElectricityDatabase : RoomDatabase() {
    abstract fun electricityPriceDao(): ElectricityPriceDao

    companion object {
        @Volatile
        private var INSTANCE: ElectricityDatabase? = null

        fun getInstance(context: Context): ElectricityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ElectricityDatabase::class.java,
                    "electricity_prices.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
