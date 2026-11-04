package com.example.data.offers.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedQueryCacheDao {

  @Query("SELECT * FROM feed_query_cache WHERE queryKey = :queryKey LIMIT 1")
  suspend fun getCache(queryKey: String): FeedQueryCacheEntity?

  @Query("SELECT * FROM feed_query_cache WHERE queryKey = :queryKey LIMIT 1")
  fun observeCache(queryKey: String): Flow<FeedQueryCacheEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCache(cache: FeedQueryCacheEntity)

  @Query("DELETE FROM feed_query_cache WHERE queryKey = :queryKey")
  suspend fun clearCache(queryKey: String)

  @Query("DELETE FROM feed_query_cache")
  suspend fun clearAll()
}
