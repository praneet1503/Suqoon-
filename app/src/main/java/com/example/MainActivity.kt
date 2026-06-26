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
      Log.d("FirebaseInit", "Firebase initialized programmatically")
    }
  } catch (e: Exception) {
    Log.e("FirebaseInit", "Failed to initialize Firebase", e)
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
      val auth = FirebaseAuth.getInstance()
      val userId = auth.currentUser?.uid ?: "anonymous"
      
      val data = hashMapOf(
        "stressLevel" to stressLevel,
        "timeMode" to timeMode,
        "mood" to (mood ?: "Calm"),
        "targetWellnessScore" to targetWellnessScore,
        "screenTimeGoal" to screenTimeGoal,
        "updatedAt" to com.google.firebase.Timestamp.now()
      )
      
      db.collection("wellness_data")
        .document(userId)
        .set(data, SetOptions.merge())
        .addOnSuccessListener {
          Log.d(TAG, "Successfully saved wellness data for $userId")
          onSuccess()
        }
        .addOnFailureListener { e ->
          Log.e(TAG, "Failed to save wellness data", e)
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
      val auth = FirebaseAuth.getInstance()
      val userId = auth.currentUser?.uid ?: return onFailure(Exception("User not authenticated"))
      
      db.collection("wellness_data")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
          if (document != null && document.exists()) {
            val stressLevel = document.getDouble("stressLevel")?.toFloat() ?: 5f
            val timeMode = document.getString("timeMode") ?: "Current System Time"
            val mood = document.getString("mood") ?: "Calm"
            val targetWellnessScore = document.getDouble("targetWellnessScore")?.toFloat() ?: 80f
            val screenTimeGoal = document.getDouble("screenTimeGoal")?.toFloat() ?: 6f
            onSuccess(stressLevel, timeMode, mood, targetWellnessScore, screenTimeGoal)
          } else {
            onFailure(Exception("No document found"))
          }
        }
        .addOnFailureListener { e -> onFailure(e) }
    } catch (e: Exception) {
      onFailure(e)
    }
  }
}

class MainActivity : ComponentActivity() {
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

  var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
  var isUserAuthenticated by remember { mutableStateOf(currentUser != null) }
  var userName by remember {
    mutableStateOf(currentUser?.displayName ?: prefs.getString("user_name", "Sami") ?: "Sami")
  }

  // Auth State Listener
  LaunchedEffect(Unit) {
    FirebaseAuth.getInstance().addAuthStateListener { auth ->
      currentUser = auth.currentUser
      isUserAuthenticated = auth.currentUser != null
      if (auth.currentUser != null) {
        userName = auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@") ?: "Sami"
        prefs.edit().putString("user_name", userName).apply()
      }
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean -> }

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
    mutableStateOf(prefs.getString("ai_quest_title_1", "✨ AI Boardgame Battle") ?: "✨ AI Boardgame Battle")
  }
  var aiQuestSubtitle1 by remember {
    mutableStateOf(prefs.getString("ai_quest_subtitle_1", "Enjoy offline gameplay with Dad to ease Work Stress.") ?: "Enjoy offline gameplay with Dad to ease Work Stress.")
  }
  var aiQuestTitle2 by remember {
    mutableStateOf(prefs.getString("ai_quest_title_2", "✨ AI Dinner Prep Assistant") ?: "✨ AI Dinner Prep Assistant")
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

  // Persist states reactively
  LaunchedEffect(currentTab, selectedMood, questsChecked, screenTime, sleepLog, userName, screenTimeGoal) {
    prefs.edit()
      .putInt("current_tab", currentTab)
      .putString("selected_mood", selectedMood)
      .putFloat("screen_time", screenTime)
      .putFloat("sleep_log", sleepLog)
      .putString("user_name", userName)
      .putFloat("screen_time_goal", screenTimeGoal)
      .apply()
  }

  if (!isUserAuthenticated) {
    UsraAuthScreen()
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
              BottomNavItem(
                selected = currentTab == 0,
                onClick = { currentTab = 0 },
                icon = if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                label = "Home",
                activeColor = AccentBlue,
                modifier = Modifier.weight(1f).testTag("home_tab")
              )
              BottomNavItem(
                selected = currentTab == 1,
                onClick = { currentTab = 1 },
                icon = if (currentTab == 1) Icons.Filled.Group else Icons.Outlined.Group,
                label = "Harmony",
                activeColor = AccentGreen,
                modifier = Modifier.weight(1f).testTag("family_tab")
              )
              Box(modifier = Modifier.weight(1f))
              BottomNavItem(
                selected = currentTab == 2,
                onClick = { currentTab = 2 },
                icon = if (currentTab == 2) Icons.Filled.Spa else Icons.Outlined.Spa,
                label = "Quests",
                activeColor = AccentBlue,
                modifier = Modifier.weight(1f).testTag("quests_tab")
              )
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
                text = { Text("Ask Usra AI", color = DarkSlate) },
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
                activeToastMessage = "🎉 Quest Completed!"
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
                    val result = GeminiService.getAIQuests(userName, screenTime, sleepLog, screenTimeGoal, selectedMood, 5)
                    aiQuestTitle1 = result["title1"] ?: "✨ AI Boardgame Battle"
                    aiQuestSubtitle1 = result["subtitle1"] ?: "Enjoy offline gameplay with Dad to ease Work Stress."
                    aiQuestTitle2 = result["title2"] ?: "✨ AI Dinner Prep Assistant"
                    aiQuestSubtitle2 = result["subtitle2"] ?: "Help Mom with device-free cooking prep to unwind."
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

    AnimatedVisibility(visible = showResetModal, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
      MentalResetModal(onClose = { showResetModal = false })
    }

    AnimatedVisibility(visible = showAccountPage, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
      AccountScreen(
        userName = userName,
        onUserNameChange = { userName = it },
        onResetApp = { /* Reset app state logic */ },
        onLogOut = {
          FirebaseAuth.getInstance().signOut()
          showAccountPage = false
        },
        onClose = { showAccountPage = false }
      )
    }

    AnimatedVisibility(visible = showUsraAIChat, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
      UsraAIChatScreen(userName, screenTime, { screenTime = it }, sleepLog, { sleepLog = it }, { showUsraAIChat = false })
    }

    AnimatedVisibility(visible = showWatchDataPage, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
      HealthTrackerDashboard(onClose = { showWatchDataPage = false })
    }

    AnimatedVisibility(visible = activeToastMessage != null, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut(),
      modifier = Modifier.align(Alignment.TopCenter).padding(top = 44.dp).padding(horizontal = 24.dp).zIndex(99f)
    ) {
      activeToastMessage?.let { msg ->
        Card(
          colors = CardDefaults.cardColors(containerColor = OffWhite),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(20.dp)).border(1.5.dp, AccentBlue, RoundedCornerShape(20.dp))
        ) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentBlue)
            Text(text = msg, style = MaterialTheme.typography.bodySmall, color = DarkSlate)
          }
        }
      }
    }
  }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsraAuthScreen() {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = true
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (!authTask.isSuccessful) {
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

    Box(modifier = Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text(text = "USRA AI", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = DarkSlate))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Mindful Screen Balance & Family Harmony", textAlign = TextAlign.Center, color = MutedGray)
                Spacer(modifier = Modifier.height(48.dp))

                errorMessage?.let {
                    Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isSignUp) {
                    OutlinedTextField(
                      value = name, 
                      onValueChange = { name = it }, 
                      label = { Text("Display Name") }, 
                      modifier = Modifier.fillMaxWidth(),
                      shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                  value = email, 
                  onValueChange = { email = it }, 
                  label = { Text("Email") }, 
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                  value = password, 
                  onValueChange = { password = it }, 
                  label = { Text("Password") }, 
                  visualTransformation = PasswordVisualTransformation(), 
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) return@Button
                        isLoading = true
                        val auth = FirebaseAuth.getInstance()
                        if (isSignUp) {
                            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                                    auth.currentUser?.updateProfile(profileUpdates)
                                } else {
                                    errorMessage = task.exception?.message
                                }
                                isLoading = false
                            }
                        } else {
                            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (!task.isSuccessful) errorMessage = task.exception?.message
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6)),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text(if (isSignUp) "Create Account" else "Sign In", fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = { isSignUp = !isSignUp }) {
                    Text(if (isSignUp) "Already have an account? Sign In" else "New here? Create Account", color = AccentBlue)
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(BuildConfig.FIREBASE_WEB_CLIENT_ID)
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading
                ) {
                    Text("Continue with Google", color = DarkSlate)
                }
            }
        }
    }
}

// RESTORATION OF ORIGINAL HIGH-FIDELITY COMPONENTS
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
  var aiRecommendations by remember { mutableStateOf(prefs.getString("ai_detox_recommendations", null)) }
  var aiLoading by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
  var stressLevel by remember { mutableStateOf(prefs.getFloat("ai_family_stress_level", 5f)) }
  var selectedTimeMode by remember { mutableStateOf(prefs.getString("ai_family_time_mode", "Current System Time") ?: "Current System Time") }
  var targetWellnessScore by remember { mutableStateOf(prefs.getFloat("target_wellness_score", 80f)) }

  LaunchedEffect(Unit) {
    FirestoreManager.loadWellnessData(context, { dbStress, dbTimeMode, dbMood, dbTargetScore, dbScreenGoal ->
        stressLevel = dbStress
        selectedTimeMode = dbTimeMode
        targetWellnessScore = dbTargetScore
        onScreenTimeGoalChange(dbScreenGoal)
    })
  }

  DisposableEffect(context) {
    val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
      if (status == android.speech.tts.TextToSpeech.SUCCESS) tts?.setLanguage(java.util.Locale.UK)
    }
    tts = textToSpeech
    onDispose { tts?.stop(); tts?.shutdown() }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
  val pulseAlpha by infiniteTransition.animateFloat(0.3f, 1.0f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "Alpha")

  val currentScore = ((screenTime / 12f) * 60f + ((10f - sleepLog) / 6f) * 40f).coerceIn(5f, 100f)
  val weeklyScores = listOf(42f, 68f, 85f, 58f, 35f, 22f, currentScore)
  val weeklyDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

  val riskConfig = when {
    screenTime > 7.0f && sleepLog < 6.0f -> RiskConfig("🔴 HIGH RISK", "Burnout indicators detected.", Color(0xFFFFF5F5), Color(0xFFFEE2E2), Color(0xFFDC2626), Color(0xFFEF4444))
    else -> RiskConfig("🟢 LOW RISK", "Digital fatigue is low.", Color(0xFFE2F7EA), Color(0xFFBFF0D4), Color(0xFF047857), Color(0xFF10B981))
  }

  LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
    item {
      Spacer(modifier = Modifier.height(16.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
          Text(text = "Hello, $userName 👋", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = DarkSlate))
          Text(text = "Your digital balance today", color = MutedGray, fontSize = 14.sp)
        }
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(AccentBlueSoft).clickable { onAccountClick() }, contentAlignment = Alignment.Center) {
          Text(text = userName.take(1).uppercase(), color = AccentBlue, fontWeight = FontWeight.Bold)
        }
      }
    }

    item {
      Box(modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp)).background(riskConfig.bgColor).border(1.dp, riskConfig.borderColor, RoundedCornerShape(32.dp)).padding(20.dp)) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(riskConfig.dotColor.copy(alpha = pulseAlpha)))
            Text(text = riskConfig.title, color = riskConfig.textColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }
          Text(text = "Burnout AI Tracker Evaluation", fontWeight = FontWeight.Bold, color = DarkSlate)
          Text(text = riskConfig.desc, color = MutedGray, fontSize = 14.sp)
        }
      }
    }

    item {
      ManualScreenTimeLogCard(screenTime, onScreenTimeChange, screenTimeGoal, onScreenTimeGoalChange)
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricItemCard(Modifier.weight(1f), "${"%.1f".format(screenTime)}h", "Screen", AccentBlue)
        MetricItemCard(Modifier.weight(1f), "${"%.1f".format(sleepLog)}h", "Sleep", AccentGreen)
        MetricItemCard(Modifier.weight(1f), selectedMood ?: "Okay", "Mood", Color(0xFFF59E0B))
      }
    }

    item {
      WeeklyTrendsChartCard(weeklyScores, weeklyDays, targetWellnessScore, { targetWellnessScore = it }, stressLevel, selectedTimeMode, selectedMood ?: "Calm", screenTimeGoal)
    }

    item {
      Box(modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp)).background(AccentGreenSoft).border(1.dp, Color(0xFFBFF0D4), RoundedCornerShape(32.dp)).clickable { onNavigateToHarmony() }.padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Family Harmony Score", fontWeight = FontWeight.Bold, color = DarkSlate)
          }
          Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AccentGreen)
        }
      }
    }
  }
}

@Composable
fun MetricItemCard(modifier: Modifier, value: String, title: String, valueColor: Color) {
  Box(modifier = modifier.shadow(1.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(16.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(text = title.uppercase(), fontSize = 10.sp, color = MutedGray, fontWeight = FontWeight.Bold)
      Text(text = value, fontWeight = FontWeight.Bold, color = valueColor, fontSize = 18.sp)
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
  var stressLevel by remember { mutableStateOf(prefs.getFloat("ai_family_stress_level", 5f)) }
  var selectedTimeMode by remember { mutableStateOf(prefs.getString("ai_family_time_mode", "Current System Time") ?: "Current System Time") }
  var targetWellnessScore by remember { mutableStateOf(prefs.getFloat("target_wellness_score", 80f)) }

  LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
    item {
      Spacer(modifier = Modifier.height(16.dp))
      Text(text = "Family Harmony Index", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = DarkSlate))
      Button(onClick = {
        FirestoreManager.saveWellnessData(context, stressLevel, selectedTimeMode, currentUserMood, targetWellnessScore, screenTimeGoal, {
            onTriggerToast("Cloud Sync Complete!", "sync")
        })
      }, colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)) {
        Text("Sync Data")
      }
    }
    item {
      HouseholdMemberRow(currentUserDisplayName, "Mood: $currentUserMood", "$currentUserScreenTime hrs", currentUserScreenTime > 6f, currentUserDisplayName.take(1), AccentBlue)
    }
  }
}

@Composable
fun HouseholdMemberRow(name: String, moodDescription: String, screenTime: String, isHighest: Boolean, avatarInitials: String, avatarColor: Color) {
  Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
          Text(text = avatarInitials, color = avatarColor, fontWeight = FontWeight.Bold)
        }
        Column {
          Text(text = name, fontWeight = FontWeight.Bold, color = DarkSlate)
          Text(text = moodDescription, color = MutedGray, fontSize = 12.sp)
        }
      }
      Text(text = screenTime, fontWeight = FontWeight.Bold, color = if (isHighest) Color.Red else DarkSlate)
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
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Reconnection Quests", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = DarkSlate))
        }
        item {
            Button(onClick = { onGenerateAIQuests({}) }, enabled = !aiLoading, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) {
                Text(if (aiLoading) "Generating..." else "Get AI Quests")
            }
        }
    }
}

@Composable
fun FamilyFeedView(currentUserDisplayName: String, onTriggerToast: (String, String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Family Feed Coming Soon", color = MutedGray)
    }
}

@Composable
fun MentalResetModal(onClose: () -> Unit) {
    var isInhaling by remember { mutableStateOf(true) }
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
      while(true) {
        isInhaling = true
        scale.animateTo(1.0f, tween(4000))
        isInhaling = false
        scale.animateTo(0.4f, tween(4000))
      }
    }
    Box(modifier = Modifier.fillMaxSize().background(SoftNeutralBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)) {
            Text("Mental Reset Active 🧘", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.size(200.dp * scale.value).clip(CircleShape).background(AccentBlue.copy(alpha = 0.5f)))
            Text(if (isInhaling) "Breathe In..." else "Breathe Out...", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = DarkSlate)) { Text("End Reset") }
        }
    }
}

@Composable
fun AccountScreen(userName: String, onUserNameChange: (String) -> Unit, onResetApp: () -> Unit, onLogOut: () -> Unit, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(SoftNeutralBackground).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Account Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = DarkSlate)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
          Column(Modifier.padding(16.dp)) {
            Text("Logged in as: $userName", fontWeight = FontWeight.Bold)
          }
        }
        Button(onClick = onLogOut, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = DarkSlate)) { Text("Log Out") }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DarkSlate)) { Text("Close") }
    }
}

@Composable
fun ManualScreenTimeLogCard(screenTime: Float, onScreenTimeChange: (Float) -> Unit, screenTimeGoal: Float, onScreenTimeGoalChange: (Float) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Daily Screen Time", fontWeight = FontWeight.Bold, color = DarkSlate)
            Slider(value = screenTime, onValueChange = onScreenTimeChange, valueRange = 0f..24f, colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue))
            Text("${"%.1f".format(screenTime)}h / ${"%.1f".format(screenTimeGoal)}h goal", color = MutedGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun WeeklyTrendsChartCard(scores: List<Float>, days: List<String>, target: Float, onTargetChange: (Float) -> Unit, stress: Float, timeMode: String, mood: String, goal: Float) {
    Card(modifier = Modifier.fillMaxWidth().height(240.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp)) {
          Text("Weekly Burnout Trends", fontWeight = FontWeight.Bold, color = DarkSlate)
          Spacer(Modifier.height(10.dp))
          Canvas(Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val points = scores.mapIndexed { i, s -> i * (width / 6f) to height - (s / 100f * height) }
            val path = Path().apply {
              moveTo(points[0].first, points[0].second)
              points.forEach { lineTo(it.first, it.second) }
            }
            drawPath(path, AccentBlue, style = Stroke(3.dp.toPx()))
          }
        }
    }
}

@Composable
fun UsraAIChatScreen(userName: String, screenTime: Float, onScreenChange: (Float) -> Unit, sleep: Float, onSleepChange: (Float) -> Unit, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(SoftNeutralBackground)) {
        Text("Usra AI Wellness Coach", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
        Box(Modifier.weight(1f))
        Button(onClick = onClose, modifier = Modifier.padding(16.dp).fillMaxWidth()) { Text("Close") }
    }
}

@Composable
fun HealthTrackerDashboard(onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(SoftNeutralBackground), contentAlignment = Alignment.Center) {
        Button(onClick = onClose) { Text("Back to Dashboard") }
    }
}

@Composable
fun BottomNavItem(selected: Boolean, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, activeColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = if (selected) activeColor else MutedGray)
        Text(text = label, color = if (selected) activeColor else MutedGray, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

class BottomBarWithCutoutShape : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

data class RiskConfig(val title: String, val desc: String, val bgColor: Color, val borderColor: Color, val textColor: Color, val dotColor: Color)
