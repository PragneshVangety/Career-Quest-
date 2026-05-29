package com.example.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiClient
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar



class QuestViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = QuestRepository(db.questDao())

    // --- Core Reactive Streams ---
    val userStats: StateFlow<UserStats?> = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val learningPaths: StateFlow<List<LearningPath>> = repository.learningPaths
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudySessions: StateFlow<List<StudySession>> = repository.allStudySessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements: StateFlow<List<Achievement>> = repository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Interactive UI State ---
    val isDarkMode = mutableStateOf(true) // RPG default is elegant Dark Slate
    val selectedTab = mutableStateOf("tasks") // tasks, scores, revision, consistency

    // --- Focus State Machine (Pomodoro Mode) ---
    val isFocusTimerActive = mutableStateOf(false)
    val focusSecondsRemaining = mutableStateOf(1500) // Default 25 min (25 * 60)
    val focusTopic = mutableStateOf("Core Algorithms")
    val selectedTimerDurationMinutes = mutableStateOf(25) // 25, 45, 60 or customized
    private var timerJob: Job? = null

    // --- AI Chat Guild State ---
    val guildMasterChatHistory = mutableStateOf<List<Pair<String, String>>>(
        listOf(
            "Guild Master" to "Hail, Career Apprentice! I am your Placement Guild Master. Here in the sanctuary, you can ask for technical interview feedback, resume optimization advice, or have me forge a custom learning quest for your portfolio. What coding dragons are we slaying today?"
        )
    )
    val isAILoading = mutableStateOf(false)
    val lastAIResponse = mutableStateOf("")

    val mentorAdvice = mutableStateOf<String>("Click below to fetch dynamic strategic advise for your Level parameters!")
    val mentorResources = mutableStateOf<String>("Click below to query curated links and course curricula tailored to your target!")
    val mentorMotivation = mutableStateOf<String>("Click below for a burst of deep, RPG-style developer motivation!")
    val documentAnalysisText = mutableStateOf<String>("")
    val isAnalyzingDocument = mutableStateOf<Boolean>(false)
    val analyzedPathAndTasks = mutableStateOf<Pair<LearningPath, List<Task>>?>(null)

    // --- Offline Synchronized States ---
    val isSyncing = mutableStateOf(false)
    val lastSyncTime = mutableStateOf("Sycned locally just now")

    init {
        // Run daily audit to make sure streaks are maintained or broken
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkDailyStreakMaintenance()
            createNotificationChannel()
        }
    }

    // --- Focus/Timer Controls ---
    fun startFocusTimer() {
        if (isFocusTimerActive.value) return
        isFocusTimerActive.value = true
        timerJob = viewModelScope.launch(Dispatchers.Main) {
            while (focusSecondsRemaining.value > 0) {
                delay(1000)
                focusSecondsRemaining.value--
            }
            onFocusTimerComplete()
        }
    }

    fun pauseFocusTimer() {
        isFocusTimerActive.value = false
        timerJob?.cancel()
    }

    fun resetFocusTimer() {
        pauseFocusTimer()
        focusSecondsRemaining.value = selectedTimerDurationMinutes.value * 60
    }

    fun setTimerDuration(mins: Int) {
        selectedTimerDurationMinutes.value = mins
        focusSecondsRemaining.value = mins * 60
    }

    private fun onFocusTimerComplete() {
        isFocusTimerActive.value = false
        val durationMins = selectedTimerDurationMinutes.value
        val topic = focusTopic.value

        viewModelScope.launch(Dispatchers.IO) {
            val session = StudySession(
                topicName = topic,
                durationMinutes = durationMins,
                xpEarned = durationMins + 10,
                rating = 4 // Base rating
            )
            repository.logStudySession(session)
            sendLocalNotification(
                "Focus Session Completed!",
                "Incredible grind, hero! You studied '$topic' for $durationMins minutes and earned ${durationMins + 10} XP!"
            )
        }
        
        // Reset
        focusSecondsRemaining.value = selectedTimerDurationMinutes.value * 60
    }

    // --- Database Operations ---
    fun addTask(title: String, description: String, difficulty: String, deadlineDays: Int, pathId: Int? = null, category: String = "Learning") {
        viewModelScope.launch(Dispatchers.IO) {
            val xpReward = when (difficulty.uppercase()) {
                "HEROIC" -> 60
                "MEDIUM" -> 30
                else -> 15
            }
            val deadline = if (deadlineDays > 0) {
                System.currentTimeMillis() + (86400000L * deadlineDays)
            } else {
                null
            }

            val task = Task(
                title = title,
                description = description,
                difficulty = difficulty,
                xpReward = xpReward,
                deadlineTimestamp = deadline,
                pathId = pathId,
                category = category
            )
            repository.addTask(task)
        }
    }

    fun addCustomLearningPath(title: String, description: String, difficulty: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val xp = when (difficulty.uppercase()) {
                "MASTER" -> 250
                "INTERMEDIATE" -> 150
                else -> 80
            }
            val path = LearningPath(
                title = title,
                description = description,
                difficulty = difficulty,
                xpReward = xp,
                field = userStats.value?.chosenField ?: "Software Engineer"
            )
            repository.addLearningPath(path)
        }
    }

    fun completeTask(taskId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleTaskComplete(taskId)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task)
        }
    }

    fun changeChosenField(field: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = repository.getDirectUserStats()
            repository.saveUserStats(stats.copy(chosenField = field))
        }
    }

    // --- Google Calendar Auto Scheduling Contract ---
    fun scheduleTaskInCalendar(context: Context, task: Task) {
        val calendarTime = task.deadlineTimestamp ?: (System.currentTimeMillis() + 86400000) // Default tomorrow
        val field = userStats.value?.chosenField ?: "Career Developer"
        
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendarTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, calendarTime + 3600000) // 1 Hour event
            putExtra(CalendarContract.Events.TITLE, "🏆 [Career Quest]: ${task.title}")
            putExtra(CalendarContract.Events.DESCRIPTION, 
                "Task description: ${task.description}\n\nCareer Path: $field\nXP Reward: ${task.xpReward} XP. Unlock this quest on your dev platform to level up!")
            putExtra(CalendarContract.Events.EVENT_LOCATION, "Sanctuary Coding Forge")
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- AI Careers Assistant Interaction ---
    fun sendMessageToAI(userMessage: String) {
        if (userMessage.isBlank()) return
        
        // Add User Message to local chat log instantly
        val currentHistory = guildMasterChatHistory.value.toMutableList()
        currentHistory.add("You" to userMessage)
        guildMasterChatHistory.value = currentHistory

        isAILoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val stats = repository.getDirectUserStats()
            
            // Format existing history as string index for context
            val historyStr = guildMasterChatHistory.value
                .takeLast(6)
                .joinToString("\n") { "${it.first}: ${it.second}" }

            val response = GeminiClient.chatWithGuildMaster(historyStr, userMessage, stats.chosenField)
            
            // Add AI response inside main thread
            viewModelScope.launch(Dispatchers.Main) {
                isAILoading.value = false
                val updatedWithResponse = guildMasterChatHistory.value.toMutableList()
                updatedWithResponse.add("Guild Master" to response)
                guildMasterChatHistory.value = updatedWithResponse
            }
        }
    }

    // --- Forge AI Quest to Room Database ---
    fun forgeCustomAIQuest() {
        val currentHistory = guildMasterChatHistory.value.toMutableList()
        currentHistory.add("You" to "Forge me a specialized custom Quest Path based on my chosen profile!")
        guildMasterChatHistory.value = currentHistory

        isAILoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val stats = repository.getDirectUserStats()
            val forgedResult = GeminiClient.generateCustomQuest(stats.chosenField)

            viewModelScope.launch(Dispatchers.Main) {
                isAILoading.value = false
                
                if (forgedResult != null) {
                    val (path, tasks) = forgedResult
                    
                    // Insert into Database
                    viewModelScope.launch(Dispatchers.IO) {
                        val pathId = repository.addLearningPath(path).toInt()
                        for (task in tasks) {
                            repository.addTask(task.copy(pathId = pathId))
                        }
                    }

                    val responseText = "⚡ [Quest Forged]: I have forged a customized learning path in your quest scrolls: **${path.title}**!\n\nI populated your to-do log with ${tasks.size} unique skill challenges targeting ${stats.chosenField} preparation under this path. Embark on the path, schedule them on your Google Calendar, and claim your victory!"
                    
                    val updatedList = guildMasterChatHistory.value.toMutableList()
                    updatedList.add("Guild Master" to responseText)
                    guildMasterChatHistory.value = updatedList
                } else {
                    val errorList = guildMasterChatHistory.value.toMutableList()
                    errorList.add("Guild Master" to "The forge ran out of high-grade coal! Please toggle your connection or secure your standard scrolls, then retry.")
                    guildMasterChatHistory.value = errorList
                }
            }
        }
    }

    // --- Offline Synchronize trigger ---
    fun forceDataSync() {
        viewModelScope.launch {
            isSyncing.value = true
            delay(1500) // Simulate fast cryptographic syncing
            isSyncing.value = false
            
            val calendar = Calendar.getInstance()
            val minutes = String.format("%02d", calendar.get(Calendar.MINUTE))
            val seconds = String.format("%02d", calendar.get(Calendar.SECOND))
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            
            lastSyncTime.value = "Synced with Cloud Fortress at $hour:$minutes:$seconds"
        }
    }

    // --- Push / System Notification Utilities ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = getApplication<Application>().applicationContext
            val name = "Career Quest Channels"
            val descriptionText = "Reminders for focus completed and career deadlines"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("career_quest_events", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendLocalNotification(title: String, content: String) {
        val context = getApplication<Application>().applicationContext
        val builder = NotificationCompat.Builder(context, "career_quest_events")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Send a unique notice using timestamp id
        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }

    // --- AI MENTOR BOT CORE COMMANDS ---
    fun fetchStrategicAdvice() {
        isAILoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val stats = repository.getDirectUserStats()
            val advice = com.example.ai.GeminiClient.getStrategicAdvice(stats.chosenField, stats.level)
            viewModelScope.launch(Dispatchers.Main) {
                isAILoading.value = false
                mentorAdvice.value = advice
            }
        }
    }

    fun fetchRecommendedResources(currentPathTitle: String = "General Core Placement") {
        isAILoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val stats = repository.getDirectUserStats()
            val resources = com.example.ai.GeminiClient.getRecommendedResources(stats.chosenField, currentPathTitle, stats.level)
            viewModelScope.launch(Dispatchers.Main) {
                isAILoading.value = false
                mentorResources.value = resources
            }
        }
    }

    fun fetchMotivationalBoost() {
        isAILoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val stats = repository.getDirectUserStats()
            val motivation = com.example.ai.GeminiClient.getMotivationalBoost(stats.chosenField, stats.currentStreak)
            viewModelScope.launch(Dispatchers.Main) {
                isAILoading.value = false
                mentorMotivation.value = motivation
            }
        }
    }

    fun analyzeDocumentContent(content: String) {
        if (content.isBlank()) return
        isAnalyzingDocument.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val stats = repository.getDirectUserStats()
            val result = com.example.ai.GeminiClient.parseDocumentToPlan(content, stats.chosenField)
            viewModelScope.launch(Dispatchers.Main) {
                isAnalyzingDocument.value = false
                analyzedPathAndTasks.value = result
            }
        }
    }

    fun commitAnalyzedSyllabusToQuestLog() {
        val data = analyzedPathAndTasks.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val pathId = repository.addLearningPath(data.first).toInt()
            for (task in data.second) {
                repository.addTask(task.copy(pathId = pathId))
            }
            viewModelScope.launch(Dispatchers.Main) {
                analyzedPathAndTasks.value = null // Reset
                documentAnalysisText.value = "" // Reset
                sendLocalNotification(
                    "Syllabus Quest Created!",
                    "Your Mentor Bot has successfully engraved '${data.first.title}' into your custom Quest Log scrolls!"
                )
            }
        }
    }

    // --- TOPIC REVISION QUIZ SYSTEM ---
    val quizTopic = mutableStateOf("Kotlin Basics")
    val quizQuestions = mutableStateOf<List<com.example.ai.QuizQuestion>>(emptyList())
    val isQuizLoading = mutableStateOf(false)
    val currentQuizIndex = mutableStateOf(0)
    val selectedQuizOptionIndex = mutableStateOf<Int?>(null)
    val hasAnsweredCurrentQuiz = mutableStateOf(false)
    val quizCorrectCount = mutableStateOf(0)
    val quizCompletedStatus = mutableStateOf(false)

    fun startQuizForTopic(topicName: String) {
        if (topicName.isBlank()) return
        quizTopic.value = topicName
        isQuizLoading.value = true
        quizCompletedStatus.value = false
        currentQuizIndex.value = 0
        quizCorrectCount.value = 0
        selectedQuizOptionIndex.value = null
        hasAnsweredCurrentQuiz.value = false

        viewModelScope.launch(Dispatchers.IO) {
            val questions = com.example.ai.GeminiClient.generateQuizForTopic(topicName)
            viewModelScope.launch(Dispatchers.Main) {
                quizQuestions.value = questions
                isQuizLoading.value = false
            }
        }
    }

    fun submitQuizAnswer(optionIdx: Int) {
        if (hasAnsweredCurrentQuiz.value || quizQuestions.value.isEmpty()) return
        selectedQuizOptionIndex.value = optionIdx
        hasAnsweredCurrentQuiz.value = true

        val currentQ = quizQuestions.value.getOrNull(currentQuizIndex.value) ?: return
        val isCorrect = optionIdx == currentQ.correctIndex
        if (isCorrect) {
            quizCorrectCount.value = quizCorrectCount.value + 1
            viewModelScope.launch(Dispatchers.IO) {
                repository.grantXp(15) // +15 XP score per correct answer
            }
        }
    }

    fun nextQuizQuestion() {
        val totalQuestions = quizQuestions.value.size
        if (currentQuizIndex.value + 1 < totalQuestions) {
            currentQuizIndex.value = currentQuizIndex.value + 1
            selectedQuizOptionIndex.value = null
            hasAnsweredCurrentQuiz.value = false
        } else {
            quizCompletedStatus.value = true
            val earnedXp = quizCorrectCount.value * 15
            sendLocalNotification(
                "Revision Quiz Complete!",
                "You finished the revision quiz for '${quizTopic.value}' with accuracy ${quizCorrectCount.value}/$totalQuestions. Gained +$earnedXp XP score!"
            )
            viewModelScope.launch(Dispatchers.IO) {
                repository.triggerDailyStreak()
            }
        }
    }
}
