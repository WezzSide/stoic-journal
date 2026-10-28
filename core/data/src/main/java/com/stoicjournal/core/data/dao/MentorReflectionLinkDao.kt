package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.MentorReflectionLinkEntity

@Dao
interface MentorReflectionLinkDao {
    @Query("SELECT * FROM mentor_reflection_links WHERE conversationId = :conversationId AND candidateId = :candidateId LIMIT 1")
    suspend fun getLink(conversationId: String, candidateId: String): MentorReflectionLinkEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: MentorReflectionLinkEntity): Long
}
