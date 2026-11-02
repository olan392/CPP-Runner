package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BuiltAppDao {
    @Query("SELECT * FROM built_apps ORDER BY lastRunAt DESC")
    fun getAllApps(): Flow<List<BuiltAppEntity>>

    @Query("SELECT * FROM built_apps WHERE id = :id LIMIT 1")
    suspend fun getAppById(id: Long): BuiltAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: BuiltAppEntity): Long

    @Update
    suspend fun updateApp(app: BuiltAppEntity)

    @Delete
    suspend fun deleteApp(app: BuiltAppEntity)

    @Query("UPDATE built_apps SET runCount = runCount + 1, lastRunAt = :timestamp WHERE id = :id")
    suspend fun incrementRunCount(id: Long, timestamp: Long = System.currentTimeMillis())
}
