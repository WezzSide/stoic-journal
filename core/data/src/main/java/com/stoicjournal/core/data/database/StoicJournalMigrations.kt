package com.stoicjournal.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every Room migration the app has ever shipped, in one place so they can be exercised by
 * `MigrationTest` (androidTest) with the exported schema JSON files. None of them drops data.
 */
object StoicJournalMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `ai_reflections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL, `insight` TEXT NOT NULL, `interpretation` TEXT NOT NULL, `reflectiveQuestion` TEXT NOT NULL, `actionableStep` TEXT NOT NULL, `quote` TEXT, `principle` TEXT, `timestamp` INTEGER NOT NULL, `theme` TEXT NOT NULL)"
            )
        }
    }
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `journal_entries` ADD COLUMN `virtues` TEXT NOT NULL DEFAULT ''"
            )
        }
    }
    val MIGRATION_1_3: Migration = object : Migration(1, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `ai_reflections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL, `insight` TEXT NOT NULL, `interpretation` TEXT NOT NULL, `reflectiveQuestion` TEXT NOT NULL, `actionableStep` TEXT NOT NULL, `quote` TEXT, `principle` TEXT, `timestamp` INTEGER NOT NULL, `theme` TEXT NOT NULL)"
            )
            db.execSQL(
                "ALTER TABLE `journal_entries` ADD COLUMN `virtues` TEXT NOT NULL DEFAULT ''"
            )
        }
    }
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `mentor_conversations` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `providerId` TEXT, `modelId` TEXT, `conversationMode` TEXT NOT NULL, `status` TEXT NOT NULL, `lastMessageAt` INTEGER NOT NULL, `linkedJournalEntryId` TEXT, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `mentor_messages` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `sequenceNumber` INTEGER NOT NULL, `deliveryStatus` TEXT NOT NULL, `requestCorrelationId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`conversationId`) REFERENCES `mentor_conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_mentor_messages_conversationId` ON `mentor_messages` (`conversationId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_mentor_messages_createdAt` ON `mentor_messages` (`createdAt`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `mentor_sessions` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `completedAt` INTEGER, `status` TEXT NOT NULL, `reflectionCandidateId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`conversationId`) REFERENCES `mentor_conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_mentor_sessions_conversationId` ON `mentor_sessions` (`conversationId`)")
        }
    }
    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `mentor_reflection_links` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `candidateId` TEXT NOT NULL, `journalEntryId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `savePolicy` TEXT NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_mentor_reflection_links_conversationId_candidateId` ON `mentor_reflection_links` (`conversationId`, `candidateId`)"
            )
        }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `mentor_memories` (`id` TEXT NOT NULL, `category` TEXT NOT NULL, `content` TEXT NOT NULL, `sourceEntryIds` TEXT NOT NULL, `sourceConversationIds` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `lastReviewedAt` INTEGER, `status` TEXT NOT NULL, `userApproved` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6
    )
}
