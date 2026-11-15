package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun observeMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()

    @Query("SELECT * FROM jarvis_skills ORDER BY isBuiltIn DESC, createdAt ASC")
    fun observeSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM jarvis_skills")
    suspend fun getAllSkillsOnce(): List<SkillEntity>

    @Query("SELECT * FROM jarvis_skills WHERE id = :id LIMIT 1")
    suspend fun getSkillById(id: String): SkillEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSkillsIfAbsent(skills: List<SkillEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSkill(skill: SkillEntity)

    @Query("DELETE FROM jarvis_skills WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomSkill(id: String)
}
