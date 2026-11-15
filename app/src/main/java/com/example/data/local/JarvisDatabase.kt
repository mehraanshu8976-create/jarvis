package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class, SkillEntity::class],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun jarvisDao(): JarvisDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_hud.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }

        val defaultSkills: List<SkillEntity> = listOf(
            SkillEntity(
                id = "youtube",
                name = "YouTube Media Engine",
                category = "Media",
                description = "Open YouTube, search songs or videos, and trigger instant playback.",
                skillType = "NATIVE_INTENT",
                isBuiltIn = true,
                isConnected = true,
                isEnabled = true,
                accountLabel = "Device Native Handler",
                endpointOrUri = "https://www.youtube.com/results?search_query={query}",
                httpMethod = "GET",
                triggerKeywords = "youtube,song,music,video,gaana,bajao,play"
            ),
            SkillEntity(
                id = "spotify",
                name = "Spotify Audio",
                category = "Media",
                description = "Link your Spotify profile to search artists, tracks, and playlists.",
                skillType = "ACCOUNT_LINK",
                isBuiltIn = true,
                isConnected = false,
                isEnabled = true,
                accountLabel = "",
                endpointOrUri = "spotify:search:{query}",
                httpMethod = "GET",
                triggerKeywords = "spotify,playlist,track,album"
            ),
            SkillEntity(
                id = "google_calendar",
                name = "Google Calendar",
                category = "Productivity",
                description = "Link your calendar account to inspect meetings and schedule reminders.",
                skillType = "ACCOUNT_LINK",
                isBuiltIn = true,
                isConnected = false,
                isEnabled = true,
                accountLabel = "",
                endpointOrUri = "content://com.android.calendar/time/",
                httpMethod = "GET",
                triggerKeywords = "calendar,meeting,schedule,event,reminder"
            ),
            SkillEntity(
                id = "smart_home",
                name = "Smart Home Bridge",
                category = "Smart Home",
                description = "Connect Home Assistant, Matter, or webhook URLs to control lights and appliances.",
                skillType = "WEBHOOK_API",
                isBuiltIn = true,
                isConnected = false,
                isEnabled = true,
                accountLabel = "",
                endpointOrUri = "",
                httpMethod = "POST",
                triggerKeywords = "lights,fan,ac,smart home,lamp,switch,room"
            )
        )
    }
}
