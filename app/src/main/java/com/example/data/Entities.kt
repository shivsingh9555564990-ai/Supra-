package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val isUnread: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: Int,
    val text: String,
    val isUser: Boolean,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
