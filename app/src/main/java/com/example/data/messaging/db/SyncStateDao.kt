package com.example.data.messaging.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncStateDao {

  @Query("SELECT * FROM sync_states WHERE syncKey = :syncKey LIMIT 1")
  suspend fun getSyncState(syncKey: String): SyncStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSyncState(state: SyncStateEntity)

  @Query("DELETE FROM sync_states WHERE syncKey = :syncKey")
  suspend fun clearSyncState(syncKey: String)
}
