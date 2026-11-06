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
  version = 4,
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
    val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
      override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // The previous production simulator used non-UUID server IDs.
        // Keep unsent drafts; discard synthetic history and acknowledgements.
        db.execSQL("UPDATE conversations SET lastMessageBody = '', lastMessageAt = '', lastMessageTimestamp = 0 WHERE id IN (SELECT conversationId FROM messages WHERE clientMessageId LIKE 'seed-%' OR serverId LIKE 'srv-%')")
        db.execSQL("DELETE FROM messages WHERE clientMessageId LIKE 'seed-%' OR serverId LIKE 'srv-%'")
        db.execSQL("DELETE FROM sync_states WHERE syncKey LIKE 'thread_%'")
      }
    }

    // AI Studio shipped two version-3 schemas: one before and one after location.
    // Inspect the actual schema so both installations upgrade without data loss.
    val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
      override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        val hasLocation = db.query("PRAGMA table_info(feed_offers)").use { cursor ->
          val nameIndex = cursor.getColumnIndexOrThrow("name")
          var found = false
          while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == "location") found = true
          }
          found
        }
        if (!hasLocation) db.execSQL("ALTER TABLE feed_offers ADD COLUMN location TEXT")
      }
    }

    @Volatile
    private var instance: HavenDatabase? = null

    fun getInstance(context: Context): HavenDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          HavenDatabase::class.java,
          "haven.db"
        )
          .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
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
