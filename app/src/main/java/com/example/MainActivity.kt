package com.example

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        SuqoonApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuqoonApp() {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("suqoon_prefs", Context.MODE_PRIVATE) }

  var currentTab by remember { 
    mutableStateOf(prefs.getInt("current_tab", 0)) 
  }
  var selectedMood by remember { 
    mutableStateOf(prefs.getString("selected_mood", null)) 
  }
  var questsChecked by remember {
    mutableStateOf(
      listOf(
        prefs.getBoolean("quest_checked_0", false),
        prefs.getBoolean("quest_checked_1", false),
        prefs.getBoolean("quest_checked_2", false)
      )
    )
  }
  var questDescriptions by remember {
    mutableStateOf(
      listOf(
        prefs.getString("quest_desc_0", "") ?: "",
        prefs.getString("quest_desc_1", "") ?: "",
        prefs.getString("quest_desc_2", "") ?: ""
      )
    )
  }
  var questPhotosSnapped by remember {
    mutableStateOf(
      listOf(
        prefs.getBoolean("quest_photo_0", false),
        prefs.getBoolean("quest_photo_1", false),
        prefs.getBoolean("quest_photo_2", false)
      )
    )
  }
  var screenTime by remember { 
    mutableStateOf(prefs.getFloat("screen_time", 7.5f)) 
  }
  var sleepLog by remember { 
    mutableStateOf(prefs.getFloat("sleep_log", 5.5f)) 
  }
  var showResetModal by rememberSaveable { mutableStateOf(false) }
  var showAccountPage by rememberSaveable { mutableStateOf(false) }
  var userName by remember { 
    mutableStateOf(prefs.getString("user_name", "Sami") ?: "Sami") 
  }
  var screenTimeGoal by remember { 
    mutableStateOf(prefs.getFloat("screen_time_goal", 6.0f)) 
  }

  // Persist states reactively to SharedPreferences
  LaunchedEffect(currentTab) {
    prefs.edit().putInt("current_tab", currentTab).apply()
  }
  LaunchedEffect(selectedMood) {
    prefs.edit().putString("selected_mood", selectedMood).apply()
  }
  LaunchedEffect(questsChecked) {
    prefs.edit()
      .putBoolean("quest_checked_0", questsChecked.getOrElse(0) { false })
      .putBoolean("quest_checked_1", questsChecked.getOrElse(1) { false })
      .putBoolean("quest_checked_2", questsChecked.getOrElse(2) { false })
      .apply()
  }
  LaunchedEffect(questDescriptions) {
    prefs.edit()
      .putString("quest_desc_0", questDescriptions.getOrElse(0) { "" })
      .putString("quest_desc_1", questDescriptions.getOrElse(1) { "" })
      .putString("quest_desc_2", questDescriptions.getOrElse(2) { "" })
      .apply()
  }
  LaunchedEffect(questPhotosSnapped) {
    prefs.edit()
      .putBoolean("quest_photo_0", questPhotosSnapped.getOrElse(0) { false })
      .putBoolean("quest_photo_1", questPhotosSnapped.getOrElse(1) { false })
      .putBoolean("quest_photo_2", questPhotosSnapped.getOrElse(2) { false })
      .apply()
  }
  LaunchedEffect(screenTime) {
    prefs.edit().putFloat("screen_time", screenTime).apply()
  }
  LaunchedEffect(sleepLog) {
    prefs.edit().putFloat("sleep_log", sleepLog).apply()
  }
  LaunchedEffect(userName) {
    prefs.edit().putString("user_name", userName).apply()
  }
  LaunchedEffect(screenTimeGoal) {
    prefs.edit().putFloat("screen_time_goal", screenTimeGoal).apply()
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
        NavigationBar(
          containerColor = Color.White,
          tonalElevation = 8.dp,
          modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
          NavigationBarItem(
            selected = currentTab == 0,
            onClick = { currentTab = 0 },
            icon = {
              Icon(
                imageVector = if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                contentDescription = "Home"
              )
            },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = AccentBlue,
              selectedTextColor = AccentBlue,
              indicatorColor = AccentBlueSoft,
              unselectedIconColor = MutedGray,
              unselectedTextColor = MutedGray
            ),
            modifier = Modifier.testTag("home_tab")
          )
          NavigationBarItem(
            selected = currentTab == 1,
            onClick = { currentTab = 1 },
            icon = {
              Icon(
                imageVector = if (currentTab == 1) Icons.Filled.Group else Icons.Outlined.Group,
                contentDescription = "Family Harmony"
              )
            },
            label = { Text("Harmony") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = AccentGreen,
              selectedTextColor = AccentGreen,
              indicatorColor = AccentGreenSoft,
              unselectedIconColor = MutedGray,
              unselectedTextColor = MutedGray
            ),
            modifier = Modifier.testTag("family_tab")
          )
          NavigationBarItem(
            selected = currentTab == 2,
            onClick = { currentTab = 2 },
            icon = {
              Icon(
                imageVector = if (currentTab == 2) Icons.Filled.Spa else Icons.Outlined.Spa,
                contentDescription = "Quests"
              )
            },
            label = { Text("Quests") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = AccentBlue,
              selectedTextColor = AccentBlue,
              indicatorColor = AccentBlueSoft,
              unselectedIconColor = MutedGray,
              unselectedTextColor = MutedGray
            ),
            modifier = Modifier.testTag("quests_tab")
          )
        }
      },
      containerColor = SoftNeutralBackground
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        AnimatedContent(
          targetState = currentTab,
          transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
          },
          label = "TabTransition"
        ) { targetTab ->
          when (targetTab) {
            0 -> HomeDashboardView(
              userName = userName,
              onAccountClick = { showAccountPage = true },
              selectedMood = selectedMood,
              onMoodSelect = { mood ->
                selectedMood = mood
                if (mood == "Tired" || mood == "Stressed") {
                  showResetModal = true
                }
              },
              onNavigateToHarmony = { currentTab = 1 },
              screenTime = screenTime,
              onScreenTimeChange = { screenTime = it },
              sleepLog = sleepLog,
              onSleepLogChange = { sleepLog = it },
              screenTimeGoal = screenTimeGoal,
              onScreenTimeGoalChange = { screenTimeGoal = it }
            )
            1 -> FamilyHarmonyView(
              currentUserDisplayName = userName,
              currentUserMood = selectedMood ?: "Tired",
              currentUserScreenTime = screenTime
            )
            2 -> ReconnectionQuestsView(
              questsChecked = questsChecked,
              questDescriptions = questDescriptions,
              questPhotosSnapped = questPhotosSnapped,
              onQuestComplete = { index, desc ->
                val newChecked = questsChecked.toMutableList()
                newChecked[index] = true
                questsChecked = newChecked

                val newDesc = questDescriptions.toMutableList()
                newDesc[index] = desc
                questDescriptions = newDesc

                val newPhotos = questPhotosSnapped.toMutableList()
                newPhotos[index] = true
                questPhotosSnapped = newPhotos
              },
              onQuestReset = { index ->
                val newChecked = questsChecked.toMutableList()
                newChecked[index] = false
                questsChecked = newChecked

                val newDesc = questDescriptions.toMutableList()
                newDesc[index] = ""
                questDescriptions = newDesc

                val newPhotos = questPhotosSnapped.toMutableList()
                newPhotos[index] = false
                questPhotosSnapped = newPhotos
              }
            )
          }
        }
      }
    }

    // Full screen Slide Up Mental Reset Animation Modal Overlay
    AnimatedVisibility(
      visible = showResetModal,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      MentalResetModal(onClose = { showResetModal = false })
    }

    // Full screen Slide Up Account Page Overlay
    AnimatedVisibility(
      visible = showAccountPage,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      AccountScreen(
        userName = userName,
        onUserNameChange = { userName = it },
        onResetApp = {
          prefs.edit().clear().apply()
          currentTab = 0
          selectedMood = null
          questsChecked = listOf(false, false, false)
          questDescriptions = listOf("", "", "")
          questPhotosSnapped = listOf(false, false, false)
          screenTime = 7.5f
          sleepLog = 5.5f
          showResetModal = false
          userName = "Sami"
          screenTimeGoal = 6.0f
          showAccountPage = false
        },
        onClose = { showAccountPage = false }
      )
    }
  }
}

@Composable
fun HomeDashboardView(
  userName: String,
  onAccountClick: () -> Unit,
  selectedMood: String?,
  onMoodSelect: (String) -> Unit,
  onNavigateToHarmony: () -> Unit,
  screenTime: Float,
  onScreenTimeChange: (Float) -> Unit,
  sleepLog: Float,
  onSleepLogChange: (Float) -> Unit,
  screenTimeGoal: Float,
  onScreenTimeGoalChange: (Float) -> Unit
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("suqoon_prefs", Context.MODE_PRIVATE) }
  var aiRecommendations by remember {
    mutableStateOf(prefs.getString("ai_detox_recommendations", null))
  }
  var aiLoading by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(aiRecommendations) {
    if (aiRecommendations != null) {
      prefs.edit().putString("ai_detox_recommendations", aiRecommendations).apply()
    } else {
      prefs.edit().remove("ai_detox_recommendations").apply()
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "PulseWarningDot")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "WarningDotAlpha"
  )

  // Live client-side evaluation engine matrix
  val currentScore = ((screenTime / 12f) * 60f + ((10f - sleepLog) / 6f) * 40f).coerceIn(5f, 100f)
  val weeklyScores = listOf(42f, 68f, 85f, 58f, 35f, 22f, currentScore)
  val weeklyDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

  val riskConfig = when {
    screenTime > 7.0f && sleepLog < 6.0f -> {
      RiskConfig(
        title = "🔴 HIGH RISK",
        desc = "Your late-night screen usage + low sleep is matching high burnout indicators.",
        bgColor = Color(0xFFFFF5F5),
        borderColor = Color(0xFFFEE2E2),
        textColor = Color(0xFFDC2626),
        dotColor = Color(0xFFEF4444)
      )
    }
    screenTime < 4.0f && sleepLog > 7.0f -> {
      RiskConfig(
        title = "🟢 LOW RISK",
        desc = "Great job! Your digital fatigue is extremely low. Keep up this healthy balance.",
        bgColor = Color(0xFFE2F7EA),
        borderColor = Color(0xFFBFF0D4),
        textColor = Color(0xFF047857),
        dotColor = Color(0xFF10B981)
      )
    }
    else -> {
      RiskConfig(
        title = "🟡 MODERATE RISK",
        desc = "Your digital status is moderately balanced but could improve.",
        bgColor = Color(0xFFFFFBEB),
        borderColor = Color(0xFFFEF3C7),
        textColor = Color(0xFFD97706),
        dotColor = Color(0xFFF59E0B)
      )
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // Top Bar Welcome Greeting
    item {
      Spacer(modifier = Modifier.height(16.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Good morning, $userName 👋",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 24.sp,
              color = DarkSlate
            )
          )
          Text(
            text = "Your digital balance today",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MutedGray,
              fontSize = 14.sp
            )
          )
        }
        // Rounded profile placeholder avatar
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .background(AccentBlueSoft)
            .clickable { onAccountClick() }
            .testTag("account_profile_button"),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(AccentBlue.copy(alpha = 0.2f))
          )
          Text(
            text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "S",
            color = AccentBlue,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp
          )
        }
      }
    }

    // Dynamic Burnout Evaluation Indicator Card
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(
            elevation = 2.dp,
            shape = RoundedCornerShape(32.dp),
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.04f)
          )
          .clip(RoundedCornerShape(32.dp))
          .background(riskConfig.bgColor)
          .border(1.dp, riskConfig.borderColor, RoundedCornerShape(32.dp))
          .padding(20.dp)
      ) {
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(riskConfig.dotColor.copy(alpha = pulseAlpha))
            )
            Text(
              text = riskConfig.title,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = riskConfig.textColor,
                fontSize = 11.sp,
                letterSpacing = 1.sp
              )
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Burnout AI Tracker Evaluation",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = DarkSlate,
              fontSize = 18.sp
            )
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = riskConfig.desc,
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MutedGray,
              fontSize = 14.sp,
              lineHeight = 20.sp
            )
          )
        }
      }
    }

    // Burnout AI Tracker Control Center (Sliders)
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(
            elevation = 3.dp,
            shape = RoundedCornerShape(32.dp),
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.04f)
          )
          .clip(RoundedCornerShape(32.dp))
          .background(Color.White)
          .border(1.dp, Color.White, RoundedCornerShape(32.dp))
          .padding(20.dp)
      ) {
        Column {
          Text(
            text = "Burnout AI Tracker Sliders",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = DarkSlate,
              fontSize = 15.sp
            )
          )
          Text(
            text = "Simulate values to recalculate risk indicators instantly",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MutedGray,
              fontSize = 12.sp
            )
          )
          Spacer(modifier = Modifier.height(16.dp))

          // Screen Time Slider Layout
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(text = "📱", fontSize = 16.sp)
              Text(
                text = "Screen Time Limit",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = DarkSlate,
                  fontSize = 13.sp
                )
              )
            }
            Text(
              text = "${screenTime}h",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AccentBlue,
                fontSize = 14.sp
              )
            )
          }
          Slider(
            value = screenTime,
            onValueChange = onScreenTimeChange,
            valueRange = 1f..12f,
            steps = 21,
            colors = SliderDefaults.colors(
              thumbColor = AccentBlue,
              activeTrackColor = AccentBlue,
              inactiveTrackColor = AccentBlueSoft
            ),
            modifier = Modifier.testTag("screen_time_slider")
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Sleep Log Slider Layout
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(text = "😴", fontSize = 16.sp)
              Text(
                text = "Sleep Log Hours",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = DarkSlate,
                  fontSize = 13.sp
                )
              )
            }
            Text(
              text = "${sleepLog}h",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AccentGreen,
                fontSize = 14.sp
              )
            )
          }
          Slider(
            value = sleepLog,
            onValueChange = onSleepLogChange,
            valueRange = 4f..10f,
            steps = 11,
            colors = SliderDefaults.colors(
              thumbColor = AccentGreen,
              activeTrackColor = AccentGreen,
              inactiveTrackColor = AccentGreenSoft
            ),
            modifier = Modifier.testTag("sleep_log_slider")
          )
        }
      }
    }

    // Manual Screen Time & Goal Logger
    item {
      ManualScreenTimeLogCard(
        screenTime = screenTime,
        onScreenTimeChange = onScreenTimeChange,
        screenTimeGoal = screenTimeGoal,
        onScreenTimeGoalChange = onScreenTimeGoalChange
      )
    }

    // Quick Grid Row (Updates live based on screenTime & sleepLog sliders)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricItemCard(
          modifier = Modifier.weight(1f),
          value = "${screenTime}h",
          title = "Screen",
          valueColor = AccentBlue
        )
        MetricItemCard(
          modifier = Modifier.weight(1f),
          value = "${sleepLog}h",
          title = "Sleep",
          valueColor = AccentGreen
        )
        MetricItemCard(
          modifier = Modifier.weight(1f),
          value = selectedMood ?: "Tired",
          title = "Mood",
          valueColor = Color(0xFFF59E0B) // Amber
        )
      }
    }

    // Emotion Check-in Component
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(
            elevation = 3.dp,
            shape = RoundedCornerShape(32.dp),
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.04f)
          )
          .clip(RoundedCornerShape(32.dp))
          .background(Color.White)
          .border(1.dp, Color.White, RoundedCornerShape(32.dp))
          .padding(20.dp)
      ) {
        Column {
          Text(
            text = "How are you feeling right now?",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = DarkSlate,
              fontSize = 15.sp
            )
          )
          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val moods = listOf(
              MoodOption("😴", "Tired", "mood_tired"),
              MoodOption("😟", "Stressed", "mood_stressed"),
              MoodOption("🙂", "Okay", "mood_okay"),
              MoodOption("😄", "Happy", "mood_happy")
            )
            moods.forEach { mood ->
              val isSelected = selectedMood == mood.name
              Box(
                modifier = Modifier
                   .weight(1f)
                   .testTag(mood.tag)
                   .clip(RoundedCornerShape(16.dp))
                   .border(
                     width = 1.dp,
                     color = if (isSelected) AccentBlue else Color.Transparent,
                     shape = RoundedCornerShape(16.dp)
                   )
                   .background(
                     if (isSelected) AccentBlueSoft else Color.Transparent
                   )
                   .clickable { onMoodSelect(mood.name) }
                   .padding(vertical = 12.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(text = mood.emoji, fontSize = 24.sp)
                  Text(
                    text = mood.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isSelected) AccentBlue else MutedGray,
                      fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }
    }

    // Weekly Trends Line Chart Card Widget
    item {
      WeeklyTrendsChartCard(
        weeklyScores = weeklyScores,
        weeklyDays = weeklyDays
      )
    }

    // AI Detox & Wellness Companion Suggestions Card
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(
            elevation = 3.dp,
            shape = RoundedCornerShape(32.dp),
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.04f)
          )
          .clip(RoundedCornerShape(32.dp))
          .background(Color(0xFFFBF9FF))
          .border(1.dp, Color(0xFFEADBFF), RoundedCornerShape(32.dp))
          .padding(20.dp)
          .testTag("ai_detox_companion_card")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.Spa,
                  contentDescription = "AI Companion Icon",
                  tint = Color(0xFF8B5CF6),
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "SUQOON AI",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED),
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                  )
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Personal Detox Coach",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = DarkSlate,
                  fontSize = 17.sp
                )
              )
            }

            Button(
              onClick = {
                if (!aiLoading) {
                  aiLoading = true
                  scope.launch {
                    val result = GeminiService.getDetoxRecommendations(
                      userName = userName,
                      screenTime = screenTime,
                      sleepLog = sleepLog,
                      screenTimeGoal = screenTimeGoal,
                      mood = selectedMood
                    )
                    aiRecommendations = result
                    aiLoading = false
                  }
                }
              },
              enabled = !aiLoading,
              shape = RoundedCornerShape(50),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B5CF6),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFD8B4FE),
                disabledContentColor = Color.White
              ),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
              modifier = Modifier
                .height(34.dp)
                .testTag("get_ai_tips_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                if (aiLoading) {
                  CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp
                  )
                  Text(text = "Analyzing...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                  Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                  )
                  Text(
                    text = if (aiRecommendations != null) "Refresh Tips" else "Get Coached",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(20.dp))
              .background(Color.White)
              .border(1.dp, Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
              .padding(16.dp)
          ) {
            Column {
              if (aiRecommendations == null) {
                Text(
                  text = "Connect with Suqoon AI to evaluate your screen time ($screenTime hrs) and sleep duration ($sleepLog hrs) metrics. Our AI counselor will prompt 3 personalized physical replacement suggestions and detox habits for you.",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    color = MutedGray,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                  ),
                  modifier = Modifier.testTag("ai_tips_initial_message")
                )
              } else {
                Text(
                  text = aiRecommendations ?: "",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    color = DarkSlate,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                  ),
                  modifier = Modifier.testTag("ai_recommendations_content")
                )
              }

              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEEF2F6))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                  Text(
                    text = "Usage: $screenTime h",
                    fontSize = 10.sp,
                    color = MutedGray,
                    fontWeight = FontWeight.Bold
                  )
                }
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEEF2F6))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                  Text(
                    text = "Sleep: $sleepLog h",
                    fontSize = 10.sp,
                    color = MutedGray,
                    fontWeight = FontWeight.Bold
                  )
                }
                if (!selectedMood.isNullOrBlank()) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFFEEF2F6))
                      .padding(horizontal = 6.dp, vertical = 3.dp)
                  ) {
                    Text(
                      text = "Mood: $selectedMood",
                      fontSize = 10.sp,
                      color = MutedGray,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Family Harmony Highlight Snippet Widget
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(
            elevation = 2.dp,
            shape = RoundedCornerShape(32.dp),
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.04f)
          )
          .clip(RoundedCornerShape(32.dp))
          .background(AccentGreenSoft)
          .border(1.dp, Color(0xFFBFF0D4), RoundedCornerShape(32.dp))
          .clickable { onNavigateToHarmony() }
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // High-fidelity progress representation
            Box(
              modifier = Modifier.size(48.dp),
              contentAlignment = Alignment.Center
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                // White background track
                drawCircle(
                  color = Color.White,
                  radius = size.minDimension / 2 - 2.dp.toPx(),
                  style = Stroke(width = 3.dp.toPx())
                )
                // Green indicator
                drawArc(
                  color = AccentGreen,
                  startAngle = -90f,
                  sweepAngle = 360f * 0.65f,
                  useCenter = false,
                  style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
              }
              Text(
                text = "65%",
                color = AccentGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            }
            Column {
              Text(
                text = "Family Harmony Score",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = DarkSlate,
                  fontSize = 15.sp
                )
              )
              Text(
                text = "Moderately Balanced Today",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = Color(0xFF065F46).copy(alpha = 0.8f),
                  fontSize = 12.sp
                )
              )
            }
          }

          // Rounded trailing details button
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(Color.White)
              .shadow(1.dp, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "View Details",
              tint = AccentGreen,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
fun MetricItemCard(
  modifier: Modifier = Modifier,
  value: String,
  title: String,
  valueColor: Color
) {
  Box(
    modifier = modifier
      .shadow(
        elevation = 3.dp,
        shape = RoundedCornerShape(24.dp),
        ambientColor = Color.Black.copy(alpha = 0.03f),
        spotColor = Color.Black.copy(alpha = 0.03f)
      )
      .clip(RoundedCornerShape(24.dp))
      .background(Color.White)
      .border(1.dp, Color.White, RoundedCornerShape(24.dp))
      .padding(vertical = 16.dp, horizontal = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          color = MutedGray,
          fontSize = 10.sp,
          letterSpacing = 0.5.sp
        )
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = valueColor,
          fontSize = 18.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun FamilyHarmonyView(
  currentUserDisplayName: String,
  currentUserMood: String,
  currentUserScreenTime: Float
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("suqoon_prefs", Context.MODE_PRIVATE) }

  var stressLevel by remember {
    mutableStateOf(prefs.getFloat("ai_family_stress_level", 5f))
  }
  var aiRecommendations by remember {
    mutableStateOf(prefs.getString("ai_family_recommendations", null))
  }
  var selectedTimeMode by remember {
    mutableStateOf(prefs.getString("ai_family_time_mode", "Current System Time") ?: "Current System Time")
  }
  var aiLoading by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // Header
    item {
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Family Harmony Index",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 24.sp,
          color = DarkSlate
        )
      )
      Text(
        text = "Tracking collective screen balance seamlessly",
        style = MaterialTheme.typography.bodyMedium.copy(
          color = MutedGray,
          fontSize = 14.sp
        )
      )
    }

    // Double Gauge with text overlays
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(2.dp, RoundedCornerShape(28.dp))
          .clip(RoundedCornerShape(28.dp))
          .background(Color.White)
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier.size(170.dp),
            contentAlignment = Alignment.Center
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              // Soft background track
              drawArc(
                color = AccentGreenSoft,
                startAngle = -210f,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round)
              )
              // Highly visible green score indicator
              drawArc(
                color = AccentGreen,
                startAngle = -210f,
                sweepAngle = 240f * 0.65f, // 65/100
                useCenter = false,
                style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round)
              )
            }
            Column(
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "65/100",
                style = MaterialTheme.typography.headlineLarge.copy(
                  fontWeight = FontWeight.ExtraBold,
                  color = DarkSlate,
                  fontSize = 32.sp
                )
              )
              Text(
                text = "Moderately Balanced",
                style = MaterialTheme.typography.labelMedium.copy(
                  color = AccentGreen,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              )
            }
          }
        }
      }
    }

    // AI Family Harmonizer Recommendations Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(2.dp, RoundedCornerShape(28.dp))
          .clip(RoundedCornerShape(28.dp))
          .background(Color(0xFFF7F9FC))
          .border(1.dp, Color(0xFFDCE2EC), RoundedCornerShape(28.dp))
          .testTag("ai_family_harmonizer_card"),
        colors = CardDefaults.cardColors(containerColor = Color.White)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          // Card Title Block
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = "AI Icon",
                  tint = AccentGreen,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "AI FAMILY HARMONIZER",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                  )
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Bonding Activity Planner",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = DarkSlate,
                  fontSize = 18.sp
                )
              )
            }
            
            // Sub-badge for time status
            val resolvedTime = remember(selectedTimeMode) {
              if (selectedTimeMode == "Current System Time") {
                java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
              } else {
                selectedTimeMode
              }
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AccentBlueSoft)
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = resolvedTime,
                color = AccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "Fine-tune your conditions to receive screen-free interactive bonding ideas custom-fitted to your stress metrics at this hour.",
            style = MaterialTheme.typography.bodySmall.copy(color = MutedGray, fontSize = 12.5.sp, lineHeight = 17.sp)
          )

          Spacer(modifier = Modifier.height(16.dp))

          // 1. Stress Level Regulator Slider
          Text(
            text = "Your Stress Level Indicator:",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = DarkSlate, fontSize = 13.sp)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            val stressText = when {
              stressLevel >= 8f -> "🚨 High Stress / Exhausted"
              stressLevel >= 4f -> "☕ Moderately Stressed"
              else -> "🍃 Calm & Balanced"
            }
            val stressColor = when {
              stressLevel >= 8f -> SoftRed
              stressLevel >= 4f -> AmberBurnout
              else -> AccentGreen
            }
            Text(
              text = stressText,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = stressColor)
            )
            Text(
              text = "${stressLevel.toInt()}/10",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold, color = DarkSlate)
            )
          }
          Slider(
            value = stressLevel,
            onValueChange = {
              stressLevel = it
              prefs.edit().putFloat("ai_family_stress_level", it).apply()
            },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
              thumbColor = AccentBlue,
              activeTrackColor = AccentBlue,
              inactiveTrackColor = SoftNeutralBackground
            ),
            modifier = Modifier.testTag("ai_family_stress_slider")
          )

          Spacer(modifier = Modifier.height(12.dp))

          // 2. Time-of-Day Override Selectors (for demo flexibility)
          Text(
            text = "Select Time Period:",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = DarkSlate, fontSize = 13.sp)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            val modes = listOf("Current System Time", "08:00 AM (Morning)", "02:00 PM (Afternoon)", "09:00 PM (Evening)")
            modes.forEach { mode ->
              val isSelected = selectedTimeMode == mode
              val modeColor = if (isSelected) AccentBlue else Color.Transparent
              val contentColor = if (isSelected) Color.White else MutedGray
              val borderColor = if (isSelected) AccentBlue else Color(0xFFECEFF3)
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) AccentBlue else Color.White)
                  .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                  .clickable {
                    selectedTimeMode = mode
                    prefs.edit().putString("ai_family_time_mode", mode).apply()
                  }
                  .padding(vertical = 5.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = mode.replace(" (Morning)", "").replace(" (Afternoon)", "").replace(" (Evening)", "").replace("Current ", ""),
                  color = contentColor,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // 3. Action Button
          Button(
            onClick = {
              if (!aiLoading) {
                aiLoading = true
                scope.launch {
                  val resolvedTime = if (selectedTimeMode == "Current System Time") {
                    java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                  } else {
                    selectedTimeMode
                  }
                  val result = GeminiService.getFamilyRecommendations(
                    userName = currentUserDisplayName,
                    stressLevel = stressLevel.toInt(),
                    currentTime = resolvedTime
                  )
                  aiRecommendations = result
                  prefs.edit().putString("ai_family_recommendations", result).apply()
                  aiLoading = false
                }
              }
            },
            enabled = !aiLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .testTag("ai_family_request_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = AccentBlue,
              contentColor = Color.White,
              disabledContainerColor = AccentBlueSoft,
              disabledContentColor = AccentBlue
            )
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              if (aiLoading) {
                CircularProgressIndicator(
                  color = Color.White,
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp
                )
                Text("Analyzing Household State...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
              } else {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = if (aiRecommendations != null) "Refresh AI Recommendations" else "Consult AI Harmonizer",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          if (aiRecommendations != null || aiLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFECEFF3), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))
            
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF9FAFC))
                .border(1.dp, Color(0xFFEFF1F5), RoundedCornerShape(16.dp))
                .padding(16.dp)
            ) {
              if (aiLoading) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = "Suqoon AI is matching stress indexes with collective family time gaps...",
                    fontSize = 12.5.sp,
                    color = MutedGray,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                  )
                }
              } else {
                Text(
                  text = aiRecommendations ?: "",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    color = DarkSlate,
                    fontSize = 13.5.sp,
                    lineHeight = 19.5.sp
                  ),
                  modifier = Modifier.testTag("ai_family_recommendations_text")
                )
              }
            }
          }
        }
      }
    }

    // List Header
    item {
      Text(
        text = "HOUSEHOLD BREAKDOWN",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          color = MutedGray,
          letterSpacing = 1.2.sp
        )
      )
    }

    // Household Breakdown List items
    item {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // SAMI (Highest Screen warning highlighted dynamically based on sliders)
        val isSamiHighest = currentUserScreenTime > 5.0f
        HouseholdMemberRow(
          name = "$currentUserDisplayName (Student)",
          moodDescription = "Mood: $currentUserMood",
          screenTime = "${currentUserScreenTime} hrs screen",
          isHighest = isSamiHighest,
          avatarInitials = if (currentUserDisplayName.isNotEmpty()) currentUserDisplayName.take(1).uppercase() else "S",
          avatarColor = AccentBlue
        )

        // MOM
        HouseholdMemberRow(
          name = "Mom (Working Parent)",
          moodDescription = "Mood: Okay",
          screenTime = "4.0 hrs screen",
          isHighest = !isSamiHighest && (4.0f > 5.0f), // MOM can't exceed dad's 5.0 in this simulation
          avatarInitials = "M",
          avatarColor = AccentGreen
        )

        // DAD
        HouseholdMemberRow(
          name = "Dad (Work Mode)",
          moodDescription = "Mood: Happy",
          screenTime = "5.0 hrs screen",
          isHighest = !isSamiHighest && (5.0f >= 4.0f), // DAD is highest if Sami drops below 5.0
          avatarInitials = "D",
          avatarColor = AmberBurnout
        )
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
fun HouseholdMemberRow(
  name: String,
  moodDescription: String,
  screenTime: String,
  isHighest: Boolean,
  avatarInitials: String,
  avatarColor: Color
) {
  // Bold outline if Sami as highest
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(1.dp, RoundedCornerShape(20.dp))
      .then(
        if (isHighest) Modifier.border(1.5.dp, AmberBurnout, RoundedCornerShape(20.dp))
        else Modifier
      ),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(20.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(avatarColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = avatarInitials,
            color = avatarColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = name,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = DarkSlate,
                fontSize = 15.sp
              )
            )
            if (isHighest) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(SoftRed.copy(alpha = 0.12f))
                  .padding(horizontal = 4.dp, vertical = 1.dp)
              ) {
                Text(
                  text = "HIGHEST",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = SoftRed,
                    fontSize = 8.sp,
                    letterSpacing = 0.5.sp
                  )
                )
              }
            }
          }
          Text(
            text = moodDescription,
            style = MaterialTheme.typography.bodySmall.copy(
              color = MutedGray,
              fontSize = 12.sp
            )
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(
            if (isHighest) LightAmber else SoftNeutralBackground
          )
          .padding(vertical = 6.dp, horizontal = 10.dp)
      ) {
        Text(
          text = screenTime,
          style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold,
            color = if (isHighest) AmberBurnout else DarkSlate,
            fontSize = 12.sp
          )
        )
      }
    }
  }
}

@Composable
fun QuestPolaroidPreview(
  questIndex: Int,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .background(Color.White)
      .padding(6.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
      ) {
        when (questIndex) {
          0 -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
              drawRect(
                brush = Brush.radialGradient(
                  colors = listOf(Color(0xFFE2F6F3), Color(0xFF88D2C4), Color(0xFF3B8377))
                )
              )
              drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = size.minDimension / 3.2f,
                style = Stroke(width = 4.dp.toPx())
              )
              drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = size.minDimension / 2.2f,
                style = Stroke(width = 2.dp.toPx())
              )
              drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = size.minDimension / 5.0f,
                style = Stroke(width = 6.dp.toPx())
              )
            }
            Icon(
              imageVector = Icons.Filled.Spa,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier
                .size(36.dp)
                .align(Alignment.Center)
            )
          }
          1 -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
              drawRect(
                brush = Brush.linearGradient(
                  colors = listOf(Color(0xFFFFB38A), Color(0xFFF35F5F), Color(0xFF7B3FA3))
                )
              )
              drawCircle(
                color = Color(0xFFFFF2AC),
                radius = size.minDimension / 4.5f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.35f)
              )
              val path1 = Path().apply {
                moveTo(0f, size.height)
                lineTo(size.width * 0.4f, size.height * 0.5f)
                lineTo(size.width * 0.8f, size.height)
                close()
              }
              val path2 = Path().apply {
                moveTo(size.width * 0.3f, size.height)
                lineTo(size.width * 0.75f, size.height * 0.62f)
                lineTo(size.width, size.height)
                close()
              }
              drawPath(path = path1, color = Color(0xFF512A72).copy(alpha = 0.7f))
              drawPath(path = path2, color = Color(0xFF2E1349))
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier
                .size(36.dp)
                .align(Alignment.Center)
            )
          }
          else -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
              drawRect(
                brush = Brush.linearGradient(
                  colors = listOf(Color(0xFFFED8A6), Color(0xFFE28F5E), Color(0xFF9E4B35))
                )
              )
              val pathSteam1 = Path().apply {
                moveTo(size.width * 0.4f, size.height * 0.4f)
                quadraticTo(size.width * 0.45f, size.height * 0.3f, size.width * 0.4f, size.height * 0.2f)
              }
              val pathSteam2 = Path().apply {
                moveTo(size.width * 0.6f, size.height * 0.4f)
                quadraticTo(size.width * 0.55f, size.height * 0.3f, size.width * 0.6f, size.height * 0.2f)
              }
              drawPath(
                path = pathSteam1,
                color = Color.White.copy(alpha = 0.4f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
              )
              drawPath(
                path = pathSteam2,
                color = Color.White.copy(alpha = 0.4f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
              )
              drawCircle(
                color = Color(0xFF421C12),
                radius = size.minDimension / 4.0f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.65f)
              )
            }
            Icon(
              imageVector = Icons.Filled.Restaurant,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier
                .size(36.dp)
                .align(Alignment.Center)
            )
          }
        }

        Box(
          modifier = Modifier
            .padding(6.dp)
            .align(Alignment.BottomStart)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Box(
              modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(Color.Green)
            )
            Text(
              text = "PROVEN",
              color = Color.White,
              fontSize = 7.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = when (questIndex) {
          0 -> "🧘 Breathing • Selfie"
          1 -> "🚶 Sunset Trail • GPS"
          else -> "🍲 Kitchen • Cook Savor"
        },
        color = Color.DarkGray,
        fontWeight = FontWeight.Bold,
        fontSize = 8.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun QuestVerificationDialog(
  questIndex: Int,
  quest: QuestData,
  onDismiss: () -> Unit,
  onComplete: (String) -> Unit
) {
  var hasSnapped by remember { mutableStateOf(false) }
  var descriptionText by remember { mutableStateOf("") }
  var isSimulatingCamera by remember { mutableStateOf(false) }
  var countdown by remember { mutableStateOf(3) }

  LaunchedEffect(isSimulatingCamera) {
    if (isSimulatingCamera) {
      countdown = 3
      while (countdown > 0) {
        kotlinx.coroutines.delay(600)
        countdown--
      }
      hasSnapped = true
      isSimulatingCamera = false
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(horizontal = 8.dp)
        .shadow(8.dp, RoundedCornerShape(28.dp)),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Verify Quest Completion",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = DarkSlate,
            fontSize = 18.sp
          )
        )
        Text(
          text = quest.title,
          style = MaterialTheme.typography.bodyMedium.copy(
            color = AccentBlue,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
          ),
          modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SoftNeutralBackground)
            .border(
              1.5.dp, 
              if (hasSnapped) AccentGreen.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.5f), 
              RoundedCornerShape(16.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          if (isSimulatingCamera) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              CircularProgressIndicator(
                color = AccentBlue,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Simulating viewfinder capture: $countdown...",
                color = MutedGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          } else if (hasSnapped) {
            Box(modifier = Modifier.fillMaxSize()) {
              QuestPolaroidPreview(questIndex = questIndex, modifier = Modifier.fillMaxSize())
              
              IconButton(
                onClick = { hasSnapped = false },
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(8.dp)
                  .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                  .size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = "Retake",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          } else {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier
                .fillMaxSize()
                .clickable { isSimulatingCamera = true }
                .padding(16.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = "Camera",
                tint = AccentBlue,
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Capture Visual Proof",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = DarkSlate,
                  fontSize = 14.sp
                )
              )
              Text(
                text = "Tap to simulate photo taking",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = MutedGray,
                  fontSize = 11.sp
                ),
                modifier = Modifier.padding(top = 2.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
          value = descriptionText,
          onValueChange = { descriptionText = it },
          label = { Text("Offline Habit Description", fontSize = 12.sp) },
          placeholder = { Text("What did you do offline? (e.g., Cooking with Mum)", fontSize = 12.sp) },
          singleLine = false,
          maxLines = 3,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.8f),
            focusedLabelColor = AccentBlue
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("quest_verification_desc_input")
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel", color = MutedGray)
          }
          Button(
            onClick = { 
              if (hasSnapped && descriptionText.length >= 3) {
                onComplete(descriptionText)
              }
            },
            enabled = hasSnapped && descriptionText.length >= 3,
            colors = ButtonDefaults.buttonColors(
              containerColor = AccentGreen,
              disabledContainerColor = Color.LightGray.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("submit_quest_proof_button")
          ) {
            Text("Verify Quest", color = Color.White)
          }
        }
      }
    }
  }
}

@Composable
fun ReconnectionQuestsView(
  questsChecked: List<Boolean>,
  questDescriptions: List<String>,
  questPhotosSnapped: List<Boolean>,
  onQuestComplete: (Int, String) -> Unit,
  onQuestReset: (Int) -> Unit
) {
  var activeVerificationQuestIndex by remember { mutableStateOf<Int?>(null) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Reconnection Quests",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 24.sp,
          color = DarkSlate
        )
      )
      Text(
        text = "Positive offline habits to lower your burnout score",
        style = MaterialTheme.typography.bodyMedium.copy(
          color = MutedGray,
          fontSize = 14.sp
        )
      )
    }

    item {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        val quests = listOf(
          QuestData(
            "🧘 Solo Box Breathing",
            "3-minute fast focus reset. +15 Mood Score",
            Icons.Filled.Spa,
            AccentBlue,
            "quest_item_0"
          ),
          QuestData(
            "🚶 Evening Family Stroll",
            "20-minute offline neighborhood walk. +20 Harmony",
            Icons.AutoMirrored.Filled.DirectionsWalk,
            AmberBurnout,
            "quest_item_1"
          ),
          QuestData(
            "🍲 Cook Together",
            "Collaborative device-free kitchen takeover. +30 Family Bond",
            Icons.Filled.Restaurant,
            AccentGreen,
            "quest_item_2"
          )
        )

        quests.forEachIndexed { index, quest ->
          val isChecked = questsChecked[index]
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag(quest.tag)
              .shadow(1.dp, RoundedCornerShape(22.dp))
              .clickable { 
                if (isChecked) {
                  onQuestReset(index)
                } else {
                  activeVerificationQuestIndex = index
                }
              }
              .then(
                if (isChecked) Modifier.border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                else Modifier
              ),
            colors = CardDefaults.cardColors(
              containerColor = if (isChecked) AccentGreenSoft.copy(alpha = 0.25f) else Color.White
            ),
            shape = RoundedCornerShape(22.dp)
          ) {
            Column {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(44.dp)
                      .clip(CircleShape)
                      .background(quest.iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = quest.icon,
                      contentDescription = quest.title,
                      tint = quest.iconColor,
                      modifier = Modifier.size(22.dp)
                    )
                  }

                  Column(
                    modifier = Modifier.weight(1f)
                  ) {
                    Text(
                      text = quest.title,
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isChecked) MutedGray else DarkSlate,
                        fontSize = 15.sp,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else null
                      )
                    )
                    Text(
                      text = quest.subtitle,
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = MutedGray,
                        fontSize = 12.sp,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else null
                      )
                    )
                  }
                }

                Checkbox(
                  checked = isChecked,
                  onCheckedChange = { checked ->
                    if (checked) {
                      activeVerificationQuestIndex = index
                    } else {
                      onQuestReset(index)
                    }
                  },
                  colors = CheckboxDefaults.colors(
                    checkedColor = AccentGreen,
                    uncheckedColor = Color.LightGray
                  )
                )
              }

              if (isChecked) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.65f))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                  HorizontalDivider(color = Color.LightGray.copy(alpha = 0.25f))
                  Spacer(modifier = Modifier.height(12.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    QuestPolaroidPreview(
                      questIndex = index,
                      modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shadow(1.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(6.dp))
                          .background(AccentGreenSoft)
                          .padding(horizontal = 8.dp, vertical = 2.dp)
                      ) {
                        Text(
                          text = "VERIFIED PROOF",
                          color = AccentGreen,
                          fontWeight = FontWeight.ExtraBold,
                          fontSize = 9.sp
                        )
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = questDescriptions[index].ifEmpty { "Logged successfully offline." },
                        color = DarkSlate,
                        style = MaterialTheme.typography.bodySmall.copy(
                          fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                          fontSize = 12.5.sp,
                          lineHeight = 16.sp
                        )
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  activeVerificationQuestIndex?.let { index ->
    val quest = when (index) {
      0 -> QuestData(
        "🧘 Solo Box Breathing",
        "3-minute fast focus reset. +15 Mood Score",
        Icons.Filled.Spa,
        AccentBlue,
        "quest_item_0"
      )
      1 -> QuestData(
        "🚶 Evening Family Stroll",
        "20-minute offline neighborhood walk. +20 Harmony",
        Icons.AutoMirrored.Filled.DirectionsWalk,
        AmberBurnout,
        "quest_item_1"
      )
      else -> QuestData(
        "🍲 Cook Together",
        "Collaborative device-free kitchen takeover. +30 Family Bond",
        Icons.Filled.Restaurant,
        AccentGreen,
        "quest_item_2"
      )
    }
    QuestVerificationDialog(
      questIndex = index,
      quest = quest,
      onDismiss = { activeVerificationQuestIndex = null },
      onComplete = { desc ->
        onQuestComplete(index, desc)
        activeVerificationQuestIndex = null
      }
    )
  }
}

// Support Classes
data class MoodOption(val emoji: String, val name: String, val tag: String)
data class QuestData(
  val title: String,
  val subtitle: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val iconColor: Color,
  val tag: String
)

data class RiskConfig(
  val title: String,
  val desc: String,
  val bgColor: Color,
  val borderColor: Color,
  val textColor: Color,
  val dotColor: Color
)

private fun mapMoodToEmoji(mood: String?): String {
  return when (mood) {
    "Tired" -> "😴"
    "Stressed" -> "😟"
    "Okay" -> "🙂"
    "Happy" -> "😄"
    else -> "😴" // Default starting state
  }
}

@Composable
fun MentalResetModal(
  onClose: () -> Unit
) {
  var isInhaling by remember { mutableStateOf(true) }
  val breathingScale = remember { Animatable(0.40f) }

  LaunchedEffect(Unit) {
    while (true) {
      isInhaling = true
      breathingScale.animateTo(
        targetValue = 1.0f,
        animationSpec = tween(4000, easing = LinearOutSlowInEasing)
      )
      isInhaling = false
      breathingScale.animateTo(
        targetValue = 0.40f,
        animationSpec = tween(4000, easing = FastOutSlowInEasing)
      )
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(SoftNeutralBackground)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("reset_modal"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(26.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Header
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 40.dp)
      ) {
        Text(
          text = "Mental Reset Active 🧘",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = DarkSlate,
            fontSize = 24.sp
          )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Focus on the breathing circle to restore neural balance",
          style = MaterialTheme.typography.bodyMedium.copy(
            color = MutedGray,
            fontSize = 14.sp
          ),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
      }

      // Breathing Circle Visualizers
      Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
      ) {
        // Outer pulsing energy waves
        Box(
          modifier = Modifier
            .size(280.dp * breathingScale.value)
            .clip(CircleShape)
            .background(AccentBlueSoft.copy(alpha = 0.3f))
        )
        Box(
          modifier = Modifier
            .size(200.dp * breathingScale.value)
            .clip(CircleShape)
            .background(AccentGreenSoft.copy(alpha = 0.5f))
        )
        // Core breathing node
        Box(
          modifier = Modifier
            .size(130.dp * breathingScale.value)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                colors = listOf(AccentBlue, AccentGreen)
              )
            )
            .shadow(4.dp, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (isInhaling) "Inhale" else "Exhale",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        }
      }

      // Text indicators below the circle
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = if (isInhaling) "Breathe In..." else "Breathe Out...",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            color = if (isInhaling) AccentBlue else AccentGreen,
            fontSize = 24.sp
          )
        )
        Text(
          text = if (isInhaling) "Expand your chest and feel the calm" else "Let go of daily screens and stress",
          style = MaterialTheme.typography.bodySmall.copy(
            color = MutedGray,
            fontSize = 13.sp
          ),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
      }

      // Close Button
      Button(
        onClick = onClose,
        colors = ButtonDefaults.buttonColors(
          containerColor = DarkSlate,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("close_reset_button")
      ) {
        Text(
          text = "Close / End Reset",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      }
    }
  }
}

@Composable
fun WeeklyTrendsChartCard(
  weeklyScores: List<Float>,
  weeklyDays: List<String>
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("suqoon_prefs", Context.MODE_PRIVATE) }
  
  var viewMode by rememberSaveable { mutableStateOf("Weekly") } // "Weekly" or "Monthly"

  // Load weekly scores (with Sunday/today dynamically computed)
  val defaultWeekly = listOf(42f, 68f, 85f, 58f, 35f, 22f)
  val storedWeeklyScores = remember(weeklyScores) {
    val list = mutableListOf<Float>()
    for (i in 0..5) {
      val key = "weekly_score_$i"
      if (!prefs.contains(key)) {
        prefs.edit().putFloat(key, defaultWeekly[i]).apply()
      }
      list.add(prefs.getFloat(key, defaultWeekly[i]))
    }
    // Add today's dynamic score
    list.add(weeklyScores.lastOrNull() ?: 50f)
    list
  }

  // Load monthly scores (with "This Week" dynamically computed from the average of the active week)
  val defaultMonthly = listOf(55f, 48f, 62f, 38f)
  val monthlyDays = listOf("Week 1", "Week 2", "Week 3", "Week 4", "This Week")
  val storedMonthlyScores = remember(storedWeeklyScores) {
    val list = mutableListOf<Float>()
    for (i in 0..3) {
      val key = "monthly_score_$i"
      if (!prefs.contains(key)) {
        prefs.edit().putFloat(key, defaultMonthly[i]).apply()
      }
      list.add(prefs.getFloat(key, defaultMonthly[i]))
    }
    // Dynamic calculate current week average
    val currentWeeklyAverage = storedWeeklyScores.average().toFloat()
    list.add(currentWeeklyAverage)
    list
  }

  // Choose active dataset based on toggle
  val activeScores = if (viewMode == "Weekly") storedWeeklyScores else storedMonthlyScores
  val activeDays = if (viewMode == "Weekly") weeklyDays else monthlyDays
  val maxIndex = activeScores.size - 1

  var selectedIndex by remember(viewMode) { mutableStateOf(maxIndex) }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(
        elevation = 3.dp,
        shape = RoundedCornerShape(32.dp),
        ambientColor = Color.Black.copy(alpha = 0.04f),
        spotColor = Color.Black.copy(alpha = 0.04f)
      )
      .clip(RoundedCornerShape(32.dp))
      .background(Color.White)
      .border(1.dp, Color.White, RoundedCornerShape(32.dp))
      .padding(20.dp)
  ) {
    Column {
      // Header Section with Segmented Toggle Control
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (viewMode == "Weekly") "Weekly Trends" else "Monthly Trends",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = DarkSlate,
              fontSize = 15.sp
            )
          )
          Text(
            text = if (viewMode == "Weekly") 
              "Historical Burnout Risk (last 7 days)" 
            else 
              "Historical Burnout Risk (longitudinal monthly epochs)",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MutedGray,
              fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Beautiful segmented controller pill
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SoftNeutralBackground)
            .padding(2.dp)
            .testTag("trend_view_toggle"),
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          listOf("Weekly", "Monthly").forEach { mode ->
            val isSelected = viewMode == mode
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(if (isSelected) AccentBlue else Color.Transparent)
                .clickable { viewMode = mode }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("toggle_mode_$mode"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = mode,
                color = if (isSelected) Color.White else MutedGray,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
      
      Spacer(modifier = Modifier.height(14.dp))
      
      // Inline Interactive Tooltip Row representing D3 Hovercard
      val activeDay = activeDays.getOrElse(selectedIndex) { "" }
      val activeScore = activeScores.getOrElse(selectedIndex) { 0f }.toInt()
      val (riskText, riskColor, riskBg) = when {
        activeScore >= 70 -> Triple("High Risk", SoftRed, Color(0xFFFFF5F5))
        activeScore >= 40 -> Triple("Moderate Risk", AmberBurnout, Color(0xFFFFFBEB))
        else -> Triple("Low Risk", AccentGreen, Color(0xFFE2F7EA))
      }
      
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(riskBg)
          .padding(vertical = 10.dp, horizontal = 14.dp)
          .testTag("chart_tooltip_row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(riskColor)
          )
          Text(
            text = "$activeDay Status: $activeScore%",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Bold,
              color = DarkSlate,
              fontSize = 13.sp
            )
          )
        }
        
        Text(
          text = riskText.uppercase(),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            color = riskColor,
            fontSize = 10.sp,
            letterSpacing = 1.sp
          )
        )
      }
      
      Spacer(modifier = Modifier.height(16.dp))
      
      // Recharts Canvas layout
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp)
          .pointerInput(activeScores) {
            detectTapGestures { offset ->
              val canvasWidth = size.width
              val paddingLeftRight = 24.dp.toPx()
              val usableWidth = canvasWidth - (paddingLeftRight * 2)
              val slotWidth = usableWidth / maxIndex.toFloat()
              val touchX = offset.x
              val index = ((touchX - paddingLeftRight) / slotWidth + 0.5f).toInt().coerceIn(0, maxIndex)
              selectedIndex = index
            }
          }
          .testTag("burnout_canvas_graph")
      ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val paddingLeftRight = 24.dp.toPx()
        val paddingTopBottom = 16.dp.toPx()
        
        val chartWidth = canvasWidth - (paddingLeftRight * 2)
        val chartHeight = canvasHeight - (paddingTopBottom * 2)
        
        // 1. Dotted horizontal axis lines (0%, 50%, 100%)
        val gridLevels = listOf(0f, 50f, 100f)
        gridLevels.forEach { level ->
          val y = paddingTopBottom + chartHeight - (level / 100f) * chartHeight
          drawLine(
            color = Color.LightGray.copy(alpha = 0.4f),
            start = androidx.compose.ui.geometry.Offset(paddingLeftRight, y),
            end = androidx.compose.ui.geometry.Offset(canvasWidth - paddingLeftRight, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
          )
        }
        
        // 2. Data points offsets
        val slotWidth = chartWidth / maxIndex.toFloat()
        val points = activeScores.mapIndexed { i, score ->
          val x = paddingLeftRight + i * slotWidth
          val y = paddingTopBottom + chartHeight - (score / 100f) * chartHeight
          androidx.compose.ui.geometry.Offset(x, y)
        }
        
        // 3. Draw gradient filled path under the line (Area Stream style)
        if (points.isNotEmpty()) {
          val fillPath = Path().apply {
            moveTo(points.first().x, paddingTopBottom + chartHeight)
            points.forEach { pt ->
              lineTo(pt.x, pt.y)
            }
            lineTo(points.last().x, paddingTopBottom + chartHeight)
            close()
          }
          
          drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
              colors = listOf(
                AccentBlue.copy(alpha = 0.25f),
                Color.Transparent
              ),
              startY = paddingTopBottom,
              endY = paddingTopBottom + chartHeight
            )
          )
          
          // 4. Draw main line stroke
          val strokePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
              lineTo(points[i].x, points[i].y)
            }
          }
          
          drawPath(
            path = strokePath,
            color = AccentBlue,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
          )
        }
        
        // 5. Highlight interactive selection elements (Glow Node with Selector Line)
        if (selectedIndex in 0..maxIndex && points.size > selectedIndex) {
          val activeX = points[selectedIndex].x
          val activeY = points[selectedIndex].y
          
          // Vertical selector dashed line
          drawLine(
            color = AccentBlue.copy(alpha = 0.3f),
            start = androidx.compose.ui.geometry.Offset(activeX, paddingTopBottom),
            end = androidx.compose.ui.geometry.Offset(activeX, paddingTopBottom + chartHeight),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
          )
          
          // Glow node dot overlay
          drawCircle(
            color = AccentBlueSoft,
            radius = 9.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(activeX, activeY)
          )
          drawCircle(
            color = AccentBlue,
            radius = 5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(activeX, activeY)
          )
        }
      }
      
      Spacer(modifier = Modifier.height(10.dp))
      
      // Horizontal labels under axis
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        activeDays.forEachIndexed { i, day ->
          val isSelected = i == selectedIndex
          Text(
            text = day,
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) AccentBlue else MutedGray,
              fontSize = 11.sp
            ),
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .clickable { selectedIndex = i }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
  userName: String,
  onUserNameChange: (String) -> Unit,
  onResetApp: () -> Unit,
  onClose: () -> Unit
) {
  var notificationToggled by rememberSaveable { mutableStateOf(true) }
  var biometricToggled by rememberSaveable { mutableStateOf(false) }
  var showResetPrompts by rememberSaveable { mutableStateOf(true) }
  var isEditingName by remember { mutableStateOf(false) }
  var nameInput by remember { mutableStateOf(userName) }
  val context = LocalContext.current

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = "Profile & Settings",
            fontWeight = FontWeight.Bold,
            color = DarkSlate,
            fontSize = 18.sp
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("account_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = DarkSlate
            )
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = SoftNeutralBackground
        )
      )
    },
    containerColor = SoftNeutralBackground,
    modifier = Modifier
      .fillMaxSize()
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(8.dp))
        // High polish avatar + general stats card
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Large colored Avatar representation
            Box(
              modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    colors = listOf(AccentBlue, AccentGreen)
                  )
                ),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "S",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inline Editable name field
            if (isEditingName) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                OutlinedTextField(
                  value = nameInput,
                  onValueChange = { nameInput = it },
                  label = { Text("Display Name") },
                  singleLine = true,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("name_edit_input"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = MutedGray,
                    focusedLabelColor = AccentBlue
                  )
                )
                Button(
                  onClick = {
                    if (nameInput.isNotBlank()) {
                      onUserNameChange(nameInput)
                    }
                    isEditingName = false
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("save_name_button")
                ) {
                  Text("Save")
                }
              }
            } else {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = userName,
                  style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                  )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                  onClick = {
                    nameInput = userName
                    isEditingName = true
                  },
                  modifier = Modifier.size(24.dp).testTag("edit_name_icon_button")
                ) {
                  Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit Name",
                    tint = AccentBlue,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }

            Text(
              text = "praneetnrana@gmail.com",
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MutedGray,
                fontSize = 14.sp
              )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status label
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AccentGreenSoft)
                .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AccentGreen)
                )
                Text(
                  text = "Connected & Active",
                  color = AccentGreen,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                )
              }
            }
          }
        }
      }

      // App Settings / Configurations
      item {
        Text(
          text = "App Settings",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = DarkSlate,
            fontSize = 15.sp
          ),
          modifier = Modifier.padding(start = 4.dp)
        )
      }

      // Notification settings
      item {
        SettingToggleRow(
          title = "Weekly Balance Notifications",
          subtitle = "Receive alert reminders every Sunday for digital balance summary.",
          icon = Icons.Filled.Notifications,
          checked = notificationToggled,
          onCheckedChange = { notificationToggled = it },
          tag = "notification_toggle"
        )
      }

      // Auto Mental reset prompt during high risk
      item {
        SettingToggleRow(
          title = "Burnout Risk Auto Reset",
          subtitle = "When High Burnout indicators are reached, slide up the Breath Meditation circle.",
          icon = Icons.Filled.Spa,
          checked = showResetPrompts,
          onCheckedChange = { showResetPrompts = it },
          tag = "reset_prompt_toggle"
        )
      }

      // Biometric Locking
      item {
        SettingToggleRow(
          title = "Enable Biometric Unlock",
          subtitle = "Require Face ID or Fingerprint authentication to view family state index.",
          icon = Icons.Filled.Lock,
          checked = biometricToggled,
          onCheckedChange = { biometricToggled = it },
          tag = "biometric_toggle"
        )
      }

      // General Profile actions
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text(
              text = "Device Connections",
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = DarkSlate
              )
            )
            DeviceRow(deviceName = "Mobile Device (This Phone)", active = true)
            DeviceRow(deviceName = "Family Smart Hub", active = false)
            DeviceRow(deviceName = "Primary Student Laptop", active = true)
          }
        }
      }

      // Account Actions (Log Out & Hard Reset App)
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("account_actions_card")
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Text(
              text = "Account Actions",
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = DarkSlate
              )
            )

            // Log Out Button
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Button(
                onClick = {
                  Toast.makeText(
                    context,
                    "log out part is incomplete.....in works!!!",
                    Toast.LENGTH_LONG
                  ).show()
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color.LightGray.copy(alpha = 0.2f),
                  contentColor = DarkSlate
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("log_out_button")
              ) {
                Row(
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(vertical = 4.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Log Out",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                }
              }
              Text(
                text = "log out part is incomplete.....in works!!!",
                color = MutedGray,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                modifier = Modifier.padding(start = 4.dp)
              )
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.25f))

            // Hard Reset Button
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Button(
                onClick = {
                  onResetApp()
                  Toast.makeText(
                    context,
                    "App state hard reset successful!",
                    Toast.LENGTH_SHORT
                  ).show()
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = SoftRed,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("hard_reset_button")
              ) {
                Row(
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(vertical = 4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Hard Reset App State",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                }
              }
              Text(
                text = "Permanently deletes all screen logs, targets, and completed reconnection quests to re-initialize.",
                color = MutedGray,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  lineHeight = 15.sp
                ),
                modifier = Modifier.padding(start = 4.dp)
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun SettingToggleRow(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  tag: String
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AccentBlueSoft),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(20.dp)
          )
        }
        Column {
          Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Bold,
              color = DarkSlate
            )
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
              color = MutedGray,
              fontSize = 11.sp
            )
          )
        }
      }
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
          checkedThumbColor = Color.White,
          checkedTrackColor = AccentBlue,
          uncheckedThumbColor = MutedGray,
          uncheckedTrackColor = Color.Transparent
        ),
        modifier = Modifier.testTag(tag)
      )
    }
  }
}

@Composable
fun ManualScreenTimeLogCard(
  screenTime: Float,
  onScreenTimeChange: (Float) -> Unit,
  screenTimeGoal: Float,
  onScreenTimeGoalChange: (Float) -> Unit
) {
  var manualInputText by remember { mutableStateOf("") }
  var goalInputText by remember { mutableStateOf(screenTimeGoal.toString()) }
  var isEditingGoal by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var hasShownLimitExceededToast by rememberSaveable { mutableStateOf(false) }
  val context = LocalContext.current

  LaunchedEffect(screenTime, screenTimeGoal) {
    if (screenTime > screenTimeGoal) {
      if (!hasShownLimitExceededToast) {
        Toast.makeText(
          context,
          "⚠️ Daily Screen Time limit exceeded! (${"%.1f".format(screenTime)}h / ${"%.1f".format(screenTimeGoal)}h goal)",
          Toast.LENGTH_LONG
        ).show()
        hasShownLimitExceededToast = true
      }
    } else {
      hasShownLimitExceededToast = false
    }
  }

  // Update localized text if external changes occur
  LaunchedEffect(screenTimeGoal) {
    if (!isEditingGoal) {
      goalInputText = screenTimeGoal.toString()
    }
  }

  Card(
    shape = RoundedCornerShape(32.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("screen_time_log_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      // Card Title Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Track Daily Screentime",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = DarkSlate,
              fontSize = 16.sp
            )
          )
          Text(
            text = "Assess hours logged against your personal target limit",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MutedGray,
              fontSize = 12.sp
            )
          )
        }
        
        // Settings or Edit button to tweak personal goal limit
        TextButton(
          onClick = { 
            if (isEditingGoal) {
              // Save goal
              val parsedGoal = goalInputText.toFloatOrNull()
              if (parsedGoal != null && parsedGoal in 0.5f..24.0f) {
                onScreenTimeGoalChange(parsedGoal)
                errorMessage = null
                isEditingGoal = false
              } else {
                errorMessage = "Enter valid goal (0.5 to 24)"
              }
            } else {
              isEditingGoal = true
            }
          },
          colors = ButtonDefaults.textButtonColors(contentColor = AccentBlue),
          modifier = Modifier.testTag("toggle_edit_goal_button")
        ) {
          Text(
            text = if (isEditingGoal) "Set Goal" else "Edit Goal",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = AccentBlue,
              fontSize = 13.sp
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (screenTime > screenTimeGoal) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SoftRed.copy(alpha = 0.08f)),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SoftRed.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("limit_exceeded_visual_alert")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SoftRed.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Visual alert warning",
                tint = SoftRed,
                modifier = Modifier.size(20.dp)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Screen Time Limit Exceeded!",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = SoftRed
                )
              )
              Text(
                text = "You've exceeded your daily limit by ${"%.1f".format(screenTime - screenTimeGoal)} hrs. Step away and reconnect with family!",
                color = MutedGray,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.5.sp,
                  lineHeight = 15.sp
                )
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Goal Input Fields vs Quick summary description
      if (isEditingGoal) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = goalInputText,
            onValueChange = { goalInputText = it },
            label = { Text("Daily Limit Goal (hrs)", fontSize = 11.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AccentBlue,
              unfocusedBorderColor = Color.LightGray,
              focusedLabelColor = AccentBlue
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("goal_hours_input")
          )
          
          IconButton(
            onClick = { isEditingGoal = false },
            modifier = Modifier.testTag("cancel_edit_goal_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Cancel",
              tint = MutedGray
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      // Manual screen hours entry flow
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Quick adjust Decrement action
        IconButton(
          onClick = {
            val decremented = (screenTime - 0.5f).coerceAtLeast(0.0f)
            onScreenTimeChange(decremented)
            errorMessage = null
          },
          modifier = Modifier
            .background(SoftNeutralBackground, CircleShape)
            .size(40.dp)
            .testTag("decrement_hours_button")
        ) {
          Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = "Subtract 30 minutes",
            tint = DarkSlate
          )
        }

        // Direct Text input entry for specific logs
        OutlinedTextField(
          value = manualInputText,
          onValueChange = { 
            manualInputText = it
            errorMessage = null 
          },
          placeholder = { Text("${screenTime} hrs", color = MutedGray, fontSize = 13.sp) },
          label = { Text("Log Screentime (hrs)", fontSize = 11.sp) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.8f),
            focusedLabelColor = AccentBlue
          ),
          modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .testTag("manual_hours_input")
        )

        // Quick adjust Increment action
        IconButton(
          onClick = {
            val incremented = (screenTime + 0.5f).coerceAtMost(24.0f)
            onScreenTimeChange(incremented)
            errorMessage = null
          },
          modifier = Modifier
            .background(SoftNeutralBackground, CircleShape)
            .size(40.dp)
            .testTag("increment_hours_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add 30 minutes",
            tint = DarkSlate
          )
        }
      }

      // Action logging triggers
      if (manualInputText.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = {
            val parsedLog = manualInputText.toFloatOrNull()
            if (parsedLog != null && parsedLog in 0.0f..24.0f) {
              onScreenTimeChange(parsedLog)
              manualInputText = ""
              errorMessage = null
            } else {
              errorMessage = "Please enter valid hours (0 - 24)"
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("save_screen_time_button")
        ) {
          Text(
            text = "Over-write Custom Hours",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.White
          )
        }
      }

      // Display warning error notifications if present
      errorMessage?.let { error ->
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = error,
          color = SoftRed,
          style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          ),
          modifier = Modifier.padding(horizontal = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Progress Tracker Section
      val percentage = if (screenTimeGoal > 0f) (screenTime / screenTimeGoal) else 0f
      val clampedProgress = percentage.coerceIn(0f, 1f)

      // CSS-style smooth animation transition for the progress bar
      val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(
          durationMillis = 800,
          easing = FastOutSlowInEasing
        ),
        label = "screen_time_progress_animation"
      )
      
      val isOverGoal = screenTime > screenTimeGoal
      val progressColor = if (isOverGoal) SoftRed else AccentBlue
      val progressBg = if (isOverGoal) SoftRed.copy(alpha = 0.15f) else AccentBlueSoft

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(
            imageVector = if (isOverGoal) Icons.Default.Warning else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = progressColor,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = if (isOverGoal) "Goal Exceeded" else "Screentime Status",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Bold,
              color = DarkSlate,
              fontSize = 13.sp
            )
          )
        }
        Text(
          text = "${(percentage * 100).toInt()}%",
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            color = progressColor,
            fontSize = 13.sp
          )
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Custom linear indicator progress bar
      LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
          .clip(RoundedCornerShape(10.dp))
          .testTag("screen_time_progress_bar"),
        color = progressColor,
        trackColor = progressBg
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Adaptive instructional footer text
      val remainingHours = screenTimeGoal - screenTime
      val descriptiveSubtext = if (isOverGoal) {
        "You have logged ${"%.1f".format(screenTime)} hours, exceeding your daily target of ${"%.1f".format(screenTimeGoal)} hours by ${"%.1f".format(-remainingHours)} hours. Consider stepping away for a dynamic screen break!"
      } else {
        "Logged ${"%.1f".format(screenTime)}h / ${"%.1f".format(screenTimeGoal)}h goal. You have ${"%.1f".format(remainingHours)} hours of balance left today to stay offline balanced."
      }

      Text(
        text = descriptiveSubtext,
        color = if (isOverGoal) DarkSlate else MutedGray,
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 11.5.sp,
          lineHeight = 16.sp
        ),
        modifier = Modifier.padding(horizontal = 2.dp)
      )
    }
  }
}

@Composable
fun DeviceRow(
  deviceName: String,
  active: Boolean
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = deviceName,
      style = MaterialTheme.typography.bodyMedium.copy(
        color = DarkSlate,
        fontWeight = FontWeight.Medium
      )
    )
    Text(
      text = if (active) "ACTIVE" else "OFFLINE",
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        color = if (active) AccentGreen else MutedGray,
        letterSpacing = 0.5.sp
      )
    )
  }
}

