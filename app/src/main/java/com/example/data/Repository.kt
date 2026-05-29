package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class QuestRepository(private val questDao: QuestDao) {

    val userStats: Flow<UserStats?> = questDao.getUserStatsFlow()
    val learningPaths: Flow<List<LearningPath>> = questDao.getAllLearningPathsFlow()
    val allTasks: Flow<List<Task>> = questDao.getAllTasksFlow()
    val allStudySessions: Flow<List<StudySession>> = questDao.getAllStudySessionsFlow()
    val achievements: Flow<List<Achievement>> = questDao.getAllAchievementsFlow()

    fun getTasksByPath(pathId: Int): Flow<List<Task>> = questDao.getTasksByPathFlow(pathId)

    suspend fun getDirectUserStats(): UserStats {
        return questDao.getUserStats() ?: UserStats()
    }

    suspend fun saveUserStats(stats: UserStats) {
        questDao.insertUserStats(stats)
    }

    suspend fun addLearningPath(path: LearningPath): Long {
        val pathId = questDao.insertLearningPath(path)
        // Unlock "path_master" achievement
        unlockAchievement("path_master")
        return pathId
    }

    suspend fun updatePath(path: LearningPath) {
        questDao.updateLearningPath(path)
    }

    suspend fun deletePath(path: LearningPath) {
        questDao.deleteLearningPath(path)
    }

    suspend fun addTask(task: Task): Long {
        return questDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        questDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        questDao.deleteTask(task)
    }

    suspend fun logStudySession(session: StudySession) {
        questDao.insertStudySession(session)
        
        // Grant XP for studying: 1 XP per minute studied!
        val addedXp = session.durationMinutes + (session.rating * 2)
        grantXp(addedXp)

        // Increment total focus stats
        val stats = getDirectUserStats()
        val newFocusMins = stats.totalFocusMinutes + session.durationMinutes
        saveUserStats(stats.copy(totalFocusMinutes = newFocusMins))

        // Check Night Owl achievement
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = session.timestamp
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        if (hourOfDay >= 20 || hourOfDay < 4) { // Night focus: after 8 PM or before 4 AM
            unlockAchievement("night_owl")
        }

        // Check Zen Master focus achievement
        if (newFocusMins >= 60) {
            unlockAchievement("focus_60")
        }

        triggerDailyStreak()
    }

    // --- Experience / RPG Logic ---
    suspend fun grantXp(amount: Int) {
        var stats = getDirectUserStats()
        var currentXp = stats.xp + amount
        var currentLevel = stats.level

        // Handle leveling up
        while (currentXp >= stats.xpNeededForNextLevel) {
            currentXp -= stats.xpNeededForNextLevel
            currentLevel++
            // Regenerate stats object temporarily to evaluate updated levels
            stats = stats.copy(xp = currentXp, level = currentLevel)
        }

        val updatedStats = stats.copy(xp = currentXp, level = currentLevel)
        saveUserStats(updatedStats)

        // Check if level achievement reached
        if (currentLevel >= 5) {
            unlockAchievement("level_5")
        }
    }

    // --- Streak Game Logic ---
    suspend fun checkDailyStreakMaintenance() {
        val stats = getDirectUserStats()
        val lastActive = stats.lastActiveTimestamp
        if (lastActive == 0L) return

        val now = System.currentTimeMillis()
        val daysDiff = getDaysDiff(lastActive, now)

        if (daysDiff > 1) {
            // Broken streak, reset to 0
            saveUserStats(stats.copy(currentStreak = 0))
        }
    }

    suspend fun triggerDailyStreak() {
        val stats = getDirectUserStats()
        val now = System.currentTimeMillis()
        val lastActive = stats.lastActiveTimestamp

        val daysDiff = getDaysDiff(lastActive, now)

        if (daysDiff == 1) {
            // Consecutive day: advance streak!
            val newStreak = stats.currentStreak + 1
            val longest = if (newStreak > stats.longestStreak) newStreak else stats.longestStreak
            saveUserStats(stats.copy(
                currentStreak = newStreak,
                longestStreak = longest,
                lastActiveTimestamp = now
            ))

            // Achievement triggers
            if (newStreak >= 3) {
                unlockAchievement("streak_3")
            }
        } else if (daysDiff > 1 || lastActive == 0L) {
            // Brand new streak
            saveUserStats(stats.copy(
                currentStreak = 1,
                lastActiveTimestamp = now
            ))
        } else {
            // Already active today, just record active timestamp
            saveUserStats(stats.copy(lastActiveTimestamp = now))
        }
    }

    // --- Complete Task / Claim Code Quest Reward ---
    suspend fun toggleTaskComplete(taskId: Int) {
        val task = questDao.getTaskById(taskId) ?: return
        val isNowCompleted = !task.isCompleted
        
        // Update task completion
        questDao.updateTask(task.copy(isCompleted = isNowCompleted))

        if (isNowCompleted) {
            // Complete first task achievement
            unlockAchievement("first_task")

            // Reward XP based on difficulty
            val baseXP = when (task.difficulty.uppercase()) {
                "HEROIC" -> 60
                "MEDIUM" -> 30
                else -> 15
            }
            grantXp(baseXP)
            triggerDailyStreak()

            // Update associated learning path progress percentage
            task.pathId?.let { pathId ->
                val allPathTasks = questDao.getTasksByPath(pathId)
                if (allPathTasks.isNotEmpty()) {
                    val completed = allPathTasks.count { it.isCompleted }
                    val percent = (completed * 100) / allPathTasks.size
                    
                    val path = questDao.getLearningPathById(pathId)
                    if (path != null) {
                        questDao.updateLearningPath(path.copy(
                            progress = percent,
                            isCompleted = percent == 100
                        ))
                    }
                }
            }
        }
    }

    // --- Achievement Trigger ---
    suspend fun unlockAchievement(id: String) {
        val ach = questDao.getAchievementById(id)
        if (ach != null && !ach.isUnlocked) {
            questDao.updateAchievement(ach.copy(
                isUnlocked = true,
                unlockedTimestamp = System.currentTimeMillis()
            ))
            // Achievement bonus XP is automatically awarded
            grantXp(ach.xpReward)
        }
    }

    // Simple Calendar Helpers
    private fun getDaysDiff(timestamp1: Long, timestamp2: Long): Int {
        if (timestamp1 == 0L) return 999
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        // Normalize calendar to midnight for day comparison
        cal1.set(Calendar.HOUR_OF_DAY, 0)
        cal1.set(Calendar.MINUTE, 0)
        cal1.set(Calendar.SECOND, 0)
        cal1.set(Calendar.MILLISECOND, 0)

        cal2.set(Calendar.HOUR_OF_DAY, 0)
        cal2.set(Calendar.MINUTE, 0)
        cal2.set(Calendar.SECOND, 0)
        cal2.set(Calendar.MILLISECOND, 0)

        val diffMs = cal2.timeInMillis - cal1.timeInMillis
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    }
}
