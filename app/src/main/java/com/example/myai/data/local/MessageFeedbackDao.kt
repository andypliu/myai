package com.example.myai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: MessageFeedback)

    @Query("SELECT * FROM message_feedback WHERE messageId = :messageId")
    suspend fun getFeedbackForMessage(messageId: String): MessageFeedback?

    @Query("SELECT * FROM message_feedback")
    fun getAllFeedback(): Flow<List<MessageFeedback>>

    @Query("DELETE FROM message_feedback WHERE messageId = :messageId")
    suspend fun deleteFeedback(messageId: String)
}
