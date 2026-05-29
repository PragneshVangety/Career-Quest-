package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.viewmodel.QuestViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom modern colors
val PureObsidian = Color(0xFF121214)
val SlateGrey = Color(0xFF1E1E24)
val ElectricViolet = Color(0xFF7C4DFF)
val BrightTeal = Color(0xFF03DAC6)
val SunsetOrange = Color(0xFFFF5722)
val GoldYellow = Color(0xFFFFC107)
val LitPurple = Color(0xFFD0BCFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationWrapper(viewModel: QuestViewModel) {
    val context = LocalContext.current
    val currentTab = viewModel.selectedTab.value
    val isDark = viewModel.isDarkMode.value

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_navigation_bar"),
                tonalElevation = 8.dp
            ) {
                val tabsList = listOf(
                    Triple("tasks", "Tasks List", Icons.Default.Assignment),
                    Triple("scores", "Gamified Scores", Icons.Default.EmojiEvents),
                    Triple("revision", "Topic Practice", Icons.Default.MenuBook),
                    Triple("consistency", "Consistency Hub", Icons.Default.NotificationsActive)
                )

                tabsList.forEach { (tabId, label, icon) ->
                    NavigationBarItem(
                        selected = currentTab == tabId,
                        onClick = { viewModel.selectedTab.value = tabId },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_item_$tabId")
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = if (isDark) PureObsidian else MaterialTheme.colorScheme.background
        ) {
            when (currentTab) {
                "tasks" -> TasksListView(viewModel, context)
                "scores" -> ScoresView(viewModel)
                "revision" -> TopicRevisionQuizView(viewModel)
                "consistency" -> ConsistencyHubView(viewModel, context)
                else -> TasksListView(viewModel, context)
            }
        }
    }
}

// ==========================================
// 1. TASKS LIST VIEW (TO-DO LIST)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksListView(viewModel: QuestViewModel, context: Context) {
    val tasks by viewModel.allTasks.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToRewardPopup by remember { mutableStateOf<Task?>(null) }
    
    var filterState by remember { mutableStateOf("Pending") } // Pending, Completed, All
    var filterCategory by remember { mutableStateOf("All") } // All, Learning, Day-to-Day

    val filteredTasks = tasks.filter { task ->
        val matchesStatus = when (filterState) {
            "Pending" -> !task.isCompleted
            "Completed" -> task.isCompleted
            else -> true
        }
        val matchesCategory = when (filterCategory) {
            "All" -> true
            else -> task.category == filterCategory
        }
        matchesStatus && matchesCategory
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Quest Log",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Organize tasks, complete quests & track learning",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = ElectricViolet,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_task_fab"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Quest", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Pending", "Completed", "All").forEach { mode ->
                    val isSelected = filterState == mode
                    val containerColor = if (isSelected) LitPurple else Color.White.copy(alpha = 0.05f)
                    val contentColor = if (isSelected) PureObsidian else Color.White.copy(alpha = 0.75f)
                    val borderColor = if (isSelected) LitPurple else Color.White.copy(alpha = 0.15f)

                    Box(
                        modifier = Modifier
                            .background(containerColor, RoundedCornerShape(100))
                            .border(1.dp, borderColor, RoundedCornerShape(100))
                            .clickable { filterState = mode }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("filter_chip_$mode"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Category filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Learning", "Day-to-Day").forEach { cat ->
                    val isSelected = filterCategory == cat
                    val containerColor = if (isSelected) BrightTeal else Color.White.copy(alpha = 0.05f)
                    val contentColor = if (isSelected) PureObsidian else Color.White.copy(alpha = 0.6f)
                    val borderColor = if (isSelected) BrightTeal else Color.White.copy(alpha = 0.15f)

                    Box(
                        modifier = Modifier
                            .background(containerColor, RoundedCornerShape(100))
                            .border(1.dp, borderColor, RoundedCornerShape(100))
                            .clickable { filterCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("filter_category_chip_$cat"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (cat == "All") "All Categories" else cat,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateGrey)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = "No tasks",
                                tint = ElectricViolet,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Your Quest Scroll is empty!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Create tasks to record goals, earn score points, increase levels, and keep your consistency streak blazing!",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = Color.White.copy(alpha = 0.62f)
                            )
                            Button(
                                onClick = { showAddTaskDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrightTeal,
                                    contentColor = PureObsidian
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Summon First Task", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = {
                                viewModel.completeTask(task.id)
                                if (!task.isCompleted) {
                                    // Trigger popup instantly!
                                    taskToRewardPopup = task
                                }
                            },
                            onDelete = {
                                viewModel.deleteTask(task)
                            }
                        )
                    }
                }
            }
        }
    }

    // Task Completion Pop-up (Consistent gamified encouragement)
    taskToRewardPopup?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToRewardPopup = null },
            confirmButton = {
                Button(
                    onClick = { taskToRewardPopup = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Praise Received!", color = PureObsidian, fontWeight = FontWeight.ExtraBold)
                }
            },
            title = {
                Text(
                    text = "🏆 Quest Accomplished!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = BrightTeal
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Outstanding work on completing \"${task.title}\"!",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text("🔥 Status:", fontWeight = FontWeight.Bold, color = GoldYellow)
                        Text(
                            text = "+${task.xpReward} XP Gained! Your consistency level has increased.",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Consistency is built brick by brick. Answer a dynamic MCQ Revision quiz in the next tab to solidify your understanding of this topic!",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SlateGrey,
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = BrightTeal,
                    modifier = Modifier.size(48.dp)
                )
            }
        )
    }

    // Add Quest Dialog
    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var difficulty by remember { mutableStateOf("EASY") } // EASY, MEDIUM, HEROIC
        var deadlineDays by remember { mutableStateOf("1") }
        var category by remember { mutableStateOf("Learning") } // Learning, Day-to-Day

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addTask(
                                title = title,
                                description = description,
                                difficulty = difficulty,
                                deadlineDays = deadlineDays.toIntOrNull() ?: 1,
                                category = category
                            )
                            showAddTaskDialog = false
                            viewModel.sendLocalNotification(
                                "Quest Summoned!",
                                "You added a new $difficulty $category challenge: '$title' to your Quest Log scrolls!"
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Assemble Quest", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Retreat", color = Color.White.copy(alpha = 0.85f))
                }
            },
            title = {
                Text(
                    text = "Summon New Quest Challenge",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Quest/Task Name") },
                        placeholder = { Text("e.g., Code a binary tree search / Morning exercise") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_task_title"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Topic / Details") },
                        placeholder = { Text("e.g. Master tree structures / 30 mins cardio gym run") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Text("Quest Category:", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Learning", "Day-to-Day").forEach { cat ->
                            val isSelected = category == cat
                            val baseColor = if (cat == "Learning") LitPurple else BrightTeal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) baseColor.copy(alpha = 0.2f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        2.dp,
                                        if (isSelected) baseColor else Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { category = cat }
                                    .padding(vertical = 10.dp)
                                    .testTag("category_select_$cat"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) baseColor else Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (category == "Day-to-Day") {
                        val daySuggestions = listOf(
                            Pair("Water Hydration", "Drink 8 glasses of fresh water throughout the day"),
                            Pair("Cardio Workout", "Complete 30 minutes of refreshing cardio exercise"),
                            Pair("Desk Cleanup", "De-clutter my desk and organize work layout"),
                            Pair("Book Reading", "Read 10 pages of a novel or self-help book"),
                            Pair("Mindful Breath", "Meditate or practice deep breathing for 10 minutes"),
                            Pair("Plan Tomorrow", "List the top 3 priorities for tomorrow's checklist")
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Random Day-to-Day Ideas:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                daySuggestions.forEach { (shortLabel, fullText) ->
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(100))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(100))
                                            .clickable {
                                                title = shortLabel
                                                description = fullText
                                                difficulty = "EASY"
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(shortLabel, fontSize = 11.sp, color = BrightTeal, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        val learningSuggestions = listOf(
                            Pair("Git Branching", "Interact with basic git commit, rebase, and checkout commands"),
                            Pair("OOP Concepts", "Revise inheritance, polymorphism, and encapsulation examples"),
                            Pair("Leetsolver", "Solve 2 array-based technical warmup questions"),
                            Pair("SQL Joins", "Practice Left/Right/Inner Joins query structures"),
                            Pair("API Reading", "Examine and test a public third-party REST API response block")
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Assembly Learning Ideas:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                learningSuggestions.forEach { (shortLabel, fullText) ->
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(100))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(100))
                                            .clickable {
                                                title = shortLabel
                                                description = fullText
                                                difficulty = "EASY"
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(shortLabel, fontSize = 11.sp, color = LitPurple, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Text("Challenge Difficulty Rate:", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("EASY", "MEDIUM", "HEROIC").forEach { level ->
                            val isSelected = difficulty == level
                            val baseColor = when (level) {
                                "HEROIC" -> SunsetOrange
                                "MEDIUM" -> GoldYellow
                                else -> BrightTeal
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) baseColor.copy(alpha = 0.2f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        2.dp,
                                        if (isSelected) baseColor else Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { difficulty = level }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) baseColor else Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = deadlineDays,
                        onValueChange = { deadlineDays = it },
                        label = { Text("Estimated Days remaining") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SlateGrey
        )
    }
}

@Composable
fun TaskCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val difficultyColor = when (task.difficulty.uppercase()) {
        "HEROIC" -> SunsetOrange
        "MEDIUM" -> GoldYellow
        else -> BrightTeal
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateGrey),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (task.isCompleted) Color.White.copy(alpha = 0.1f) else ElectricViolet.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checklist checkbox
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("checkbox_${task.id}")
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (task.isCompleted) BrightTeal else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (task.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Category badge
                    val catColor = if (task.category == "Learning") LitPurple else BrightTeal
                    Box(
                        modifier = Modifier
                            .background(catColor.copy(alpha = 0.15f), RoundedCornerShape(100))
                            .border(1.dp, catColor.copy(alpha = 0.3f), RoundedCornerShape(100))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = catColor
                        )
                    }

                    // Difficulty badge
                    Box(
                        modifier = Modifier
                            .background(difficultyColor.copy(alpha = 0.15f), RoundedCornerShape(100))
                            .border(1.dp, difficultyColor.copy(alpha = 0.3f), RoundedCornerShape(100))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.difficulty,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = difficultyColor
                        )
                    }

                    // Score prize reward
                    Text(
                        text = "+${task.xpReward} Score",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldYellow
                    )
                }
            }

            // Delete task button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_task_btn_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Quest",
                    tint = SunsetOrange.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// 2. GAMIFIED SCORES VIEW (RPG STATS & SCORE BOARD)
// ==========================================
@Composable
fun ScoresView(viewModel: QuestViewModel) {
    val uStats by viewModel.userStats.collectAsState()
    val stats = uStats ?: UserStats()
    val achievements by viewModel.achievements.collectAsState()

    val progressValue = stats.xp.toFloat() / stats.xpNeededForNextLevel.toFloat()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Gamified Scores",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track your level, consistency, and place on the scoreboard",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // 1. Character Level & EXP Progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("score_stats_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateGrey)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LEVEL ${stats.level}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricViolet
                            )
                            Text(
                                text = stats.levelTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Sparkly Circular Ring showing stats
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(ElectricViolet.copy(alpha = 0.1f), CircleShape)
                                .border(2.dp, ElectricViolet, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Progress bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Experience Points (XP)",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${stats.xp} / ${stats.xpNeededForNextLevel} XP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrightTeal
                            )
                        }
                        LinearProgressIndicator(
                            progress = progressValue.coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(100)),
                            color = BrightTeal,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }

        // 2. Streaks and Focus Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Streak card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGrey)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .background(SunsetOrange.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak Fire",
                                tint = SunsetOrange,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "${stats.currentStreak} Days",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Current Streak",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Record High Streak card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGrey)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .background(GoldYellow.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Longest Streak",
                                tint = GoldYellow,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "${stats.longestStreak} Days",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Longest Record",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 4. Consistency Achievements checklist
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("achievements_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateGrey)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🛡️ Consistency Trophies",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    achievements.forEach { achievement ->
                        val isUnlocked = achievement.isUnlocked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isUnlocked) BrightTeal.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Outlined.MilitaryTech,
                                    contentDescription = null,
                                    tint = if (isUnlocked) BrightTeal else Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = achievement.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.65f)
                                )
                                Text(
                                    text = achievement.description,
                                    fontSize = 11.sp,
                                    color = if (isUnlocked) Color.White.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. REVISION QUIZ VIEW (TOPIC MASTERY)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicRevisionQuizView(viewModel: QuestViewModel) {
    var searchTopic by remember { mutableStateOf("") }
    val isQuizLoading = viewModel.isQuizLoading.value
    val quizQuestions = viewModel.quizQuestions.value
    val isQuizActive = quizQuestions.isNotEmpty() && !viewModel.quizCompletedStatus.value
    val isQuizFinished = viewModel.quizCompletedStatus.value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Topic Revision Quiz",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Revise any subject through direct, interactive quizzes!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        if (!isQuizActive && !isQuizFinished) {
            // 1. Initial Start State Select Topic Screen
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("quiz_init_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGrey)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Verify & Solidify Knowledge",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Enter any topic name you want to practice — such as a programming language, dynamic programming algorithm, SQL commands, system design, or college placements subject. Our Gemini wizard will assemble an interactive multiple choice quiz to test your accuracy!",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        OutlinedTextField(
                            value = searchTopic,
                            onValueChange = { searchTopic = it },
                            label = { Text("Topic to Revise") },
                            placeholder = { Text("e.g. Kotlin Coroutines, Recursion, Trees") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("topic_quiz_search"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (searchTopic.isNotBlank()) {
                                    viewModel.startQuizForTopic(searchTopic)
                                }
                            })
                        )

                        Button(
                            onClick = {
                                if (searchTopic.isNotBlank()) {
                                    viewModel.startQuizForTopic(searchTopic)
                                }
                            },
                            enabled = searchTopic.isNotBlank() && !isQuizLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("summon_quiz_btn")
                        ) {
                            if (isQuizLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Summon Revision Quiz", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        // Helpful suggestions
                        Text(
                            text = "Quick Suggestion Paths:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Kotlin", "SQL Database", "Binary Trees").forEach { suggest ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(100))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(100))
                                        .clickable {
                                            searchTopic = suggest
                                            viewModel.startQuizForTopic(suggest)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(suggest, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else if (isQuizActive) {
            // 2. Active MCQ Quiz Game Screen
            val questionsCount = quizQuestions.size
            val currentIdx = viewModel.currentQuizIndex.value
            val currentQuestion = quizQuestions.getOrNull(currentIdx)

            if (currentQuestion != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("quiz_active_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateGrey)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Progress tag
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "REVISING: ${viewModel.quizTopic.value.uppercase()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ElectricViolet,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Question ${currentIdx + 1} of $questionsCount",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }

                            // Dynamic Linear Progress Indicator
                            LinearProgressIndicator(
                                progress = ((currentIdx + 1).toFloat() / questionsCount.toFloat()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(100)),
                                color = ElectricViolet,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )

                            Text(
                                text = currentQuestion.question,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Options cards
                            currentQuestion.options.forEachIndexed { optionIdx, textOption ->
                                val selectedAnswer = viewModel.selectedQuizOptionIndex.value
                                val hasAnswered = viewModel.hasAnsweredCurrentQuiz.value
                                val isSelectedThis = selectedAnswer == optionIdx

                                // Correct / Incorrect visual colors
                                val isCorrectOption = optionIdx == currentQuestion.correctIndex
                                val containerColor = when {
                                    hasAnswered && isCorrectOption -> Color(0xFF1E523A) // Green correct
                                    hasAnswered && isSelectedThis && !isCorrectOption -> Color(0xFF6B1D1D) // Red wrong
                                    else -> Color.White.copy(alpha = 0.04f)
                                }
                                val borderColor = when {
                                    hasAnswered && isCorrectOption -> Color(0xFF4CAF50)
                                    hasAnswered && isSelectedThis && !isCorrectOption -> Color(0xFFF44336)
                                    isSelectedThis -> ElectricViolet
                                    else -> Color.White.copy(alpha = 0.12f)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(containerColor, RoundedCornerShape(12.dp))
                                        .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (!hasAnswered) {
                                                viewModel.submitQuizAnswer(optionIdx)
                                            }
                                        }
                                        .padding(16.dp)
                                        .testTag("quiz_option_$optionIdx"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = textOption,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (hasAnswered) {
                                        Icon(
                                            imageVector = if (isCorrectOption) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (isCorrectOption) Color(0xFF4CAF50) else Color(0xFFF44336),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }

                            // Mastery explanation card
                            if (viewModel.hasAnsweredCurrentQuiz.value) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "💡 Concept Explanation:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = BrightTeal
                                        )
                                        Text(
                                            text = currentQuestion.explanation,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { viewModel.nextQuizQuestion() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("quiz_next_btn")
                                ) {
                                    Text(
                                        text = if (currentIdx + 1 < questionsCount) "Next Question" else "Reveal Revision Score",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (isQuizFinished) {
            // 3. Quiz Completion Screen
            val correctTotal = viewModel.quizCorrectCount.value
            val totalQuestions = quizQuestions.size
            val earnedXpScore = correctTotal * 15

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("quiz_finish_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGrey)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Success",
                            tint = GoldYellow,
                            modifier = Modifier.size(64.dp)
                        )

                        Text(
                            text = "Quiz Defeated!",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = Color.White
                        )

                        Text(
                            text = "You completed the training module for \"${viewModel.quizTopic.value}\"!",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        // Circular/box rating
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$correctTotal / $totalQuestions",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BrightTeal
                                )
                                Text("Correct Answers", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+$earnedXpScore XP",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GoldYellow
                                )
                                Text("Added Score Points", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.quizCompletedStatus.value = false
                                viewModel.quizQuestions.value = emptyList() // clear and reset
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Summon Another Quiz", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. CONSISTENCY & NOTIFICATIONS HUB
// ==========================================
@Composable
fun ConsistencyHubView(viewModel: QuestViewModel, context: Context) {
    val uStats by viewModel.userStats.collectAsState()
    val stats = uStats ?: UserStats()
    val isDark = viewModel.isDarkMode.value

    var showAlarmTimeSelection by remember { mutableStateOf(false) }
    var selectedAlarmHour by remember { mutableStateOf("09:00 AM") }
    var testDialogChallengeInput by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Consistency Hub",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Build deep habits, trigger pop-ups & set reminders",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // 1. Habit Motivation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("consistency_habit_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateGrey)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Why Consistency Matters?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Studies show that studying a topic for just 15 minutes a day has 4x better retention rates than a weekend cram session. Every time you check off a task or complete a revision quiz here, your streak remains active. Maintain active levels to score trophies!",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        lineHeight = 18.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ElectricViolet.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Mood, contentDescription = null, tint = ElectricViolet)
                        Text(
                            text = "Active Level Streak: ${stats.currentStreak} Days of Study",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 2. Consistent popups and alerts configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("consistency_controls_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateGrey)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BrightTeal)
                        Text(
                            text = "Notification Timers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Practice Reminder", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Alert sounds daily at $selectedAlarmHour", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                        }

                        Button(
                            onClick = { showAlarmTimeSelection = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet.copy(alpha = 0.2f), contentColor = ElectricViolet),
                            border = BorderStroke(1.dp, ElectricViolet)
                        ) {
                            Text("Change", fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    // Buttons to trigger Consistency popup and alerts
                    Text(
                        text = "Practice consistency triggers:",
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Button(
                        onClick = {
                            testDialogChallengeInput = "Active Today Challenge"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_consistency_dialog_btn")
                    ) {
                        Icon(Icons.Default.Forum, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger Simulated Habit Popup", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.sendLocalNotification(
                                "🔥 Study Goal Alert!",
                                "Keep your active ${stats.currentStreak}-day streak alive! Take 2 minutes to revise your learning topics!"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateGrey, contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_local_notif_btn")
                    ) {
                        Icon(Icons.Default.NotificationAdd, contentDescription = null, tint = BrightTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Local Notification Reminder", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Simple UI Theme Customizer
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("theme_controls_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateGrey)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Obsidian Dark Mode",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Eye-comfortable high-contrast dark palette",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.isDarkMode.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricViolet,
                            checkedTrackColor = ElectricViolet.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }

    // Change alarm hours dialog
    if (showAlarmTimeSelection) {
        val alarmTimes = listOf("08:00 AM", "09:00 AM", "12:00 PM", "05:00 PM", "08:00 PM")
        AlertDialog(
            onDismissRequest = { showAlarmTimeSelection = false },
            confirmButton = {
                TextButton(onClick = { showAlarmTimeSelection = false }) {
                    Text("Ready", color = BrightTeal, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text("Select Reminder Time", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    alarmTimes.forEach { rawTime ->
                        val isSelected = selectedAlarmHour == rawTime
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) ElectricViolet.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedAlarmHour = rawTime
                                    viewModel.sendLocalNotification(
                                        "Consistency reminder set!",
                                        "A daily consistency check-in alarm is now set for $rawTime"
                                    )
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(rawTime, color = Color.White, fontWeight = FontWeight.Bold)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ElectricViolet)
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SlateGrey
        )
    }

    // Test Consistency Habit dialog
    testDialogChallengeInput?.let { challenge ->
        AlertDialog(
            onDismissRequest = { testDialogChallengeInput = null },
            confirmButton = {
                Button(
                    onClick = {
                        // Reward some bonus points
                        viewModel.sendLocalNotification(
                            "Consistency Booster Triggered!",
                            "Awesome commitment, hero! You secured a daily consistency check-in +5 XP score!"
                        )
                        testDialogChallengeInput = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Affirm Habit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { testDialogChallengeInput = null }) {
                    Text("Postpone", color = Color.White.copy(alpha = 0.6f))
                }
            },
            title = {
                Text(
                    text = "🔥 Daily Consistency Check-in!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = ElectricViolet
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Are you prepared to level up your career goals today?",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Maintaining daily habits ensures long-term placement victory. Tap 'Affirm' to maintain your daily study routine!",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SlateGrey,
            icon = {
                Icon(
                    imageVector = Icons.Default.OfflineBolt,
                    contentDescription = null,
                    tint = ElectricViolet,
                    modifier = Modifier.size(44.dp)
                )
            }
        )
    }
}
