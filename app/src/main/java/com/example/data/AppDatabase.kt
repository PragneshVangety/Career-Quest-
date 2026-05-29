package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserStats::class,
        LearningPath::class,
        Task::class,
        StudySession::class,
        Achievement::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questDao(): QuestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quest_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch(Dispatchers.IO) {
                // Wait for INSTANCE to be fully assigned
                var database: AppDatabase? = null
                for (i in 1..40) {
                    database = INSTANCE
                    if (database != null) break
                    kotlinx.coroutines.delay(50)
                }
                database?.let { dbInst ->
                    populateDatabase(dbInst.questDao())
                }
            }
        }

        suspend fun populateDatabase(dao: QuestDao) {
            // Seed UserStats
            dao.insertUserStats(UserStats())

            // Seed Default RPG Achievements
            val initialAchievements = listOf(
                Achievement(
                    id = "first_task",
                    title = "First Code Victory",
                    description = "Successfully slay manual or career-guided task to unlock your warrior path.",
                    iconName = "CheckCircle"
                ),
                Achievement(
                    id = "streak_3",
                    title = "Consistent Fire",
                    description = "Maintain a daily quest streak of 3 days to establish standard consistency.",
                    iconName = "LocalFireDepartment"
                ),
                Achievement(
                    id = "focus_60",
                    title = "Zen Dev Master",
                    description = "Aaccumulate 60 minutes or more of heavy pomodoro focus.",
                    iconName = "Timer"
                ),
                Achievement(
                    id = "night_owl",
                    title = "Midnight Coder",
                    description = "Complete a focus study session recorded past 8 PM.",
                    iconName = "Nightlight"
                ),
                Achievement(
                    id = "level_5",
                    title = "Guild Champion",
                    description = "Advance your character level upward to Level 5 or higher.",
                    iconName = "MilitaryTech"
                ),
                Achievement(
                    id = "path_master",
                    title = "Curriculum Builder",
                    description = "Design and initialize a custom Career Learning Path.",
                    iconName = "MenuBook"
                )
            )

            for (ach in initialAchievements) {
                dao.insertAchievement(ach)
            }

            // Seed Some Default Tasks & Learning Path for College placement to start off
            val pathId = dao.insertLearningPath(
                LearningPath(
                    title = "FAANG & Placement DSA Grind",
                    description = "Master Data Structures & Algorithms, Systems, and Live Interview Skills.",
                    difficulty = "Intermediate",
                    xpReward = 200,
                    field = "Software Engineer"
                )
            ).toInt()

            val dsaTasks = listOf(
                Task(
                    title = "Complete LeetCode Top 75 Matrix Quest",
                    description = "Solve 2D grid matrix algorithms using BFS/DFS. Rewarded heavily.",
                    pathId = pathId,
                    xpReward = 30,
                    difficulty = "MEDIUM",
                    deadlineTimestamp = System.currentTimeMillis() + 86400000 * 2 // 2 days from now
                ),
                Task(
                    title = "Design TinyURL System Design Sketch",
                    description = "Master load balancers, relational db scaling, hashing, base62 formats.",
                    pathId = pathId,
                    xpReward = 45,
                    difficulty = "HEROIC",
                    deadlineTimestamp = System.currentTimeMillis() + 86400000 * 4
                ),
                Task(
                    title = "Optimize Resume for ATS Matcher",
                    description = "Add high-impact project descriptions (STAR method) and skills keywords.",
                    pathId = pathId,
                    xpReward = 20,
                    difficulty = "EASY",
                    deadlineTimestamp = System.currentTimeMillis() + 86400000
                )
            )

            for (task in dsaTasks) {
                dao.insertTask(task)
            }
        }
    }
}
