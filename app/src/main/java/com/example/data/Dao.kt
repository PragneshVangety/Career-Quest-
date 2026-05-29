package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {

    // --- User Stats Queries ---
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStats)

    // --- Learning Path Queries ---
    @Query("SELECT * FROM learning_paths ORDER BY id DESC")
    fun getAllLearningPathsFlow(): Flow<List<LearningPath>>

    @Query("SELECT * FROM learning_paths WHERE id = :pathId LIMIT 1")
    suspend fun getLearningPathById(pathId: Int): LearningPath?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningPath(path: LearningPath): Long

    @Update
    suspend fun updateLearningPath(path: LearningPath)

    @Delete
    suspend fun deleteLearningPath(path: LearningPath)

    // --- Task/Todo Queries ---
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, deadlineTimestamp ASC, id DESC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE pathId = :pathId ORDER BY id DESC")
    fun getTasksByPathFlow(pathId: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE pathId = :pathId")
    suspend fun getTasksByPath(pathId: Int): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // --- Study Session Queries ---
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllStudySessionsFlow(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySession): Long

    // --- Achievement Queries ---
    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, id ASC")
    fun getAllAchievementsFlow(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getAchievementById(id: String): Achievement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)

    @Update
    suspend fun updateAchievement(achievement: Achievement)
}
