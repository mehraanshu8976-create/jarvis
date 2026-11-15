package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val role: String, // "user" or "jarvis"
    val text: String,
    val actionBadge: String? = null,
    val actionStatus: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "jarvis_skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // "Media", "Productivity", "Smart Home", "Developer API", "System"
    val description: String,
    val skillType: String, // "NATIVE_INTENT", "ACCOUNT_LINK", "WEBHOOK_API", "DEEP_LINK"
    val isBuiltIn: Boolean = false,
    val isConnected: Boolean = false,
    val isEnabled: Boolean = true,
    val accountLabel: String = "",
    val endpointOrUri: String = "",
    val httpMethod: String = "POST", // "GET" or "POST"
    val authToken: String = "",
    val triggerKeywords: String = "",
    val lastResult: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
