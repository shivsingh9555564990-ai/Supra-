package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val activeChats: Flow<List<ChatEntity>> = chatDao.getActiveChats()
    val archivedChats: Flow<List<ChatEntity>> = chatDao.getArchivedChats()

    suspend fun insertChat(chat: ChatEntity): Int {
        return chatDao.insertChat(chat).toInt()
    }

    suspend fun archiveChat(id: Int) = chatDao.archiveChat(id)
    suspend fun deleteChatById(id: Int) = chatDao.deleteChatById(id)
    suspend fun setPinned(id: Int, isPinned: Boolean) = chatDao.setPinned(id, isPinned)
    suspend fun setUnread(id: Int, isUnread: Boolean) = chatDao.setUnread(id, isUnread)
    suspend fun renameChat(id: Int, newTitle: String) = chatDao.renameChat(id, newTitle)

    fun getMessagesForChat(chatId: Int): Flow<List<MessageEntity>> = chatDao.getMessagesForChat(chatId)

    suspend fun insertMessage(message: MessageEntity) = chatDao.insertMessage(message)

    suspend fun clearHistory() {
        chatDao.clearAllChats()
        chatDao.clearAllMessages()
    }
}
