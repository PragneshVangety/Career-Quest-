package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val xp: Int = 0,
    val level: Int = 1,
    val chosenField: String = "Software Engineer",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveTimestamp: Long = 0L,
    val totalFocusMinutes: Int = 0
) {
    // RPG Levels: Base of 100XP, +50XP increase per level
    val xpNeededForNextLevel: Int
        get() = level * 100 + 50

    val levelTitle: String
        get() = when (level) {
            in 1..2 -> "Code Apprentice"
            in 3..4 -> "Stack Novice"
            in 5..6 -> "Syntax Sentinel"
            in 7..8 -> "Bug Slayer"
            in 9..10 -> "Algorithm Knight"
            in 11..14 -> "Database Paladin"
            in 15..19 -> "System Archmage"
            else -> "Deployment Sovereign"
        }
}

@Entity(tableName = "learning_paths")
data class LearningPath(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val difficulty: String = "Medium", // Novice, Intermediate, Master
    val xpReward: Int = 150,
    val isCompleted: Boolean = false,
    val progress: Int = 0, // Percentage 0-100
    val field: String = "Common"
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val pathId: Int? = null, // Linked learning path if any
    val xpReward: Int = 15,
    val isCompleted: Boolean = false,
    val isProjectTask: Boolean = false, // Tasks vs resume-boost projects
    val deadlineTimestamp: Long? = null,
    val calendarEventId: Long? = null, // If synchronized with Android Calendar Provider
    val difficulty: String = "EASY", // EASY, MEDIUM, HEROIC
    val dateCreated: Long = System.currentTimeMillis(),
    val category: String = "Learning"
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicName: String,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val xpEarned: Int = 10,
    val rating: Int = 3 // 1 to 5 scale of how focused they were
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconName: String, // String representation of material icon
    val xpReward: Int = 50,
    val isUnlocked: Boolean = false,
    val unlockedTimestamp: Long? = null
)
