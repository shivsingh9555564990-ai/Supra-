package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getActiveChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity): Long

    @Query("UPDATE chats SET isArchived = 1 WHERE id = :id")
    suspend fun archiveChat(id: Int)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Int, isPinned: Boolean)

    @Query("UPDATE chats SET isUnread = :isUnread WHERE id = :id")
    suspend fun setUnread(id: Int, isUnread: Boolean)

    @Query("UPDATE chats SET title = :newTitle WHERE id = :id")
    suspend fun renameChat(id: Int, newTitle: String)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChatById(id: Int)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM chats")
    suspend fun clearAllChats()
    
    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
