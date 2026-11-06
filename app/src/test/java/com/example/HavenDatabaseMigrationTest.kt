package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.messaging.db.HavenDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HavenDatabaseMigrationTest {
  @Test fun upgradesOriginalVersionThree() = verifyUpgrade(3, false)
  @Test fun upgradesAiStudioVersionThree() = verifyUpgrade(3, true)
  @Test fun upgradesVersionTwoThroughBothMigrations() = verifyUpgrade(2, false)

  private fun verifyUpgrade(oldVersion: Int, hadLocation: Boolean) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val name = "migration-$oldVersion-$hadLocation.db"
    context.deleteDatabase(name)
    try {
      // Create every real table/index, then reconstruct the previously shipped schema.
      val initial = Room.databaseBuilder(context, HavenDatabase::class.java, name)
        .allowMainThreadQueries().build()
      initial.openHelper.writableDatabase.execSQL(
        "INSERT INTO message_outbox (clientMessageId, conversationId, text, createdAt, retryCount, lastAttemptAt, status) VALUES ('pending-id', 'conversation-id', 'Keep my message', 1, 0, 0, 'PENDING')"
      )
      initial.close()
      SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
        if (!hadLocation) db.execSQL("ALTER TABLE feed_offers DROP COLUMN location")
        db.version = oldVersion
      }
      val upgraded = Room.databaseBuilder(context, HavenDatabase::class.java, name)
        .addMigrations(HavenDatabase.MIGRATION_2_3, HavenDatabase.MIGRATION_3_4)
        .allowMainThreadQueries().build()
      try {
        // Opening invokes Room's complete schema validation, not just our ALTER SQL.
        val db = upgraded.openHelper.writableDatabase
        assertEquals(4, db.version)
        db.query("SELECT text FROM message_outbox WHERE clientMessageId = 'pending-id'").use { cursor ->
          assertEquals(true, cursor.moveToFirst())
          assertEquals("Keep my message", cursor.getString(0))
        }
        db.query("SELECT location FROM feed_offers LIMIT 1").close()
      } finally {
        upgraded.close()
      }
    } finally {
      context.deleteDatabase(name)
    }
  }
}
