package com.example.ai

import com.example.BuildConfig
import com.example.data.LearningPath
import com.example.data.Task
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Moshi models for direct Gemini communication
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Float = 0.7f,
    val responseMimeType: String? = null
)

interface GeminiApiService {
    @Headers("Content-Type: application/json")
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): ResponseBody // Use ResponseBody directly to grab raw output and parse manual
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val service: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun chatWithGuildMaster(history: String, userMessage: String, chosenField: String): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Greetings Knight! I am your career Guild Master. (Gemini API Key is currently placeholder. Please add your key to the Secrets Panel in AI Studio for live responses!) Here is standard guidance for $chosenField: Focus heavily on Core DS&A, solve dynamic programming problems daily, mock interview with peers, and refine your resume using numerical metrics (such as 'Improved latency by 45%'). Keep grinding daily tasks to level up!"
        }

        val prompt = """
            You are "Career Guild Master", an expert career coach and immersive medieval RPG companion. 
            The user is a candidate preparing for high-stakes college placement (clg) and technical job placement in the field of: $chosenField.
            Acknowledge their goals in an RPG fantasy dialogue (use terms like coding scroll, algorithm scrolls, quest log, bug goblins, level up). 
            Provide high-yield, specific technical placement advice, core questions they should master, or resume advice. Keep it motivating, atmospheric, and highly helpful!
            
            Conversation History:
            $history
            User Message: $userMessage
            
            Guild Master's RPG advice:
        """.trimIndent()

        val requestBody = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        return try {
            val responseBody = service.generateContent(apiKey, requestBody)
            val jsonString = responseBody.string()
            parseTextFromGeminiResponse(jsonString)
        } catch (e: Exception) {
            "An online interruption occured, hero! But do not yield. Continue your DSA matrix drills. Error details: ${e.message}"
        }
    }

    suspend fun generateCustomQuest(chosenField: String): Pair<LearningPath, List<Task>>? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Return a beautiful dynamic fallback quest offline
            val randomNum = (1000..9999).random()
            val fallbackPath = LearningPath(
                title = "Backend Dungeon: API Overlord ($randomNum)",
                description = "Master building secure databases and high performance caching algorithms optimized for $chosenField placements.",
                difficulty = "Intermediate",
                xpReward = 150,
                field = chosenField
            )
            val fallbackTasks = listOf(
                Task(
                    title = "Construct DB Connection Pools",
                    description = "Deploy connection pooling and indexes, preventing query bottle-necks.",
                    xpReward = 20,
                    difficulty = "MEDIUM",
                    deadlineTimestamp = System.currentTimeMillis() + 86400000
                ),
                Task(
                    title = "Integrate Redis Guard Caching",
                    description = "Cache high-traffic endpoints to save database lookup cycles.",
                    xpReward = 30,
                    difficulty = "MEDIUM",
                    deadlineTimestamp = System.currentTimeMillis() + 172800000
                ),
                Task(
                    title = "Test APIs under Apache Bench",
                    description = "Load-test server instances at 1000 concurrent RPS.",
                    xpReward = 45,
                    difficulty = "HEROIC",
                    deadlineTimestamp = System.currentTimeMillis() + 259200000
                )
            )
            return Pair(fallbackPath, fallbackTasks)
        }

        val prompt = """
            The user wants to generate a custom Placement learning path with tasks for: $chosenField.
            Return a JSON object conforming EXACTLY to this schema. Do NOT include markdown styling or outer tags. Return raw, clean JSON text:
            {
               "path_title": "[An exciting RPG styled learning path name, e.g. Domain Name Dungeon or SQL Sentry Castle]",
               "path_desc": "[Actionable description focused on technical skill mapping]",
               "difficulty": "Intermediate",
               "xp_reward": 180,
               "tasks": [
                  {
                     "title": "[RPG styled subtask 1, e.g. Solve 5 Recursive Trees]",
                     "desc": "[Detailed educational target description]",
                     "xp": 25,
                     "difficulty": "EASY",
                     "days_deadline": 1
                  },
                  {
                     "title": "[RPG styled subtask 2]",
                     "desc": "[Detailed educational target description]",
                     "xp": 35,
                     "difficulty": "MEDIUM",
                     "days_deadline": 3
                  },
                  {
                     "title": "[RPG styled subtask 3]",
                     "desc": "[Detailed educational target description]",
                     "xp": 50,
                     "difficulty": "HEROIC",
                     "days_deadline": 5
                  }
               ]
            }
        """.trimIndent()

        val requestBody = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        return try {
            val responseBody = service.generateContent(apiKey, requestBody)
            val jsonString = responseBody.string()
            val cleanJson = cleanJsonResponse(jsonString)

            val jsonObject = JSONObject(cleanJson)
            val pathTitle = jsonObject.getString("path_title")
            val pathDesc = jsonObject.getString("path_desc")
            val difficulty = jsonObject.optString("difficulty", "Intermediate")
            val xpReward = jsonObject.optInt("xp_reward", 150)

            val newPath = LearningPath(
                title = pathTitle,
                description = pathDesc,
                difficulty = difficulty,
                xpReward = xpReward,
                field = chosenField
            )

            val tasksArray = jsonObject.getJSONArray("tasks")
            val newTasks = mutableListOf<Task>()
            for (i in 0 until tasksArray.length()) {
                val tObj = tasksArray.getJSONObject(i)
                val days = tObj.optInt("days_deadline", 2)
                newTasks.add(
                    Task(
                        title = tObj.getString("title"),
                        description = tObj.getString("desc"),
                        xpReward = tObj.optInt("xp", 20),
                        difficulty = tObj.optString("difficulty", "MEDIUM"),
                        deadlineTimestamp = System.currentTimeMillis() + (86400000 * days)
                    )
                )
            }
            Pair(newPath, newTasks)
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful network fallback
            val randomNum = (1000..9999).random()
            Pair(
                LearningPath(
                    title = "Database Castle: Schema Sledge ($randomNum)",
                    description = "Repair damaged schemas and design transaction logs.",
                    difficulty = "Master",
                    xpReward = 160,
                    field = chosenField
                ),
                listOf(
                    Task(
                        title = "Verify ACID Compliance Matrix",
                        description = "Audit tables to maintain transaction rollbacks.",
                        xpReward = 20,
                        difficulty = "HEROIC"
                    )
                )
            )
        }
    }

    suspend fun getStrategicAdvice(chosenField: String, level: Int): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "🛡️ **Career Strategic Blueprint (Level $level - Offline Mode)**:\n\n1. **Focus on Quality over Quantity**: Solve 3 problems daily on Recursion and Graphs, making sure you explain the approach out loud.\n2. **Architectural Blueprints**: Read online about system design basics (Caching, Load Balancer, CDNs).\n3. **Guild Mock Trials**: Practice mock interviews twice a week with peers.\n\n*Add a valid Gemini API Key to the AI Studio Secrets panel to unlock customized live advice from your Mentor Bot!*"
        }

        val prompt = """
            You are "Career Mentor Bot", an elite technical placement advisor. 
            The user is at Level $level preparing for placements in $chosenField.
            Provide 3 highly practical, specific, and advanced strategic tips for learning and landing a placement. 
            Keep the tone professional, encouraging, with subtle RPG elements (e.g., Level up, mastery, tactical drills). Keep it clear and format with clean markdown.
        """.trimIndent()

        val requestBody = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        return try {
            val responseBody = service.generateContent(apiKey, requestBody)
            parseTextFromGeminiResponse(responseBody.string())
        } catch (e: Exception) {
            "Strategic advisor is currently unavailable, but your focus remains absolute! Practice graph algorithm drills today."
        }
    }

    suspend fun getRecommendedResources(chosenField: String, currentPath: String, level: Int): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "📚 **Curated Career Resources (Offline Mode)**:\n\n1. **Platform Mastery**: Learn step-by-step with *Roadmap.sh* patterns for $chosenField.\n2. **System Design Guide**: Study system-design-primer on GitHub (highly recommended resource).\n3. **Practical Tutorials**: Follow tech YouTube channels (e.g., freeCodeCamp, NeetCode) for coding walkthroughs.\n\n*Secure your standard shield of knowledge! Paste your Gemini API Key in the Secrets panel to retrieve dynamically curated learning resource pathways.*"
        }

        val prompt = """
            You are "Career Mentor Bot", an elite technical placement advisor.
            Recommend 3 specific, top-tier learning online resources (such as landmark online tutorials, developer documentation, articles, or courses) for a student pursuing: $chosenField.
            Current context/path: $currentPath.
            Provide direct names of the courses/articles, what they teach, and why they are vital for Level $level preparation.
            Format with beautiful markdown list items.
        """.trimIndent()

        val requestBody = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        return try {
            val responseBody = service.generateContent(apiKey, requestBody)
            parseTextFromGeminiResponse(responseBody.string())
        } catch (e: Exception) {
            "Resource curators are deep in the dungeons. Try looking up Roadmap.sh reference cards for $chosenField!"
        }
    }

    suspend fun getMotivationalBoost(chosenField: String, currentStreak: Int): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "🔥 **Spark of Motivation (Streak: $currentStreak Days!)**\n\n\"The warrior who practices a thousand algorithm drills does not fear the bug-goblin of technical rounds.\"\n\nYou are on an incredible $currentStreak-day streak preparing for $chosenField roles. High-stakes placements are conquered day-by-day. Do not let today go by without clearing at least one side-quest! Level up your career!"
        }

        val prompt = """
            You are "Career Mentor Bot". Provide an immersive, powerful, and highly motivating coaching charge to inspire a student working to land a position in $chosenField.
            They currently have an active streak of $currentStreak Days.
            Integrate epic RPG terms (victory, quest, consistency, career peak, placement citadel) with high-growth developer mindset tips. Make it emotional, short, punchy, and incredibly inspiring.
        """.trimIndent()

        val requestBody = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        return try {
            val responseBody = service.generateContent(apiKey, requestBody)
            parseTextFromGeminiResponse(responseBody.string())
        } catch (e: Exception) {
            "Consistency is the ultimate weapon, hero! Your $currentStreak-day streak is proof of your power."
        }
    }

    suspend fun parseDocumentToPlan(docContent: String, chosenField: String): Pair<LearningPath, List<Task>>? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val titleClean = if (docContent.length > 30) docContent.take(30) + "..." else docContent
            val fallbackPath = LearningPath(
                title = "Study Guide: Parse Force ($titleClean)",
                description = "Custom-forged study blueprint mapped from your text resources. Structured in 3 steps under $chosenField.",
                difficulty = "Advanced",
                xpReward = 180,
                field = chosenField
            )
            val fallbackTasks = listOf(
                Task(
                    title = "Phase 1: Core Content Comprehension",
                    description = "Extract main formulas, architectures, or designs from this resource.",
                    xpReward = 30,
                    difficulty = "MEDIUM"
                ),
                Task(
                    title = "Phase 2: Self-Reflection Drills",
                    description = "Synthesize and write core summaries for key parts of $titleClean.",
                    xpReward = 40,
                    difficulty = "MEDIUM"
                ),
                Task(
                    title = "Phase 3: Perfect Practice Challenge",
                    description = "Take mock questions on the syllabus content and explain solution flows.",
                    xpReward = 60,
                    difficulty = "HEROIC"
                )
            )
            return Pair(fallbackPath, fallbackTasks)
        }

        val prompt = """
            The user uploaded or pasted a text resource representing a syllabus, reference guide, article, or tutorial outline:
            ---
            $docContent
            ---
            Perform a meticulous analysis. Break this content down into a step-by-step master plan of exactly 3 milestones to help them master it completely.
            Return a JSON object conforming EXACTLY to the following schema. Return only raw, clean JSON text:
            {
               "path_title": "[A compelling study path title, e.g. Mastering Redux Scrolls or Git Sentry Blueprint]",
               "path_desc": "[Actionable summary detailing the specific syllabus topics parsed and the educational roadmap]",
               "difficulty": "Intermediate",
               "xp_reward": 180,
               "tasks": [
                  {
                     "title": "[Phase 1 action plan, e.g. Parse Redux Slices]",
                     "desc": "[Detailed steps to study the specific concepts represented in the document]",
                     "xp": 30,
                     "difficulty": "EASY",
                     "days_deadline": 1
                  },
                  {
                     "title": "[Phase 2 action plan]",
                     "desc": "[Practical coding drill or design pattern to apply based on the document's content]",
                     "xp": 50,
                     "difficulty": "MEDIUM",
                     "days_deadline": 3
                  },
                  {
                     "title": "[Phase 3 action plan]",
                     "desc": "[A comprehensive testing challenge to achieve mastery and perfect accuracy]",
                     "xp": 80,
                     "difficulty": "HEROIC",
                     "days_deadline": 5
                  }
               ]
            }
        """.trimIndent()

        val requestBody = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        return try {
            val responseBody = service.generateContent(apiKey, requestBody)
            val jsonString = responseBody.string()
            val cleanJson = cleanJsonResponse(jsonString)

            val jsonObject = JSONObject(cleanJson)
            val pathTitle = jsonObject.getString("path_title")
            val pathDesc = jsonObject.getString("path_desc")
            val difficulty = jsonObject.optString("difficulty", "Intermediate")
            val xpReward = jsonObject.optInt("xp_reward", 180)

            val newPath = LearningPath(
                title = pathTitle,
                description = pathDesc,
                difficulty = difficulty,
                xpReward = xpReward,
                field = chosenField
            )

            val tasksArray = jsonObject.getJSONArray("tasks")
            val newTasks = mutableListOf<Task>()
            for (i in 0 until tasksArray.length()) {
                val tObj = tasksArray.getJSONObject(i)
                val days = tObj.optInt("days_deadline", 2)
                newTasks.add(
                    Task(
                        title = tObj.getString("title"),
                        description = tObj.getString("desc"),
                        xpReward = tObj.optInt("xp", 30),
                        difficulty = tObj.optString("difficulty", "MEDIUM"),
                        deadlineTimestamp = System.currentTimeMillis() + (86400000 * days)
                    )
                )
            }
            Pair(newPath, newTasks)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(
                LearningPath(
                    title = "Custom Shield Blueprint",
                    description = "Meticulous breakdown of the provided syllabus snippet.",
                    difficulty = "Advanced",
                    xpReward = 150,
                    field = chosenField
                ),
                listOf(
                    Task(
                        title = "Audit Document Principles",
                        description = "Parse key aspects and build a cheat-sheet tracker.",
                        xpReward = 40,
                        difficulty = "MEDIUM"
                    )
                )
            )
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        // Strip markdown backticks if any
        var trimmed = raw.trim()
        if (trimmed.startsWith("```")) {
            val lines = trimmed.lines()
            val filteredLines = lines.filterNot { it.startsWith("```") }
            trimmed = filteredLines.joinToString("\n")
        }
        return trimmed.trim()
    }

    private fun parseTextFromGeminiResponse(rawJson: String): String {
        return try {
            val obj = JSONObject(rawJson)
            val candidates = obj.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        } catch (e: Exception) {
            "Error rendering scroll text: ${e.localizedMessage}. Please secure your standard shield."
        }
    }

    suspend fun generateQuizForTopic(topic: String): List<QuizQuestion> {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return getOfflineQuiz(topic)
        }

        val prompt = """
            Generate an interactive 3-question Multiple Choice Quiz (MCQ) for learning and revising the topic: "$topic".
            Each question must have exactly 4 plausible options, with exactly 1 correct option.
            Return a JSON array containing EXACTLY 3 objects. Conform EXACTLY to this schema. Return only raw, clean JSON text:
            [
               {
                  "question": "[Clear, concise question testing concepts]",
                  "options": [
                     "[Option A]",
                     "[Option B]",
                     "[Option C]",
                     "[Option D]"
                  ],
                  "correct_index": 2,
                  "explanation": "[Short 1-2 sentence description explaining why correct_index is the correct choice]"
               }
            ]
        """.trimIndent()

        val requestBody = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        return try {
            val responseBody = service.generateContent(apiKey, requestBody)
            val jsonString = responseBody.string()
            val cleanJson = cleanJsonResponse(jsonString)

            val jsonArray = JSONArray(cleanJson)
            val questions = mutableListOf<QuizQuestion>()
            for (i in 0 until jsonArray.length()) {
                val qObj = jsonArray.getJSONObject(i)
                val optionsArray = qObj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optionsArray.length()) {
                    options.add(optionsArray.getString(j))
                }
                questions.add(
                    QuizQuestion(
                        question = qObj.getString("question"),
                        options = options,
                        correctIndex = qObj.optInt("correct_index", 0),
                        explanation = qObj.getString("explanation")
                    )
                )
            }
            questions
        } catch (e: Exception) {
            e.printStackTrace()
            getOfflineQuiz(topic)
        }
    }

    private fun getOfflineQuiz(topic: String): List<QuizQuestion> {
        val normalized = topic.uppercase()
        return when {
            normalized.contains("KOTLIN") || normalized.contains("ANDROID") || normalized.contains("COMPOSE") -> {
                listOf(
                    QuizQuestion(
                        question = "Which Jetpack Compose function is used to handle side-effects that should run on successful composition?",
                        options = listOf("LaunchedEffect", "DisposableEffect", "SideEffect", "rememberUpdatedState"),
                        correctIndex = 0,
                        explanation = "LaunchedEffect runs suspend functions in the scope of a composition when specified key parameters change."
                    ),
                    QuizQuestion(
                        question = "What is the primary benefit of declared WindowInsets under standard Compose Scaffolds?",
                        options = listOf("Increases render FPS significantly", "Injects secure cryptographic padding", "Enables adaptive Edge-to-Edge display without clipping under the camera notch or status bar", "Encrypts local database queries"),
                        correctIndex = 2,
                        explanation = "WindowInsets are designed to adapt screens safely avoiding notches, keyboards, and gesture pill lines."
                    ),
                    QuizQuestion(
                        question = "Which delegate keyword compiles safe state tracking in standard Compose structures?",
                        options = listOf("var state by remember { mutableStateOf(...) }", "val state = remember { mutableStateOf(...) }", "state.collectAsStateWithLifecycle()", "Both A and C represent correct state paradigms"),
                        correctIndex = 3,
                        explanation = "Both remember-delegate variables and Flow lifecycle collectors represent safe, modern states."
                    )
                )
            }
            normalized.contains("SQL") || normalized.contains("DATABASE") || normalized.contains("ROOM") -> {
                listOf(
                    QuizQuestion(
                        question = "What annotation is required for modern Room reactive flow observation?",
                        options = listOf("@Dao", "@Database", "@Query", "@Entity"),
                        correctIndex = 2,
                        explanation = "@Query annotations define SQLite statements that Room auto-updates periodically."
                    ),
                    QuizQuestion(
                        question = "Under modern ACID principles, what does 'A' represent?",
                        options = listOf("Asymmetry", "Atomicity", "Analytics", "Automation"),
                        correctIndex = 1,
                        explanation = "Atomicity guarantees that a transaction group succeeds entirely or fails completely."
                    ),
                    QuizQuestion(
                        question = "How under modern Clean Code should we bridge UI with database access?",
                        options = listOf("Call Dao queries directly inside @Composables", "Introduce a local Repository Pattern layer to abstract database actions", "Hardcode SQLite statements in the view", "Disable database transactions completely"),
                        correctIndex = 1,
                        explanation = "Abstacting with a Repository separates data access concerns cleanly from UI."
                    )
                )
            }
            else -> {
                listOf(
                    QuizQuestion(
                        question = "When studying '$topic', what is a recommended first milestone to ensure high consistency?",
                        options = listOf("Learn basic specifications and core terms", "Start building high-complexity enterprise structures directly", "Ignore errors for the first few weeks", "Memorize code line-by-line"),
                        correctIndex = 0,
                        explanation = "Mastering foundational rules and terms provides the groundwork before solving complex challenges."
                    ),
                    QuizQuestion(
                        question = "How does breaking down '$topic' into customized daily milestones accelerate learning?",
                        options = listOf("It makes the curriculum harder", "It helps maintain focus, schedules deadlines, and unlocks active scoring rewards", "It makes studying redundant", "It resets your active database"),
                        correctIndex = 1,
                        explanation = "Breaking topics down prevents cognitive fatigue while rewards keep consistency active!"
                    ),
                    QuizQuestion(
                        question = "To verify mastery of '$topic', what is a perfect practice habit?",
                        options = listOf("Answering interactive MCQ quiz questions and testing code principles", "Reading slides without practicing", "Skipping to new fields daily", "Avoiding mock questions entirely"),
                        correctIndex = 0,
                        explanation = "Answering targeted check questions verifies knowledge gaps and anchors long-term learning."
                    )
                )
            }
        }
    }
}

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)
