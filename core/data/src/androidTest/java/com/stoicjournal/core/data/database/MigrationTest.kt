package com.stoicjournal.core.data.database

import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * QA-10: exercises every supported upgrade path (3→6, 4→6, 5→6) against the exported schema JSON
 * files with synthetic data, and verifies that data, foreign keys and the idempotency index survive.
 * Runs against a throwaway database file — never the user's `stoic_journal.db`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "qa_migration_test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StoicJournalDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @After
    fun cleanUp() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(testDb)
    }

    private fun journalRow(id: String, body: String) = ContentValues().apply {
        put("id", id); put("type", "FREE_FORM"); put("title", "QA Journal Entry $id"); put("body", body)
        put("virtues", "WISDOM,COURAGE"); put("createdAtMillis", 1_700_000_000_000L); put("updatedAtMillis", 1_700_000_000_000L)
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To6_preservesJournalEntriesAndCreatesMentorTables() {
        helper.createDatabase(testDb, 3).apply {
            insert("journal_entries", SQLiteDatabase.CONFLICT_FAIL, journalRow("j1", "QA Journal Entry 001 body"))
            insert("journal_entries", SQLiteDatabase.CONFLICT_FAIL, journalRow("j2", "QA Journal Entry 002 body"))
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 6, true, *StoicJournalMigrations.ALL)

        db.query("SELECT id, body, virtues FROM journal_entries ORDER BY id").use { c ->
            assertEquals(2, c.count)
            c.moveToFirst()
            assertEquals("j1", c.getString(0)); assertEquals("QA Journal Entry 001 body", c.getString(1)); assertEquals("WISDOM,COURAGE", c.getString(2))
        }
        listOf("mentor_conversations", "mentor_messages", "mentor_sessions", "mentor_reflection_links", "mentor_memories").forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use { c -> c.moveToFirst(); assertEquals("table $table should exist and be empty", 0, c.getInt(0)) }
        }
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To6_preservesConversationsAndMessagesWithCascade() {
        helper.createDatabase(testDb, 4).apply {
            insert("journal_entries", SQLiteDatabase.CONFLICT_FAIL, journalRow("j1", "kept"))
            insert("mentor_conversations", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                put("id", "conv-1"); put("title", "QA Conversation 001"); put("createdAt", 1L); put("updatedAt", 2L)
                put("providerId", "openai"); put("modelId", "default"); put("conversationMode", "DAILY_REFLECTION")
                put("status", "ACTIVE"); put("lastMessageAt", 2L)
            })
            insert("mentor_messages", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                put("id", "msg-1"); put("conversationId", "conv-1"); put("role", "USER"); put("content", "QA message")
                put("createdAt", 1L); put("sequenceNumber", 1); put("deliveryStatus", "DELIVERED")
            })
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 6, true, *StoicJournalMigrations.ALL)

        db.query("SELECT title, modelId FROM mentor_conversations").use { c -> c.moveToFirst(); assertEquals("QA Conversation 001", c.getString(0)); assertEquals("default", c.getString(1)) }
        db.query("SELECT content FROM mentor_messages").use { c -> c.moveToFirst(); assertEquals("QA message", c.getString(0)) }
        // Referential integrity: deleting the conversation cascades to its messages.
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM mentor_conversations WHERE id = 'conv-1'")
        db.query("SELECT COUNT(*) FROM mentor_messages").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM journal_entries").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate5To6_preservesReflectionLinksAndUniqueIndex() {
        helper.createDatabase(testDb, 5).apply {
            insert("journal_entries", SQLiteDatabase.CONFLICT_FAIL, journalRow("j-ref", "QA Reflection 001"))
            insert("mentor_reflection_links", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                put("id", "link-1"); put("conversationId", "conv-1"); put("candidateId", "cand-1")
                put("journalEntryId", "j-ref"); put("createdAt", 1L); put("savePolicy", "REVIEW_REQUIRED")
            })
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 6, true, *StoicJournalMigrations.ALL)

        db.query("SELECT journalEntryId FROM mentor_reflection_links WHERE conversationId='conv-1' AND candidateId='cand-1'").use { c ->
            assertEquals(1, c.count); c.moveToFirst(); assertEquals("j-ref", c.getString(0))
        }
        // Duplicate-prevention index must survive the migration.
        try {
            db.execSQL("INSERT INTO mentor_reflection_links (id, conversationId, candidateId, journalEntryId, createdAt, savePolicy) VALUES ('link-2','conv-1','cand-1','j-other',2,'MANUAL')")
            fail("unique index on (conversationId, candidateId) missing after migration")
        } catch (expected: SQLiteConstraintException) {
            // expected
        }
        db.query("SELECT COUNT(*) FROM mentor_memories").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        db.close()
    }

    @Test
    fun freshInstall_opensAtVersion6AndRoundTripsThroughDaos() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(testDb)
        val db = Room.databaseBuilder(context, StoicJournalDatabase::class.java, testDb)
            .addMigrations(*StoicJournalMigrations.ALL)
            .build()
        try {
            db.mentorMemoryDao().insertOrUpdate(
                com.stoicjournal.core.data.entity.MentorMemoryEntity(
                    id = "qa-mem", category = "USER_PREFERENCE", content = "QA Memory 001", sourceEntryIds = "",
                    sourceConversationIds = "", createdAt = 1L, updatedAt = 1L, lastReviewedAt = null, status = "PROPOSED", userApproved = false
                )
            )
            assertEquals(0, db.mentorMemoryDao().getApprovedMemories().first().size)
            db.mentorMemoryDao().approveMemory("qa-mem", 2L)
            assertEquals(1, db.mentorMemoryDao().getApprovedMemories().first().size)
            db.mentorMemoryDao().deleteMemory("qa-mem", 3L)
            assertEquals(0, db.mentorMemoryDao().getApprovedMemories().first().size)
            assertEquals(0, db.mentorMemoryDao().getAllMemories().first().size)
            assertNotNull(db.mentorMemoryDao().getMemoryById("qa-mem"))
            assertTrue(db.openHelper.readableDatabase.version == 6)
        } finally {
            db.close()
        }
    }
}
