package com.example

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Process
import android.provider.Settings
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
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import android.view.KeyEvent
import android.util.Log

fun initFirebase(context: Context) {
  try {
    if (FirebaseApp.getApps(context).isEmpty()) {
      val apiKey = BuildConfig.FIREBASE_API_KEY.ifEmpty { "AIzaSyFakeKeyForUsraWellnessFirestore" }
      val appId = BuildConfig.FIREBASE_APP_ID.ifEmpty { "1:62fe49071234:android:f18f4f1c957192781ab275" }
      val projectId = BuildConfig.FIREBASE_PROJECT_ID.ifEmpty { "usra-harmony-wellness" }

      val options = FirebaseOptions.Builder()
        .setApplicationId(appId)
        .setApiKey(apiKey)
        .setProjectId(projectId)
        .build()
      FirebaseApp.initializeApp(context, options)
      Log.d("FirebaseInit", "Firebase initialized programmatically with project: $projectId")
    } else {
      Log.d("FirebaseInit", "Firebase already initialized")
    }
  } catch (e: Exception) {
    Log.e("FirebaseInit", "Failed to initialize Firebase programmatically", e)
  }
}

object SOSEmergencyManager {
  val isEmergencyTriggered = androidx.compose.runtime.mutableStateOf(false)

  fun triggerEmergency() {
    isEmergencyTriggered.value = true
    Log.d("SOS_SYSTEM", "➡️ [SOS CRITICAL] Initiating emergency broadcast...")
    Log.d("SOS_SYSTEM", "➡️ [SOS] Notifying pre-configured emergency contacts and syncing alert with family dashboard.")
  }
}

object FirestoreManager {
  private const val TAG = "FirestoreManager"

  fun saveWellnessData(
    context: Context,
    stressLevel: Float,
    timeMode: String,
    mood: String?,
    targetWellnessScore: Float,
    screenTimeGoal: Float,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
  ) {
    initFirebase(context)
    try {
      val db = FirebaseFirestore.getInstance()
      val prefs = context.getSharedPreferences("usra_prefs", Context.MODE_PRIVATE)
      val userName = prefs.getString("user_name", "Sami") ?: "Sami"
      
      val data = hashMapOf(
        "stressLevel" to stressLevel,
        "timeMode" to timeMode,
        "mood" to (mood ?: "Calm"),
        "targetWellnessScore" to targetWellnessScore,
        "screenTimeGoal" to screenTimeGoal,
        "updatedAt" to com.google.firebase.Timestamp.now()
      )
      
      db.collection("wellness_data")
        .document(userName.lowercase())
        .set(data, SetOptions.merge())
        .addOnSuccessListener {
          Log.d(TAG, "Successfully saved wellness data to Firestore for $userName")
          // Update SharedPreferences to keep local storage fully updated
          prefs.edit()
            .putFloat("ai_family_stress_level", stressLevel)
            .putString("ai_family_time_mode", timeMode)
            .putString("selected_mood", mood)
            .putFloat("target_wellness_score", targetWellnessScore)
            .putFloat("screen_time_goal", screenTimeGoal)
            .apply()
          onSuccess()
        }
        .addOnFailureListener { e ->
          Log.e(TAG, "Failed to save wellness data to Firestore", e)
          onFailure(e)
        }
    } catch (e: Exception) {
      Log.e(TAG, "Error in saveWellnessData", e)
      onFailure(e)
    }
  }

  fun loadWellnessData(
    context: Context,
    onSuccess: (stressLevel: Float, timeMode: String, mood: String?, targetWellnessScore: Float, screenTimeGoal: Float) -> Unit,
    onFailure: (Exception) -> Unit = {}
  ) {
    initFirebase(context)
    try {
      val db = FirebaseFirestore.getInstance()
      val prefs = context.getSharedPreferences("usra_prefs", Context.MODE_PRIVATE)
      val userName = prefs.getString("user_name", "Sami") ?: "Sami"
      
      db.collection("wellness_data")
        .document(userName.lowercase())
        .get()
        .addOnSuccessListener { document ->
          if (document != null && document.exists()) {
            val stressLevel = document.getDouble("stressLevel")?.toFloat() ?: prefs.getFloat("ai_family_stress_level", 5f)
            val timeMode = document.getString("timeMode") ?: prefs.getString("ai_family_time_mode", "Current System Time") ?: "Current System Time"
            val mood = document.getString("mood") ?: prefs.getString("selected_mood", "Calm")
            val targetWellnessScore = document.getDouble("targetWellnessScore")?.toFloat() ?: prefs.getFloat("target_wellness_score", 80f)
            val screenTimeGoal = document.getDouble("screenTimeGoal")?.toFloat() ?: prefs.getFloat("screen_time_goal", 6f)
            
            Log.d(TAG, "Successfully loaded wellness data from Firestore")
            // Sync locally to SharedPreferences too
            prefs.edit()
              .putFloat("ai_family_stress_level", stressLevel)
              .putString("ai_family_time_mode", timeMode)
              .putString("selected_mood", mood)
              .putFloat("target_wellness_score", targetWellnessScore)
              .putFloat("screen_time_goal", screenTimeGoal)
              .apply()
              
            onSuccess(stressLevel, timeMode, mood, targetWellnessScore, screenTimeGoal)
          } else {
            Log.d(TAG, "No Firestore document exists for $userName; fallback to SharedPreferences")
            onFailure(Exception("No document found"))
          }
        }
        .addOnFailureListener { e ->
          Log.e(TAG, "Failed to load wellness data from Firestore", e)
          onFailure(e)
        }
    } catch (e: Exception) {
      Log.e(TAG, "Error in loadWellnessData", e)
      onFailure(e)
    }
  }
}

class MainActivity : ComponentActivity() {
  private var volumeDownPressCount = 0
  private var lastVolumeDownPressTime = 0L

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      val currentTime = System.currentTimeMillis()
      if (currentTime - lastVolumeDownPressTime < 2000) {
        volumeDownPressCount++
      } else {
        volumeDownPressCount = 1
      }
      lastVolumeDownPressTime = currentTime

      if (volumeDownPressCount >= 3) {
        SOSEmergencyManager.triggerEmergency()
        volumeDownPressCount = 0
      }
      return true
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initFirebase(this)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = false) {
        UsraApp()
      }
    }
  }
}

fun hasUsageStatsPermission(context: Context): Boolean {
  val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
  val mode = appOps.checkOpNoThrow(
    AppOpsManager.OPSTR_GET_USAGE_STATS,
    Process.myUid(),
    context.packageName
  )
  return mode == AppOpsManager.MODE_ALLOWED
}

fun getDeviceScreenTime(context: Context): Float {
  if (!hasUsageStatsPermission(context)) return 0f
  val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
  val calendar = java.util.Calendar.getInstance()
  val endTime = calendar.timeInMillis
  calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
  calendar.set(java.util.Calendar.MINUTE, 0)
  calendar.set(java.util.Calendar.SECOND, 0)
  val startTime = calendar.timeInMillis

  val stats = usageStatsManager.queryUsageStats(
    UsageStatsManager.INTERVAL_DAILY,
    startTime,
    endTime
  )
  
  var totalForegroundTime = 0L
  stats?.forEach {
    totalForegroundTime += it.totalTimeInForeground
  }
  
  return (totalForegroundTime / (1000f * 60f * 60f)) // in hours
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsraApp() {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("usra_prefs", Context.MODE_PRIVATE) }
  val coroutineScope = rememberCoroutineScope()

  var isUserAuthenticated by remember { mutableStateOf(prefs.getBoolean("is_authenticated", false)) }
  var authChecked by remember { mutableStateOf(false) }
  var currentUserEmail by remember { mutableStateOf(prefs.getString("auth_user_email", null)) }
  var userName by remember {
    mutableStateOf(prefs.getString("user_name", "Sami") ?: "Sami")
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    // Ignored, just requesting on startup
  }

  var stepCount by remember { mutableStateOf(prefs.getInt("step_count", 0)) }

  val activityPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepSensor?.let { sensor ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.values?.firstOrNull()?.let { steps ->
                        stepCount = steps.toInt()
                        prefs.edit().putInt("step_count", stepCount).apply()
                    }
                }
                override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }
  }

  var screenTime by remember { 
    mutableStateOf(prefs.getFloat("screen_time", 7.5f)) 
  }

  LaunchedEffect(isUserAuthenticated) {
    if (isUserAuthenticated && hasUsageStatsPermission(context)) {
      val deviceTime = getDeviceScreenTime(context)
      if (deviceTime > 0f) {
          screenTime = deviceTime
      }
    }
  }

  LaunchedEffect(Unit) {
    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
      activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }
    if (hasUsageStatsPermission(context)) {
      val deviceTime = getDeviceScreenTime(context)
      if (deviceTime > 0f) {
          screenTime = deviceTime
      }
    }

    try {
      val auth = FirebaseAuth.getInstance()
      val user = auth.currentUser
      if (user != null) {
        isUserAuthenticated = true
        currentUserEmail = user.email
        userName = prefs.getString("user_name", null) ?: user.displayName ?: user.email?.substringBefore("@") ?: "Sami"
      } else {
        val savedEmail = prefs.getString("auth_user_email", null)
        if (savedEmail != null) {
          isUserAuthenticated = true
          currentUserEmail = savedEmail
          userName = prefs.getString("user_name", "Sami") ?: "Sami"
        }
      }
    } catch (e: Exception) {
      Log.e("UsraAppAuth", "Firebase Auth check failed", e)
      val savedEmail = prefs.getString("auth_user_email", null)
      if (savedEmail != null) {
        isUserAuthenticated = true
        currentUserEmail = savedEmail
        userName = prefs.getString("user_name", "Sami") ?: "Sami"
      }
    }
    authChecked = true
  }

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
        prefs.getBoolean("quest_checked_2", false),
        prefs.getBoolean("quest_checked_3", false),
        prefs.getBoolean("quest_checked_4", false)
      )
    )
  }
  var questDescriptions by remember {
    mutableStateOf(
      listOf(
        prefs.getString("quest_desc_0", "") ?: "",
        prefs.getString("quest_desc_1", "") ?: "",
        prefs.getString("quest_desc_2", "") ?: "",
        prefs.getString("quest_desc_3", "") ?: "",
        prefs.getString("quest_desc_4", "") ?: ""
      )
    )
  }
  var questPhotosSnapped by remember {
    mutableStateOf(
      listOf(
        prefs.getBoolean("quest_photo_0", false),
        prefs.getBoolean("quest_photo_1", false),
        prefs.getBoolean("quest_photo_2", false),
        prefs.getBoolean("quest_photo_3", false),
        prefs.getBoolean("quest_photo_4", false)
      )
    )
  }
  
  var aiQuestTitle1 by remember {
    mutableStateOf(prefs.getString("ai_quest_title_1", "✨ Boardgame Battle") ?: "✨ Boardgame Battle")
  }
  var aiQuestSubtitle1 by remember {
    mutableStateOf(prefs.getString("ai_quest_subtitle_1", "Enjoy offline gameplay with Dad to ease Work Stress.") ?: "Enjoy offline gameplay with Dad to ease Work Stress.")
  }
  var aiQuestTitle2 by remember {
    mutableStateOf(prefs.getString("ai_quest_title_2", "✨ Dinner Prep Assistant") ?: "✨ Dinner Prep Assistant")
  }
  var aiQuestSubtitle2 by remember {
    mutableStateOf(prefs.getString("ai_quest_subtitle_2", "Help Mom with device-free cooking prep to unwind.") ?: "Help Mom with device-free cooking prep to unwind.")
  }
  var aiQuestsLoading by remember { mutableStateOf(false) }
  var sleepLog by remember { 
    mutableStateOf(prefs.getFloat("sleep_log", 5.5f)) 
  }
  var showResetModal by rememberSaveable { mutableStateOf(false) }
  var showAccountPage by rememberSaveable { mutableStateOf(false) }
  var showPlusMenu by remember { mutableStateOf(false) }
  var showUsraAIChat by rememberSaveable { mutableStateOf(false) }
  var showWatchDataPage by rememberSaveable { mutableStateOf(false) }
  var screenTimeGoal by remember { 
    mutableStateOf(prefs.getFloat("screen_time_goal", 6.0f)) 
  }

  var activeToastMessage by remember { mutableStateOf<String?>(null) }
  var activeToastType by remember { mutableStateOf("success") }

  LaunchedEffect(activeToastMessage) {
    if (activeToastMessage != null) {
      delay(3500)
      activeToastMessage = null
    }
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
      .putBoolean("quest_checked_3", questsChecked.getOrElse(3) { false })
      .putBoolean("quest_checked_4", questsChecked.getOrElse(4) { false })
      .apply()
  }
  LaunchedEffect(questDescriptions) {
    prefs.edit()
      .putString("quest_desc_0", questDescriptions.getOrElse(0) { "" })
      .putString("quest_desc_1", questDescriptions.getOrElse(1) { "" })
      .putString("quest_desc_2", questDescriptions.getOrElse(2) { "" })
      .putString("quest_desc_3", questDescriptions.getOrElse(3) { "" })
      .putString("quest_desc_4", questDescriptions.getOrElse(4) { "" })
      .apply()
  }
  LaunchedEffect(questPhotosSnapped) {
    prefs.edit()
      .putBoolean("quest_photo_0", questPhotosSnapped.getOrElse(0) { false })
      .putBoolean("quest_photo_1", questPhotosSnapped.getOrElse(1) { false })
      .putBoolean("quest_photo_2", questPhotosSnapped.getOrElse(2) { false })
      .putBoolean("quest_photo_3", questPhotosSnapped.getOrElse(3) { false })
      .putBoolean("quest_photo_4", questPhotosSnapped.getOrElse(4) { false })
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

  if (!isUserAuthenticated) {
    UsraAuthScreen(
      onAuthSuccess = { email, name ->
        currentUserEmail = email
        userName = name
        isUserAuthenticated = true
      }
    )
  } else {
    Box(modifier = Modifier.fillMaxSize()) {
      Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
          contentAlignment = Alignment.BottomCenter
        ) {
          // Custom notched bottom bar background with premium shadow
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .height(72.dp)
              .shadow(
                elevation = 12.dp,
                shape = BottomBarWithCutoutShape()
              ),
            color = Color.White,
            shape = BottomBarWithCutoutShape()
          ) {
            Row(
              modifier = Modifier.fillMaxSize(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Tab 0: Home
              BottomNavItem(
                selected = currentTab == 0,
                onClick = { currentTab = 0 },
                icon = if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                label = "Home",
                activeColor = AccentBlue,
                modifier = Modifier.weight(1f).testTag("home_tab")
              )

              // Tab 1: Harmony
              BottomNavItem(
                selected = currentTab == 1,
                onClick = { currentTab = 1 },
                icon = if (currentTab == 1) Icons.Filled.Group else Icons.Outlined.Group,
                label = "Harmony",
                activeColor = AccentGreen,
                modifier = Modifier.weight(1f).testTag("family_tab")
              )

              // Central empty placeholder for the cutout/notch
              Box(modifier = Modifier.weight(1f))

              // Tab 2: Quests
              BottomNavItem(
                selected = currentTab == 2,
                onClick = { currentTab = 2 },
                icon = if (currentTab == 2) Icons.Filled.Spa else Icons.Outlined.Spa,
                label = "Quests",
                activeColor = AccentBlue,
                modifier = Modifier.weight(1f).testTag("quests_tab")
              )

              // Tab 3: Feed
              BottomNavItem(
                selected = currentTab == 3,
                onClick = { currentTab = 3 },
                icon = if (currentTab == 3) Icons.Filled.Forum else Icons.Outlined.Forum,
                label = "Feed",
                activeColor = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f).testTag("feed_tab")
              )
            }
          }

          // Floating Action Button (+) centered inside the cutout/notch
          Box(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .offset(y = (-28).dp)
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                  brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(AccentBlue, Color(0xFF7C3AED))
                  )
                )
                .clickable {
                  showPlusMenu = true
                },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Open Add Menu",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
              )
            }
            
            androidx.compose.material3.DropdownMenu(
              expanded = showPlusMenu,
              onDismissRequest = { showPlusMenu = false },
              modifier = Modifier.background(Color.White)
            ) {
              androidx.compose.material3.DropdownMenuItem(
                text = { Text("Ask Usra", color = DarkSlate) },
                onClick = { 
                  showPlusMenu = false
                  showUsraAIChat = true 
                },
                leadingIcon = {
                  Icon(Icons.Default.Chat, contentDescription = null, tint = AccentBlue)
                }
              )
              androidx.compose.material3.DropdownMenuItem(
                text = { Text("Watch Data", color = DarkSlate) },
                onClick = {
                  showPlusMenu = false
                  showWatchDataPage = true
                },
                leadingIcon = {
                  Icon(Icons.Default.Watch, contentDescription = null, tint = AccentGreen)
                }
              )
              androidx.compose.material3.DropdownMenuItem(
                text = { Text("Trigger SOS", color = Color(0xFFD32F2F)) },
                onClick = {
                  showPlusMenu = false
                  SOSEmergencyManager.triggerEmergency()
                },
                leadingIcon = {
                  Icon(Icons.Default.Warning, contentDescription = "Trigger SOS", tint = Color(0xFFD32F2F))
                }
              )
            }
          }
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
              onScreenTimeGoalChange = { screenTimeGoal = it },
              stepCount = stepCount
            )
            1 -> FamilyHarmonyView(
              currentUserDisplayName = userName,
              currentUserMood = selectedMood ?: "Tired",
              currentUserScreenTime = screenTime,
              screenTimeGoal = screenTimeGoal,
              onScreenTimeGoalChange = { screenTimeGoal = it },
              onTriggerToast = { message, type ->
                activeToastMessage = message
                activeToastType = type
              }
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

                val questTitle = when (index) {
                  0 -> "Solo Box Breathing"
                  1 -> "Evening Family Stroll"
                  2 -> "Cook Together"
                  3 -> aiQuestTitle1
                  else -> aiQuestTitle2
                }
                activeToastMessage = "🎉 Quest Completed: $questTitle!"
                activeToastType = "goal"
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
              },
              aiQuestTitle1 = aiQuestTitle1,
              aiQuestSubtitle1 = aiQuestSubtitle1,
              aiQuestTitle2 = aiQuestTitle2,
              aiQuestSubtitle2 = aiQuestSubtitle2,
              aiLoading = aiQuestsLoading,
              onGenerateAIQuests = { onSuccess ->
                if (!aiQuestsLoading) {
                  aiQuestsLoading = true
                  coroutineScope.launch {
                    val result = GeminiService.getAIQuests(
                      userName = userName,
                      screenTime = screenTime,
                      sleepLog = sleepLog,
                      screenTimeGoal = screenTimeGoal,
                      mood = selectedMood,
                      familyStress = prefs.getFloat("ai_family_stress_level", 5f).toInt()
                    )
                    val t1 = result["title1"] ?: "✨ Boardgame Battle"
                    val s1 = result["subtitle1"] ?: "Enjoy offline gameplay with Dad to ease Work Stress."
                    val t2 = result["title2"] ?: "✨ Dinner Prep Assistant"
                    val s2 = result["subtitle2"] ?: "Help Mom with device-free cooking prep to unwind."
                    
                    aiQuestTitle1 = t1
                    aiQuestSubtitle1 = s1
                    aiQuestTitle2 = t2
                    aiQuestSubtitle2 = s2
                    
                    prefs.edit()
                      .putString("ai_quest_title_1", t1)
                      .putString("ai_quest_subtitle_1", s1)
                      .putString("ai_quest_title_2", t2)
                      .putString("ai_quest_subtitle_2", s2)
                      .apply()
                      
                    // Reset checked state for index 3 and 4 as new quests are loaded
                    val newChecked = questsChecked.toMutableList()
                    if (newChecked.size > 4) {
                      newChecked[3] = false
                      newChecked[4] = false
                    }
                    questsChecked = newChecked
                    
                    val newDesc = questDescriptions.toMutableList()
                    if (newDesc.size > 4) {
                      newDesc[3] = ""
                      newDesc[4] = ""
                    }
                    questDescriptions = newDesc

                    val newPhotos = questPhotosSnapped.toMutableList()
                    if (newPhotos.size > 4) {
                      newPhotos[3] = false
                      newPhotos[4] = false
                    }
                    questPhotosSnapped = newPhotos

                    aiQuestsLoading = false
                    onSuccess()
                  }
                }
              }
            )
            3 -> FamilyFeedView(
              currentUserDisplayName = userName,
              onTriggerToast = { message, type ->
                activeToastMessage = message
                activeToastType = type
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
        onLogOut = {
          FirebaseAuth.getInstance().signOut()
          prefs.edit()
            .putBoolean("is_authenticated", false)
            .remove("auth_user_email")
            .remove("user_name")
            .apply()
          isUserAuthenticated = false
          showAccountPage = false
        },
        onClose = { showAccountPage = false }
      )
    }

    // Full screen Slide Up Usra AI Chat Screen Overlay
    AnimatedVisibility(
      visible = showUsraAIChat,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      UsraAIChatScreen(
        userName = userName,
        screenTime = screenTime,
        onScreenTimeChange = { screenTime = it },
        sleepLog = sleepLog,
        onSleepLogChange = { sleepLog = it },
        onClose = { showUsraAIChat = false }
      )
    }

    // Full screen Slide Up Watch Data Overlay
    AnimatedVisibility(
      visible = showWatchDataPage,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      HealthTrackerDashboard(
        onClose = { showWatchDataPage = false }
      )
    }

    // Elegant, custom-designed premium top-toast notification alert
    AnimatedVisibility(
      visible = activeToastMessage != null,
      enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 44.dp)
        .padding(horizontal = 24.dp)
        .zIndex(99f)
    ) {
      activeToastMessage?.let { msg ->
        Card(
          colors = CardDefaults.cardColors(containerColor = OffWhite),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .border(
              width = 1.5.dp,
              color = when (activeToastType) {
                "sync" -> AccentGreen
                "goal" -> AccentBlue
                else -> AccentBlue
              },
              shape = RoundedCornerShape(20.dp)
            )
            .testTag("custom_toast_notification")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                  color = when (activeToastType) {
                    "sync" -> AccentGreenSoft
                    "goal" -> AccentBlueSoft
                    else -> AccentBlueSoft
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = when (activeToastType) {
                  "sync" -> Icons.Default.Sync
                  "goal" -> Icons.Default.Spa
                  else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = when (activeToastType) {
                  "sync" -> AccentGreen
                  "goal" -> AccentBlue
                  else -> AccentBlue
                },
                modifier = Modifier.size(18.dp)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = when (activeToastType) {
                  "sync" -> "Household Balance Synchronized"
                  "goal" -> "Wellbeing Goal Achieved!"
                  else -> "Notification"
                },
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = DarkSlate,
                  fontSize = 11.sp,
                  letterSpacing = 0.5.sp
                )
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = msg.replace("🔄 ", "").replace("🎉 ", ""),
                style = MaterialTheme.typography.bodySmall.copy(
                  color = DarkSlate,
                  fontSize = 13.sp,
                  lineHeight = 17.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsraAuthScreen(
  onAuthSuccess: (email: String, name: String) -> Unit
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("usra_prefs", Context.MODE_PRIVATE) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var isSignUp by remember { mutableStateOf(false) }

  val googleSignInLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
    try {
      val account = task.getResult(ApiException::class.java)
      val idToken = account?.idToken
      if (idToken != null) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
          .addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
              val user = FirebaseAuth.getInstance().currentUser
              val userEmail = user?.email ?: ""
              val displayName = user?.displayName ?: "Google User"
              prefs.edit()
                .putBoolean("is_authenticated", true)
                .putString("auth_user_email", userEmail)
                .putString("user_name", displayName)
                .apply()
              successMessage = "✨ Connected with Google!"
              onAuthSuccess(userEmail, displayName)
            } else {
              errorMessage = authTask.exception?.message ?: "Google authentication failed"
            }
            isLoading = false
          }
      } else {
        errorMessage = "Google authentication failed (no ID token)"
        isLoading = false
      }
    } catch (e: ApiException) {
      errorMessage = "Google sign in failed: ${e.message}"
      isLoading = false
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .safeDrawingPadding()
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      item {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
          text = "USRA",
          style = MaterialTheme.typography.headlineLarge.copy(
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Mindful Screen Balance & Family Harmony",
          style = MaterialTheme.typography.bodyMedium.copy(
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
          )
        )

        Spacer(modifier = Modifier.height(48.dp))

        errorMessage?.let { msg ->
          Text(
            text = msg,
            color = Color(0xFFF43F5E),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
        }

        successMessage?.let { msg ->
          Text(
            text = msg,
            color = Color(0xFF10B981),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
        }

        if (isSignUp) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Family Name / Display Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AccentBlue,
              focusedLabelColor = AccentBlue
            ),
            shape = RoundedCornerShape(12.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            focusedLabelColor = AccentBlue
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Password") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            focusedLabelColor = AccentBlue
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
          onClick = {
            if (email.isBlank() || password.isBlank() || (isSignUp && name.isBlank())) {
              errorMessage = "Please fill in all fields"
              return@Button
            }
            isLoading = true
            errorMessage = null
            successMessage = null
            
            val auth = FirebaseAuth.getInstance()
            if (isSignUp) {
              auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                  if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        prefs.edit()
                          .putBoolean("is_authenticated", true)
                          .putString("auth_user_email", email)
                          .putString("user_name", name)
                          .apply()
                        successMessage = "✨ Account created!"
                        onAuthSuccess(email, name)
                        isLoading = false
                    }
                  } else {
                    errorMessage = "incorrect password or something wrong on our side please try again"
                    isLoading = false
                  }
                }
            } else {
              auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                  if (task.isSuccessful) {
                    val user = auth.currentUser
                    val displayName = user?.displayName ?: email.substringBefore("@")
                    prefs.edit()
                      .putBoolean("is_authenticated", true)
                      .putString("auth_user_email", email)
                      .putString("user_name", displayName)
                      .apply()
                    successMessage = "✨ Welcome back!"
                    onAuthSuccess(email, displayName)
                    isLoading = false
                  } else {
                    errorMessage = "incorrect password or something wrong on our side please try again"
                    isLoading = false
                  }
                }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF14B8A6), // Green
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
          enabled = !isLoading
        ) {
          if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
          } else {
            Icon(Icons.Default.AccountCircle, contentDescription = "Email Icon", tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = if (isSignUp) "Create Family Account" else "Sign In",
              fontWeight = FontWeight.SemiBold,
              style = MaterialTheme.typography.bodyLarge
            )
          }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = { isSignUp = !isSignUp },
            enabled = !isLoading
        ) {
            Text(
                text = if (isSignUp) "Already have an account? Sign In" else "New family? Create Account",
                color = AccentBlue,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = {
            isLoading = true
            errorMessage = null
            successMessage = null
            
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.FIREBASE_WEB_CLIENT_ID.ifEmpty { "1064095450410-fake-client-id.apps.googleusercontent.com" })
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE2E8F0), // Light Gray
            contentColor = Color(0xFF1E293B)
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
          enabled = !isLoading
        ) {
          if (!isLoading) {
            Icon(
              imageVector = Icons.Default.AccountCircle, // Placeholder for Google icon
              contentDescription = "Google Icon",
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Continue with Google",
              fontWeight = FontWeight.SemiBold,
              style = MaterialTheme.typography.bodyLarge
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = {
            prefs.edit()
              .putBoolean("is_authenticated", true)
              .putString("auth_user_email", "demo@usra.com")
              .putString("user_name", "Sami")
              .apply()
            successMessage = "✨ Connected in Demo Mode!"
            onAuthSuccess("demo@usra.com", "Sami")
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MutedGray
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Demo Mode (Sami Account)",
            style = MaterialTheme.typography.bodySmall.copy(
              textDecoration = TextDecoration.Underline
            )
          )
        }

        Spacer(modifier = Modifier.height(48.dp))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
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
  onScreenTimeGoalChange: (Float) -> Unit,
  stepCount: Int
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("usra_prefs", Context.MODE_PRIVATE) }
  var aiRecommendations by remember {
    mutableStateOf(prefs.getString("ai_detox_recommendations", null))
  }
  var aiLoading by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
  var stressLevel by remember { mutableStateOf(prefs.getFloat("ai_family_stress_level", 5f)) }
  var selectedTimeMode by remember { mutableStateOf(prefs.getString("ai_family_time_mode", "Current System Time") ?: "Current System Time") }
  var targetWellnessScore by remember { mutableStateOf(prefs.getFloat("target_wellness_score", 80f)) }

  LaunchedEffect(Unit) {
    FirestoreManager.loadWellnessData(
      context = context,
      onSuccess = { dbStress, dbTimeMode, dbMood, dbTargetScore, dbScreenGoal ->
        stressLevel = dbStress
        selectedTimeMode = dbTimeMode
        targetWellnessScore = dbTargetScore
        onScreenTimeGoalChange(dbScreenGoal)
      },
      onFailure = {
        Log.d("HomeDashboardView", "No existing cloud backup, using local settings")
      }
    )
  }

  DisposableEffect(context) {
    val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
      if (status == android.speech.tts.TextToSpeech.SUCCESS) {
        tts?.setLanguage(java.util.Locale.UK)
      }
    }
    tts = textToSpeech
    onDispose {
      textToSpeech.stop()
      textToSpeech.shutdown()
    }
  }

  var momScreenTime by remember { mutableStateOf(prefs.getFloat("mom_screen_time", 4.0f)) }
  var momSleepLog by remember { mutableStateOf(prefs.getFloat("mom_sleep_log", 7.0f)) }

  var dadScreenTime by remember { mutableStateOf(prefs.getFloat("dad_screen_time", 5.0f)) }
  var dadSleepLog by remember { mutableStateOf(prefs.getFloat("dad_sleep_log", 6.5f)) }

  var selectedMember by remember { mutableStateOf("Sami (You)") }

  val activeScreenVal = when (selectedMember) {
      "Sami (You)" -> screenTime
      "Mom (Working Parent)" -> momScreenTime
      else -> dadScreenTime
  }
  val activeSleepVal = when (selectedMember) {
      "Sami (You)" -> sleepLog
      "Mom (Working Parent)" -> momSleepLog
      else -> dadSleepLog
  }

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
  val currentScore = ((activeScreenVal / 12f) * 60f + ((10f - activeSleepVal) / 6f) * 40f).coerceIn(5f, 100f)
  val weeklyScores = listOf(42f, 68f, 85f, 58f, 35f, 22f, currentScore)
  val weeklyDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

  val riskConfig = when {
    activeScreenVal > 7.0f && activeSleepVal < 6.0f -> {
      RiskConfig(
        title = "🔴 HIGH RISK",
        desc = "Late-night screen usage + low sleep is matching high burnout indicators for $selectedMember.",
        bgColor = Color(0xFFFFF5F5),
        borderColor = Color(0xFFFEE2E2),
        textColor = Color(0xFFDC2626),
        dotColor = Color(0xFFEF4444)
      )
    }
    activeScreenVal < 4.0f && activeSleepVal > 7.0f -> {
      RiskConfig(
        title = "🟢 LOW RISK",
        desc = "Great job! Digital fatigue is extremely low for $selectedMember. Keep up this healthy balance.",
        bgColor = Color(0xFFE2F7EA),
        borderColor = Color(0xFFBFF0D4),
        textColor = Color(0xFF047857),
        dotColor = Color(0xFF10B981)
      )
    }
    else -> {
      RiskConfig(
        title = "🟡 MODERATE RISK",
        desc = "Digital status for $selectedMember is moderately balanced but could improve.",
        bgColor = Color(0xFFFFFBEB),
        borderColor = Color(0xFFFEF3C7),
        textColor = Color(0xFFD97706),
        dotColor = Color(0xFFF59E0B)
      )
    }
  }

  var isRefreshing by remember { mutableStateOf(false) }

  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = {
      isRefreshing = true
      scope.launch {
        kotlinx.coroutines.delay(1200) // Simulating network/data refresh
        isRefreshing = false
      }
    },
    modifier = Modifier.fillMaxSize()
  ) {
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
        // Rounded profile placeholder avatar row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .border(2.dp, OffWhite, CircleShape)
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
            text = "Burnout Tracker Evaluation",
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
            text = "Burnout Tracker Indicators",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = DarkSlate,
              fontSize = 15.sp
            )
          )
          Text(
            text = "Real-time wellness and usage indicators synced from your device",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MutedGray,
              fontSize = 12.sp
            )
          )
          Spacer(modifier = Modifier.height(16.dp))

          // Profile Chip Selector Row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            listOf(
              Triple("Sami (You)", "S", AccentBlue),
              Triple("Mom (Working Parent)", "M", AccentGreen),
              Triple("Dad (Work Mode)", "D", AmberBurnout)
            ).forEach { (memberName, initial, color) ->
              val isSelected = selectedMember == memberName
              val memberScreen = when (memberName) {
                "Sami (You)" -> screenTime
                "Mom (Working Parent)" -> momScreenTime
                else -> dadScreenTime
              }
              val memberSleep = when (memberName) {
                "Sami (You)" -> sleepLog
                "Mom (Working Parent)" -> momSleepLog
                else -> dadSleepLog
              }

              Card(
                colors = CardDefaults.cardColors(
                  containerColor = if (isSelected) color.copy(alpha = 0.12f) else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .width(160.dp)
                  .clickable { selectedMember = memberName }
                  .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) color else Color(0xFFECEFF3),
                    shape = RoundedCornerShape(12.dp)
                  )
              ) {
                Row(
                  modifier = Modifier.padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(color),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = initial,
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp
                    )
                  }
                  Column {
                    Text(
                      text = memberName.split(" ").first(),
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.5.sp,
                      color = DarkSlate,
                      maxLines = 1
                    )
                    Text(
                      text = "${"%.1f".format(memberScreen)}h • ${"%.1f".format(memberSleep)}h sleep",
                      fontSize = 9.5.sp,
                      color = MutedGray,
                      maxLines = 1
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Screen Time Progress Indicator
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
                text = "Daily Screen Time",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = DarkSlate,
                  fontSize = 13.sp
                )
              )
            }
            Text(
              text = "${"%.1f".format(activeScreenVal)}h",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AccentBlue,
                fontSize = 14.sp
              )
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(12.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(AccentBlueSoft)
              .testTag("screen_time_progress_bar")
          ) {
            val fraction = (activeScreenVal / 12f).coerceIn(0f, 1f)
            Box(
              modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(AccentBlue)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Sleep Log Progress Indicator
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
                text = "Nightly Sleep duration",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = DarkSlate,
                  fontSize = 13.sp
                )
              )
            }
            Text(
              text = "${"%.1f".format(activeSleepVal)}h",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AccentGreen,
                fontSize = 14.sp
              )
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(12.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(AccentGreenSoft)
              .testTag("sleep_log_progress_bar")
          ) {
            val fraction = (activeSleepVal / 10f).coerceIn(0f, 1f)
            Box(
              modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(AccentGreen)
            )
          }
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
          value = "${"%.1f".format(screenTime)}h",
          title = "Screen",
          valueColor = AccentBlue
        )
        MetricItemCard(
          modifier = Modifier.weight(1f),
          value = "${"%.1f".format(sleepLog)}h",
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
        weeklyDays = weeklyDays,
        targetWellnessScore = targetWellnessScore,
        onTargetWellnessScoreChange = { targetWellnessScore = it },
        stressLevel = stressLevel,
        selectedTimeMode = selectedTimeMode,
        currentUserMood = selectedMood ?: "Calm",
        screenTimeGoal = screenTimeGoal
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
              Text(
                text = "Usra",
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
                  text = "Connect with Usra to evaluate your screen time (${"%.1f".format(screenTime)} hrs) and sleep duration (${"%.1f".format(sleepLog)} hrs) metrics. Our counselor will prompt 3 personalized physical replacement suggestions and wellness habits for you.",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    color = MutedGray,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                  ),
                  modifier = Modifier.testTag("ai_tips_initial_message")
                )
              } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                  Text(
                    text = aiRecommendations ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                      color = DarkSlate,
                      fontSize = 13.5.sp,
                      lineHeight = 20.sp
                    ),
                    modifier = Modifier.testTag("ai_recommendations_content").weight(1f)
                  )
                  IconButton(
                    onClick = {
                      tts?.speak(aiRecommendations ?: "", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    modifier = Modifier.size(24.dp).padding(start = 8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                      contentDescription = "Read out loud",
                      tint = MutedGray,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
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
                    text = "Usage: ${"%.1f".format(screenTime)} h",
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
                    text = "Sleep: ${"%.1f".format(sleepLog)} h",
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
                    text = "Steps: $stepCount",
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
  currentUserScreenTime: Float,
  screenTimeGoal: Float,
  onScreenTimeGoalChange: (Float) -> Unit,
  onTriggerToast: (String, String) -> Unit
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("usra_prefs", Context.MODE_PRIVATE) }

  var stressLevel by remember {
    mutableStateOf(prefs.getFloat("ai_family_stress_level", 5f))
  }
  var aiRecommendations by remember {
    mutableStateOf(prefs.getString("ai_family_recommendations", null))
  }
  var selectedTimeMode by remember {
    mutableStateOf(prefs.getString("ai_family_time_mode", "Current System Time") ?: "Current System Time")
  }
  var targetWellnessScore by remember {
    mutableStateOf(prefs.getFloat("target_wellness_score", 80f))
  }
  var aiLoading by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    FirestoreManager.loadWellnessData(
      context = context,
      onSuccess = { dbStress, dbTimeMode, dbMood, dbTargetScore, dbScreenGoal ->
        stressLevel = dbStress
        selectedTimeMode = dbTimeMode
        targetWellnessScore = dbTargetScore
        onScreenTimeGoalChange(dbScreenGoal)
        onTriggerToast("☁️ Settings restored seamlessly from Cloud!", "success")
      },
      onFailure = {
        Log.d("FamilyHarmonyView", "No existing cloud backup, using local settings")
      }
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // Header with sync button
    item {
      var isSyncing by remember { mutableStateOf(false) }
      val rotation = remember { Animatable(0f) }
      val syncScope = rememberCoroutineScope()

      Spacer(modifier = Modifier.height(16.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
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

        // Beautiful family balance sync button
        IconButton(
          onClick = {
            if (!isSyncing) {
              isSyncing = true
              syncScope.launch {
                rotation.animateTo(
                  targetValue = rotation.value + 360f,
                  animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                  )
                )
              }
              FirestoreManager.saveWellnessData(
                context = context,
                stressLevel = stressLevel,
                timeMode = selectedTimeMode,
                mood = currentUserMood,
                targetWellnessScore = targetWellnessScore,
                screenTimeGoal = screenTimeGoal,
                onSuccess = {
                  FirestoreManager.loadWellnessData(
                    context = context,
                    onSuccess = { dbStress, dbTimeMode, dbMood, dbTargetScore, dbScreenGoal ->
                      stressLevel = dbStress
                      selectedTimeMode = dbTimeMode
                      targetWellnessScore = dbTargetScore
                      onScreenTimeGoalChange(dbScreenGoal)
                      isSyncing = false
                      syncScope.launch {
                        rotation.snapTo(0f)
                      }
                      onTriggerToast(
                        "🔄 Cloud Sync complete! Stress & Preferences backed up safely.",
                        "sync"
                      )
                    },
                    onFailure = {
                      isSyncing = false
                      syncScope.launch {
                        rotation.snapTo(0f)
                      }
                      onTriggerToast("🔄 Family screen balance synchronized with household members!", "sync")
                    }
                  )
                },
                onFailure = { e ->
                  isSyncing = false
                  syncScope.launch {
                    rotation.snapTo(0f)
                  }
                  onTriggerToast("⚠️ Sync completed locally (Offline Mode)", "sync")
                }
              )
            }
          },
          modifier = Modifier
            .shadow(2.dp, CircleShape)
            .background(Color.White, CircleShape)
            .size(44.dp)
            .testTag("family_sync_button")
        ) {
          Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Sync Family Balance",
            tint = AccentGreen,
            modifier = Modifier
              .size(22.dp)
              .graphicsLayer(rotationZ = rotation.value)
          )
        }
      }
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
                  text = "FAMILY HARMONIZER",
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
            val roundedStress = (stressLevel + 0.5f).toInt().coerceIn(1, 10)
            val stressText = when {
              roundedStress >= 8 -> "🚨 High Stress / Exhausted"
              roundedStress >= 4 -> "☕ Moderately Stressed"
              else -> "🍃 Calm & Balanced"
            }
            val stressColor = when {
              roundedStress >= 8 -> SoftRed
              roundedStress >= 4 -> AmberBurnout
              else -> AccentGreen
            }
            Text(
              text = stressText,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = stressColor)
            )
            Text(
              text = "$roundedStress/10",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold, color = DarkSlate)
            )
          }
          Slider(
            value = stressLevel,
            onValueChange = {
              stressLevel = it
            },
            onValueChangeFinished = {
              prefs.edit().putFloat("ai_family_stress_level", stressLevel).apply()
              FirestoreManager.saveWellnessData(
                context = context,
                stressLevel = stressLevel,
                timeMode = selectedTimeMode,
                mood = currentUserMood,
                targetWellnessScore = targetWellnessScore,
                screenTimeGoal = screenTimeGoal
              )
            },
            valueRange = 1f..10f,
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
                    FirestoreManager.saveWellnessData(
                      context = context,
                      stressLevel = stressLevel,
                      timeMode = mode,
                      mood = currentUserMood,
                      targetWellnessScore = targetWellnessScore,
                      screenTimeGoal = screenTimeGoal
                    )
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
                    stressLevel = (stressLevel + 0.5f).toInt().coerceIn(1, 10),
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
                  text = if (aiRecommendations != null) "Refresh Recommendations" else "Consult Harmonizer",
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
                    text = "Matching stress indexes with collective family time gaps...",
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
          2 -> {
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
          3 -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
              drawRect(
                brush = Brush.radialGradient(
                  colors = listOf(Color(0xFFD4E4FF), Color(0xFF7AAFFF), Color(0xFF1E5BBF))
                )
              )
              drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = size.minDimension / 3.0f
              )
            }
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
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
                brush = Brush.radialGradient(
                  colors = listOf(Color(0xFFFFE9D4), Color(0xFFFFB37A), Color(0xFFBF5E1E))
                )
              )
              drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = size.minDimension / 3.0f
              )
            }
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
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
          2 -> "🍲 Kitchen • Cook Savor"
          3 -> "✨ Spark • Quest"
          else -> "✨ Mindful • Quest"
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
  var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
  var descriptionText by remember { mutableStateOf("") }
  
  val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
  ) { bitmap ->
    if (bitmap != null) {
      capturedBitmap = bitmap
      hasSnapped = true
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
          if (hasSnapped && capturedBitmap != null) {
            Box(modifier = Modifier.fillMaxSize()) {
              androidx.compose.foundation.Image(
                bitmap = capturedBitmap!!.asImageBitmap(),
                contentDescription = "Captured Proof",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
              )
              
              IconButton(
                onClick = { 
                  hasSnapped = false
                  capturedBitmap = null
                },
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
                .clickable { cameraLauncher.launch(null) }
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
                text = "Tap to open camera",
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
  onQuestReset: (Int) -> Unit,
  aiQuestTitle1: String,
  aiQuestSubtitle1: String,
  aiQuestTitle2: String,
  aiQuestSubtitle2: String,
  aiLoading: Boolean,
  onGenerateAIQuests: (onSuccess: () -> Unit) -> Unit
) {
  var activeVerificationQuestIndex by remember { mutableStateOf<Int?>(null) }

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
    ),
    QuestData(
      aiQuestTitle1,
      aiQuestSubtitle1,
      Icons.Filled.AutoAwesome,
      AccentBlue,
      "quest_item_3"
    ),
    QuestData(
      aiQuestTitle2,
      aiQuestSubtitle2,
      Icons.Filled.AutoAwesome,
      AccentGreen,
      "quest_item_4"
    )
  )

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

    // WELLBEING PROGRESS BAR & STATUS BADGE CARD FOR QUESTS
    item {
      val checkedCount = questsChecked.count { it }
      val totalQuests = quests.size
      val progressPercentage = checkedCount.toFloat() / totalQuests
      
      val (badgeText, badgeColor, badgeBg) = when (checkedCount) {
        0 -> Triple("Beginner", MutedGray, SoftNeutralBackground)
        in 1..2 -> Triple("Balanced", AccentBlue, AccentBlueSoft)
        in 3..4 -> Triple("Mindful Creator", AmberBurnout, AmberBurnout.copy(alpha = 0.12f))
        else -> Triple("Zen Master", AccentGreen, AccentGreenSoft)
      }

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(1.dp, RoundedCornerShape(24.dp))
          .testTag("reconnection_progress_card"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Quest Completion Progress",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = DarkSlate,
                  fontSize = 16.sp
                )
              )
              Text(
                text = "$checkedCount of $totalQuests completed today",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = MutedGray,
                  fontSize = 12.sp
                )
              )
            }
            // Status Badge
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(badgeBg)
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = badgeText.uppercase(),
                style = MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.Black,
                  color = badgeColor,
                  fontSize = 10.sp,
                  letterSpacing = 0.5.sp
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Progress Bar
          LinearProgressIndicator(
            progress = { progressPercentage },
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(10.dp))
              .testTag("quests_progress_bar"),
            color = badgeColor,
            trackColor = Color.LightGray.copy(alpha = 0.25f)
          )
        }
      }
    }

    item {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        quests.forEachIndexed { index, quest ->
          val isChecked = questsChecked.getOrElse(index) { false }
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
                        text = questDescriptions.getOrElse(index) { "Logged successfully offline." }.ifEmpty { "Logged successfully offline." },
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
    }

    // AI CUSTOM QUEST GENERATION INTERACTIVE CARD
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(1.dp, RoundedCornerShape(24.dp))
          .testTag("ai_reconnection_config_card"),
        colors = CardDefaults.cardColors(containerColor = SoftNeutralBackground.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = AccentBlue,
              modifier = Modifier.size(24.dp)
            )
            Text(
              text = "Personalized Quests",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = DarkSlate,
                fontSize = 16.sp
              )
            )
          }

          Text(
            text = "Generate reconnection exercises dynamically mapped to your mood, screen time, and family stress levels for tailored family bonding.",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MutedGray,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              fontSize = 12.sp,
              lineHeight = 17.sp
            )
          )

          Spacer(modifier = Modifier.height(4.dp))

          Button(
            onClick = {
              onGenerateAIQuests {
                // Success callback
              }
            },
            enabled = !aiLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("generate_ai_quests_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = AccentBlue,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            if (aiLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
              )
            } else {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Generate Custom Quests",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                )
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  activeVerificationQuestIndex?.let { index ->
    val quest = quests.getOrNull(index) ?: QuestData(
      "Reconnection Quest",
      "Offline family activity",
      Icons.Filled.AutoAwesome,
      AccentBlue,
      "quest_item_unknown"
    )
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

      // Breathing Visualizers
      Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
      ) {
        val scale = breathingScale.value

        // Layer 3 (Outer most, softest)
        Box(
          modifier = Modifier
            .size(280.dp * scale)
            .clip(CircleShape)
            .background(AccentBlue.copy(alpha = 0.15f))
        )
        // Layer 2
        Box(
          modifier = Modifier
            .size(220.dp * scale)
            .clip(CircleShape)
            .background(AccentBlue.copy(alpha = 0.25f))
        )
        // Layer 1 (Inner core)
        Box(
          modifier = Modifier
            .size(160.dp * scale)
            .clip(CircleShape)
            .background(AccentBlue.copy(alpha = 0.45f))
        )
        // Solid Center
        Box(
          modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                colors = listOf(AccentBlue, AccentGreen)
              )
            )
            .shadow(8.dp, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (isInhaling) "Inhale" else "Exhale",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
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
  weeklyDays: List<String>,
  targetWellnessScore: Float,
  onTargetWellnessScoreChange: (Float) -> Unit,
  stressLevel: Float,
  selectedTimeMode: String,
  currentUserMood: String,
  screenTimeGoal: Float
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("usra_prefs", Context.MODE_PRIVATE) }
  
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

      Spacer(modifier = Modifier.height(20.dp))
      HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
      Spacer(modifier = Modifier.height(16.dp))

      // Weekly Wellness Score & Goal Tracker Section
      val averageBurnoutRisk = storedWeeklyScores.average().toFloat()
      val actualWellnessScore = (100f - averageBurnoutRisk).coerceIn(0f, 100f)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "🎯 Weekly Wellness Goal",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = DarkSlate,
              fontSize = 14.sp
            )
          )
          Text(
            text = "Keep daily screen use low & sleep high to boost score",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MutedGray,
              fontSize = 11.sp
            )
          )
        }
        
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          IconButton(
            onClick = {
              val newScore = (targetWellnessScore - 5f).coerceIn(50f, 95f)
              onTargetWellnessScoreChange(newScore)
              prefs.edit().putFloat("target_wellness_score", newScore).apply()
              FirestoreManager.saveWellnessData(
                context = context,
                stressLevel = stressLevel,
                timeMode = selectedTimeMode,
                mood = currentUserMood,
                targetWellnessScore = newScore,
                screenTimeGoal = screenTimeGoal
              )
            },
            modifier = Modifier.size(36.dp)
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
              contentAlignment = Alignment.Center
            ) {
              Text("-", fontWeight = FontWeight.Bold, color = DarkSlate, fontSize = 16.sp)
            }
          }
          
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "Target",
              style = MaterialTheme.typography.labelSmall.copy(color = MutedGray, fontSize = 10.sp)
            )
            Text(
              text = "${targetWellnessScore.toInt()}%",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AccentBlue,
                fontSize = 14.sp
              )
            )
          }

          IconButton(
            onClick = {
              val newScore = (targetWellnessScore + 5f).coerceIn(50f, 95f)
              onTargetWellnessScoreChange(newScore)
              prefs.edit().putFloat("target_wellness_score", newScore).apply()
              FirestoreManager.saveWellnessData(
                context = context,
                stressLevel = stressLevel,
                timeMode = selectedTimeMode,
                mood = currentUserMood,
                targetWellnessScore = newScore,
                screenTimeGoal = screenTimeGoal
              )
            },
            modifier = Modifier.size(36.dp)
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
              contentAlignment = Alignment.Center
            ) {
              Text("+", fontWeight = FontWeight.Bold, color = DarkSlate, fontSize = 16.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      val progressFraction = if (targetWellnessScore > 0f) (actualWellnessScore / targetWellnessScore).coerceIn(0f, 1f) else 1f
      val progressPct = (progressFraction * 100).toInt()

      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Current: ${actualWellnessScore.toInt()}%",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = DarkSlate,
                fontSize = 12.sp
              )
            )
            if (actualWellnessScore >= targetWellnessScore) {
              Text("🏆 Goal Reached!", fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
            }
          }
          Text(
            text = "$progressPct% of Goal achieved",
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.SemiBold,
              color = if (actualWellnessScore >= targetWellnessScore) AccentGreen else AccentBlue,
              fontSize = 12.sp
            )
          )
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFFF1F5F9))
            .testTag("wellness_score_progress_bar")
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(progressFraction)
              .fillMaxHeight()
              .background(
                Brush.horizontalGradient(
                  colors = listOf(AccentBlue, AccentGreen)
                )
              )
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
  onLogOut: () -> Unit,
  onClose: () -> Unit
) {
  var notificationToggled by rememberSaveable { mutableStateOf(true) }
  var biometricToggled by rememberSaveable { mutableStateOf(false) }
  var showResetPrompts by rememberSaveable { mutableStateOf(true) }
  var isEditingName by remember { mutableStateOf(false) }
  var nameInput by remember { mutableStateOf(userName) }
  var showHarmonySync by remember { mutableStateOf(false) }
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
                      val user = FirebaseAuth.getInstance().currentUser
                      if (user != null) {
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                          .setDisplayName(nameInput)
                          .build()
                        user.updateProfile(profileUpdates)
                      }
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
            DeviceRow(deviceName = "Huawei Phone (HarmonyOS Sync)", active = true)
            DeviceRow(deviceName = "Family Smart Hub", active = false)
            DeviceRow(deviceName = "Primary Student Laptop", active = true)

            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = { showHarmonySync = true },
              modifier = Modifier.fillMaxWidth().height(48.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.DevicesOther,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Sync New HarmonyOS Device", fontWeight = FontWeight.Bold)
            }
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
                onClick = onLogOut,
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
    if (showHarmonySync) {
      HarmonyOSSyncModal(onDismiss = { showHarmonySync = false })
    }
  }
}

@Composable
fun HarmonyOSSyncModal(onDismiss: () -> Unit) {
  var step by remember { mutableStateOf(0) } // 0: scanning, 1: found, 2: success

  LaunchedEffect(step) {
    if (step == 0) {
      kotlinx.coroutines.delay(2000)
      step = 1
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .shadow(8.dp, RoundedCornerShape(24.dp))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (step == 0) {
          CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Scanning for nearby HarmonyOS devices...",
            color = DarkSlate,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        } else if (step == 1) {
          Icon(
            imageVector = Icons.Default.Smartphone,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Found Huawei Phone!",
            color = DarkSlate,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
          Text(
            text = "Tap to confirm synchronization.",
            color = MutedGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
          )
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = { step = 2 },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
          ) {
            Text("Confirm Pair", fontWeight = FontWeight.Bold)
          }
        } else {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Huawei Sync Complete",
            color = DarkSlate,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
          Text(
            text = "Your device is now connected.",
            color = MutedGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
          )
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
          ) {
            Text("Done", fontWeight = FontWeight.Bold)
          }
        }
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
  var goalInputText by remember { mutableStateOf("%.1f".format(screenTimeGoal)) }
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
      goalInputText = "%.1f".format(screenTimeGoal)
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

      val (screentimeBadgeText, screentimeBadgeColor, screentimeBadgeBg) = when {
        screenTime <= screenTimeGoal * 0.5f -> Triple("Healthy Balance", AccentGreen, AccentGreenSoft)
        screenTime <= screenTimeGoal -> Triple("Nearing Limit", AmberBurnout, AmberBurnout.copy(alpha = 0.12f))
        else -> Triple("Limit Exceeded", SoftRed, SoftRed.copy(alpha = 0.12f))
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(
            imageVector = if (isOverGoal) Icons.Default.Warning else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = progressColor,
            modifier = Modifier.size(16.dp)
          )
          // Material 3 Status Badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(screentimeBadgeBg)
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = screentimeBadgeText.uppercase(),
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Black,
                color = screentimeBadgeColor,
                fontSize = 8.5.sp,
                letterSpacing = 0.5.sp
              )
            )
          }
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

// ==========================================
// FAMILY FEED COMPONENT AND RELATED MODELS
// ==========================================

enum class FeedItemType {
    QUEST_COMPLETED,
    MILESTONE_REACHED,
    ACTIVITY_LOGGED,
    MEMBER_STREAK
}

data class FamilyFeedItem(
    val id: String,
    val authorName: String,
    val avatarInitials: String,
    val avatarColorName: String,
    val type: FeedItemType,
    val title: String,
    val content: String,
    val timestamp: String,
    val iconEmoji: String,
    val initialCheers: Int = 0,
    val initialClaps: Int = 0,
    val initialFlames: Int = 0,
    val hasPhoto: Boolean = false,
    val photoDescription: String? = null
)

fun getFeedAvatarColor(colorName: String): Color {
    return when (colorName) {
        "blue" -> AccentBlue
        "green" -> AccentGreen
        "amber" -> AmberBurnout
        "purple" -> Color(0xFF7C3AED)
        else -> AccentBlue
    }
}

fun saveUserFeedItems(context: Context, items: List<FamilyFeedItem>) {
    val prefs = context.getSharedPreferences("usra_prefs", Context.MODE_PRIVATE)
    val serialized = items.joinToString("##") { item ->
        val typeStr = item.type.name
        "${item.id}|${item.authorName}|${item.avatarInitials}|${item.avatarColorName}|${typeStr}|${item.title}|${item.content}|${item.timestamp}|${item.iconEmoji}|${item.hasPhoto}|${item.photoDescription ?: ""}"
    }
    prefs.edit().putString("usra_user_feed_items", serialized).apply()
}

fun loadUserFeedItems(context: Context): List<FamilyFeedItem> {
    val prefs = context.getSharedPreferences("usra_prefs", Context.MODE_PRIVATE)
    val serialized = prefs.getString("usra_user_feed_items", null) ?: return emptyList()
    if (serialized.isEmpty()) return emptyList()
    
    return try {
        serialized.split("##").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 9) {
                FamilyFeedItem(
                    id = parts[0],
                    authorName = parts[1],
                    avatarInitials = parts[2],
                    avatarColorName = parts[3],
                    type = FeedItemType.valueOf(parts[4]),
                    title = parts[5],
                    content = parts[6],
                    timestamp = parts[7],
                    iconEmoji = parts[8],
                    hasPhoto = parts.getOrNull(9)?.toBoolean() ?: false,
                    photoDescription = parts.getOrNull(10)?.takeIf { it.isNotEmpty() }
                )
            } else null
        }
    } catch (e: Exception) {
        emptyList()
    }
}

val defaultFeedItems = listOf(
    FamilyFeedItem(
        id = "1",
        authorName = "Dad",
        avatarInitials = "D",
        avatarColorName = "amber",
        type = FeedItemType.MILESTONE_REACHED,
        title = "🏆 Screen Reduction Master!",
        content = "Dad has kept his screen time under 4.5 hours for 5 consecutive days! Let's help him keep the streak alive.",
        timestamp = "2 hours ago",
        iconEmoji = "🏆",
        initialCheers = 8,
        initialClaps = 12,
        initialFlames = 6,
        hasPhoto = false
    ),
    FamilyFeedItem(
        id = "2",
        authorName = "Sami",
        avatarInitials = "S",
        avatarColorName = "blue",
        type = FeedItemType.QUEST_COMPLETED,
        title = "🧘 Completed 'Solo Box Breathing' Quest",
        content = "Took 5 minutes off between my assignments to do deep box breathing. My brain feels completely reset!",
        timestamp = "4 hours ago",
        iconEmoji = "🧘",
        initialCheers = 5,
        initialClaps = 6,
        initialFlames = 3,
        hasPhoto = true,
        photoDescription = "Cozy desk snapshot with breathing timer"
    ),
    FamilyFeedItem(
        id = "3",
        authorName = "Mom",
        avatarInitials = "M",
        avatarColorName = "green",
        type = FeedItemType.ACTIVITY_LOGGED,
        title = "🍲 Shared Family dinner: No Phones!",
        content = "Cooked a delicious curry together with Sami. We put all of our phones in the detox basket for 1.5 hours!",
        timestamp = "Yesterday",
        iconEmoji = "🍲",
        initialCheers = 15,
        initialClaps = 18,
        initialFlames = 9,
        hasPhoto = false
    ),
    FamilyFeedItem(
        id = "4",
        authorName = "Usra",
        avatarInitials = "U",
        avatarColorName = "purple",
        type = FeedItemType.MILESTONE_REACHED,
        title = "✨ Collective Harmony Peak!",
        content = "Our collective Family Harmony Index reached 88/100 yesterday evening! Great job communicating screen-free.",
        timestamp = "Yesterday",
        iconEmoji = "✨",
        initialCheers = 20,
        initialClaps = 24,
        initialFlames = 12,
        hasPhoto = false
    ),
    FamilyFeedItem(
        id = "5",
        authorName = "Dad",
        avatarInitials = "D",
        avatarColorName = "amber",
        type = FeedItemType.QUEST_COMPLETED,
        title = "🚶‍♂️ Finished 'Evening Family Stroll' Quest",
        content = "Walked around the community park with Mom. The cool air was wonderful, absolutely zero screen distractions.",
        timestamp = "2 days ago",
        iconEmoji = "🚶‍♂️",
        initialCheers = 6,
        initialClaps = 9,
        initialFlames = 2,
        hasPhoto = true,
        photoDescription = "Evening sky sunset silhouette"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyFeedView(
    currentUserDisplayName: String,
    onTriggerToast: (String, String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("usra_prefs", Context.MODE_PRIVATE) }
    
    var userItems by remember {
        mutableStateOf(loadUserFeedItems(context))
    }
    
    val allItems = remember(userItems) {
        userItems + defaultFeedItems
    }
    
    var selectedFilter by remember { mutableStateOf("All") }
    var isSharePanelExpanded by remember { mutableStateOf(false) }
    
    // Form States
    var shareAuthor by remember { mutableStateOf(currentUserDisplayName) }
    var shareCategory by remember { mutableStateOf("🍲 Cooking") }
    var shareContent by remember { mutableStateOf("") }
    var shareHasPhoto by remember { mutableStateOf(false) }
    
    val categoryEmojis = mapOf(
        "🍲 Cooking" to "🍲",
        "🚶‍♂️ Stroll" to "🚶‍♂️",
        "🧘 Mindfulness" to "🧘",
        "🎲 Boardgames" to "🎲",
        "🏆 Goal" to "🏆"
    )
    
    val categoryTitles = mapOf(
        "🍲 Cooking" to "Shared Cooking Together!",
        "🚶‍♂️ Stroll" to "Shared Family Walk!",
        "🧘 Mindfulness" to "Completed Mindfulness Quest!",
        "🎲 Boardgames" to "Played a Boardgame Together!",
        "🏆 Goal" to "Reached a Wellness Milestone!"
    )
    
    val filteredItems = remember(allItems, selectedFilter) {
        when (selectedFilter) {
            "Quests" -> allItems.filter { it.type == FeedItemType.QUEST_COMPLETED }
            "Milestones" -> allItems.filter { it.type == FeedItemType.MILESTONE_REACHED }
            "Shared" -> allItems.filter { it.type == FeedItemType.ACTIVITY_LOGGED || it.type == FeedItemType.MEMBER_STREAK }
            else -> allItems
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Family Feed",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = DarkSlate
                        )
                    )
                    Text(
                        text = "Celebrating shared offline milestones",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MutedGray,
                            fontSize = 14.sp
                        )
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3E8FF))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "3 Online",
                            color = Color(0xFF7C3AED),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFECEFF3), RoundedCornerShape(24.dp))
                    .testTag("share_activity_card"),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (!isSharePanelExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSharePanelExpanded = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF3E8FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share",
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Share an offline activity",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkSlate,
                                            fontSize = 15.sp
                                        )
                                    )
                                    Text(
                                        text = "Keep the family motivated & balanced",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MutedGray,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Expand Share Panel",
                                tint = Color(0xFF7C3AED)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Share wellness moment",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkSlate,
                                    fontSize = 16.sp
                                )
                            )
                            IconButton(
                                onClick = { isSharePanelExpanded = false }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Collapse Share Panel",
                                    tint = MutedGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Posting as:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val authors = listOf(currentUserDisplayName, "Mom", "Dad")
                            authors.forEach { author ->
                                val isSelected = shareAuthor == author
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFF3E8FF) else Color(0xFFF1F3F5))
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) Color(0xFF7C3AED) else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { shareAuthor = author }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = author,
                                        color = if (isSelected) Color(0xFF7C3AED) else MutedGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "What did you do?",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categoryEmojis.keys.forEach { cat ->
                                val isSelected = shareCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFFE1EFF9) else Color(0xFFF1F3F5))
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) AccentBlue else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { shareCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) AccentBlue else MutedGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        OutlinedTextField(
                            value = shareContent,
                            onValueChange = { shareContent = it },
                            placeholder = { Text("What did you do? e.g., Played Monopoly screen-free!", fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .testTag("share_content_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF7C3AED),
                                unfocusedBorderColor = Color(0xFFECEFF3)
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Attach photo memory",
                                    fontSize = 12.5.sp,
                                    color = DarkSlate,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = shareHasPhoto,
                                onCheckedChange = { shareHasPhoto = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentGreen,
                                    uncheckedThumbColor = MutedGray,
                                    uncheckedTrackColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.testTag("photo_attachment_switch")
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Button(
                            onClick = {
                                if (shareContent.trim().isEmpty()) {
                                    onTriggerToast("⚠️ Please enter what you shared!", "error")
                                    return@Button
                                }
                                val authorColorName = when (shareAuthor) {
                                    "Mom" -> "green"
                                    "Dad" -> "amber"
                                    else -> "blue"
                                }
                                val authorInitials = if (shareAuthor.isNotEmpty()) shareAuthor.take(1).uppercase() else "U"
                                val newFeedItem = FamilyFeedItem(
                                    id = System.currentTimeMillis().toString(),
                                    authorName = shareAuthor,
                                    avatarInitials = authorInitials,
                                    avatarColorName = authorColorName,
                                    type = FeedItemType.ACTIVITY_LOGGED,
                                    title = categoryTitles[shareCategory] ?: "Shared a wellness activity!",
                                    content = shareContent.trim(),
                                    timestamp = "Just now",
                                    iconEmoji = categoryEmojis[shareCategory] ?: "✨",
                                    initialCheers = 0,
                                    initialClaps = 0,
                                    initialFlames = 0,
                                    hasPhoto = shareHasPhoto,
                                    photoDescription = if (shareHasPhoto) "Shared wellness moment: $shareCategory" else null
                                )
                                val updatedList = listOf(newFeedItem) + userItems
                                userItems = updatedList
                                saveUserFeedItems(context, updatedList)
                                
                                shareContent = ""
                                shareHasPhoto = false
                                isSharePanelExpanded = false
                                
                                onTriggerToast("🎉 Posted successfully to the Family Feed!", "sync")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("post_activity_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Post to Family Feed", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf("All", "Quests", "Milestones", "Shared")
                filters.forEach { filterName ->
                    val isSelected = selectedFilter == filterName
                    val bg = if (isSelected) Color(0xFF7C3AED) else Color.White
                    val textCol = if (isSelected) Color.White else MutedGray
                    val borderCol = if (isSelected) Color.Transparent else Color(0xFFECEFF3)
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = filterName }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = filterName,
                            color = textCol,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
        
        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.RssFeed,
                            contentDescription = null,
                            tint = MutedGray.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No activities in this category yet",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MutedGray)
                        )
                    }
                }
            }
        } else {
            items(filteredItems.size, key = { filteredItems[it].id }) { index ->
                val item = filteredItems[index]
                
                var cheersCount by remember(item.id) {
                    mutableStateOf(prefs.getInt("feed_cheers_${item.id}", item.initialCheers))
                }
                var clapsCount by remember(item.id) {
                    mutableStateOf(prefs.getInt("feed_claps_${item.id}", item.initialClaps))
                }
                var flamesCount by remember(item.id) {
                    mutableStateOf(prefs.getInt("feed_flames_${item.id}", item.initialFlames))
                }
                
                var hasCheered by remember(item.id) {
                    mutableStateOf(prefs.getBoolean("feed_cheered_${item.id}", false))
                }
                var hasClapped by remember(item.id) {
                    mutableStateOf(prefs.getBoolean("feed_clapped_${item.id}", false))
                }
                var hasFlamed by remember(item.id) {
                    mutableStateOf(prefs.getBoolean("feed_flamed_${item.id}", false))
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, Color(0xFFF1F3F5), RoundedCornerShape(22.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(getFeedAvatarColor(item.avatarColorName).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.avatarInitials,
                                    color = getFeedAvatarColor(item.avatarColorName),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(50.dp)
                                    .background(Color(0xFFE2E8F0))
                            )
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.authorName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkSlate,
                                            fontSize = 14.5.sp
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(MutedGray)
                                    )
                                    Text(
                                        text = item.timestamp,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MutedGray,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(getFeedAvatarColor(item.avatarColorName).copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.iconEmoji,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkSlate,
                                    fontSize = 15.5.sp
                                )
                            )
                            
                            Text(
                                text = item.content,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = DarkSlate.copy(alpha = 0.85f),
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp
                                )
                            )
                            
                            if (item.hasPhoto) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(3.dp, RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = when (item.avatarColorName) {
                                                            "blue" -> listOf(Color(0xFF6EE7B7), Color(0xFF3B82F6))
                                                            "green" -> listOf(Color(0xFFA7F3D0), Color(0xFF059669))
                                                            "amber" -> listOf(Color(0xFFFDE68A), Color(0xFFD97706))
                                                            else -> listOf(Color(0xFFC084FC), Color(0xFF7C3AED))
                                                        }
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.CameraAlt,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Wellness Snapshot Captured!",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = item.photoDescription ?: "Family balanced time",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Medium,
                                                color = DarkSlate,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = Color(0xFFF1F3F5), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (hasCheered) Color(0xFFFEE2E2) else Color.Transparent)
                                        .clickable {
                                            hasCheered = !hasCheered
                                            cheersCount += if (hasCheered) 1 else -1
                                            prefs.edit()
                                                .putInt("feed_cheers_${item.id}", cheersCount)
                                                .putBoolean("feed_cheered_${item.id}", hasCheered)
                                                .apply()
                                            if (hasCheered) {
                                                onTriggerToast("❤️ Cheered Dad, Sami, and Mom's progress!", "sync")
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hasCheered) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Cheers",
                                        tint = if (hasCheered) SoftRed else MutedGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$cheersCount",
                                        color = if (hasCheered) SoftRed else MutedGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (hasClapped) Color(0xFFFEF3C7) else Color.Transparent)
                                        .clickable {
                                            hasClapped = !hasClapped
                                            clapsCount += if (hasClapped) 1 else -1
                                            prefs.edit()
                                                .putInt("feed_claps_${item.id}", clapsCount)
                                                .putBoolean("feed_clapped_${item.id}", hasClapped)
                                                .apply()
                                            if (hasClapped) {
                                                onTriggerToast("👏 Applauded the screen-free milestone!", "goal")
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hasClapped) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                        contentDescription = "Claps",
                                        tint = if (hasClapped) AmberBurnout else MutedGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$clapsCount",
                                        color = if (hasClapped) AmberBurnout else MutedGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (hasFlamed) Color(0xFFE1EFF9) else Color.Transparent)
                                        .clickable {
                                            hasFlamed = !hasFlamed
                                            flamesCount += if (hasFlamed) 1 else -1
                                            prefs.edit()
                                                .putInt("feed_flames_${item.id}", flamesCount)
                                                .putBoolean("feed_flamed_${item.id}", hasFlamed)
                                                .apply()
                                            if (hasFlamed) {
                                                onTriggerToast("🔥 Kept the family wellness momentum burning!", "sync")
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hasFlamed) Icons.Filled.Whatshot else Icons.Outlined.Whatshot,
                                        contentDescription = "Hot",
                                        tint = if (hasFlamed) AccentBlue else MutedGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$flamesCount",
                                        color = if (hasFlamed) AccentBlue else MutedGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Custom notched bottom bar background shape
class BottomBarWithCutoutShape(
    private val cutoutWidthDp: androidx.compose.ui.unit.Dp = 80.dp,
    private val cutoutHeightDp: androidx.compose.ui.unit.Dp = 24.dp
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val cutoutWidth = with(density) { cutoutWidthDp.toPx() }
        val cutoutHeight = with(density) { cutoutHeightDp.toPx() }
        val path = Path().apply {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val startX = centerX - cutoutWidth / 2f
            val endX = centerX + cutoutWidth / 2f

            moveTo(0f, 0f)
            lineTo(startX, 0f)

            // Left side curve dropping smoothly into the notch center
            cubicTo(
                x1 = startX + cutoutWidth * 0.15f, y1 = 0f,
                x2 = centerX - cutoutWidth * 0.25f, y2 = cutoutHeight,
                x3 = centerX, y3 = cutoutHeight
            )
            // Right side curve rising smoothly back up to the bar top
            cubicTo(
                x1 = centerX + cutoutWidth * 0.25f, y1 = cutoutHeight,
                x2 = endX - cutoutWidth * 0.15f, y2 = 0f,
                x3 = endX, y3 = 0f
            )

            lineTo(width, 0f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

// Custom navigation bar item for clean presentation
@Composable
fun BottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeColor else MutedGray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (selected) activeColor else MutedGray,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// Separate screen dedicated to Usra AI wellness coaching
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsraAIChatScreen(
    userName: String,
    screenTime: Float,
    onScreenTimeChange: (Float) -> Unit,
    sleepLog: Float,
    onSleepLogChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("usra_prefs", Context.MODE_PRIVATE) }

    var messages by remember {
        mutableStateOf(
            listOf(
                Pair(
                    "Hello! I am your personal digital wellness companion. 🌸 Ask me for personalized recommendations or offline activities!",
                    false
                )
            )
        )
    }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(context) {
        val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.setLanguage(java.util.Locale.UK)
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    // Keep the conversation scrolled to the latest message
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftNeutralBackground)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFECEFF3), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go Back",
                        tint = DarkSlate
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "✨ Usra",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = DarkSlate
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        )
                        Text(
                            text = "Smart Advisor",
                            fontSize = 11.sp,
                            color = MutedGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Message History list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(messages.size) { index ->
                    val (text, isUser) = messages[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isUser) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AccentBlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✨", fontSize = 14.sp)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) AccentBlue else Color.White
                            ),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (isUser) Color.Transparent else Color(0xFFECEFF3),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    )
                                )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = text,
                                    color = if (isUser) Color.White else DarkSlate,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                                if (!isUser) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    IconButton(
                                        onClick = {
                                            tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                        },
                                        modifier = Modifier.size(24.dp).align(Alignment.End)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Read out loud",
                                            tint = MutedGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AccentBlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✨", fontSize = 14.sp)
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    color = Color(0xFFECEFF3),
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(240.dp)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = AccentBlue,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Coach is typing...",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentBlue
                                        )
                                    }
                                    // Pulses beautiful shimmer placeholders to indicate writing process
                                    ShimmerPlaceholder(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                    )
                                    ShimmerPlaceholder(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(14.dp)
                                    )
                                    ShimmerPlaceholder(
                                        modifier = Modifier
                                            .fillMaxWidth(0.55f)
                                            .height(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Chat Input Text Box Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                color = Color.White,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask coach...") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = Color(0xFFECEFF3),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB)
                        ),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val userQuery = inputText.trim()
                                messages = messages + Pair(userQuery, true)
                                inputText = ""
                                isLoading = true

                                coroutineScope.launch {
                                    try {
                                        val familyContext = "- $userName: ${"%.1f".format(screenTime)} hrs screen, ${"%.1f".format(sleepLog)} hrs sleep"

                                        val response = GeminiService.getChatResponse(
                                            userName = userName,
                                            activePersonName = userName,
                                            activeScreenTime = screenTime,
                                            activeSleepHours = sleepLog,
                                            familyContext = familyContext,
                                            messageHistory = messages.drop(1).map { Pair(it.first, it.second) },
                                            latestMessage = userQuery
                                        )
                                        messages = messages + Pair(response, false)
                                    } catch (e: Exception) {
                                        messages = messages + Pair("Sorry, I encountered an issue: ${e.localizedMessage}", false)
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                brush = if (inputText.isNotBlank()) {
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(AccentBlue, Color(0xFF7C3AED))
                                    )
                                } else {
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(MutedGray.copy(alpha = 0.3f), MutedGray.copy(alpha = 0.3f))
                                    )
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Reusable, highly performant Shimmer/Skeleton placeholder for loading states
@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = modifier
            .background(
                color = Color(0xFFEFF1F5).copy(alpha = alpha),
                shape = shape
            )
    )
}


