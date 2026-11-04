package com.example.data.messaging.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.offers.db.FeedOfferDao
import com.example.data.offers.db.FeedOfferEntity
import com.example.data.offers.db.FeedQueryCacheDao
import com.example.data.offers.db.FeedQueryCacheEntity

@Database(
  entities = [
    ConversationEntity::class,
    MessageEntity::class,
    OutboxEntity::class,
    SyncStateEntity::class,
    FeedOfferEntity::class,
    FeedQueryCacheEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class HavenDatabase : RoomDatabase() {

  abstract fun conversationDao(): ConversationDao
  abstract fun messageDao(): MessageDao
  abstract fun outboxDao(): OutboxDao
  abstract fun syncStateDao(): SyncStateDao
  abstract fun feedOfferDao(): FeedOfferDao
  abstract fun feedQueryCacheDao(): FeedQueryCacheDao

  companion object {
    @Volatile
    private var instance: HavenDatabase? = null

    fun getInstance(context: Context): HavenDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          HavenDatabase::class.java,
          "haven.db"
        )
          .fallbackToDestructiveMigration()
          .build()
          .also { instance = it }
      }
    }

    /**
     * For unit tests / in-memory testing.
     */
    fun createInMemory(context: Context, executor: java.util.concurrent.Executor? = null): HavenDatabase {
      val builder = Room.inMemoryDatabaseBuilder(
        context.applicationContext,
        HavenDatabase::class.java
      )
        .allowMainThreadQueries()
      if (executor != null) {
        builder.setQueryExecutor(executor)
        builder.setTransactionExecutor(executor)
      }
      return builder.build()
    }
  }
}
