package com.example

import android.os.Bundle
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

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
  var currentTab by rememberSaveable { mutableStateOf(0) }
  var selectedMood by rememberSaveable { mutableStateOf<String?>(null) }
  var questsChecked by rememberSaveable { mutableStateOf(listOf(false, false, false)) }

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
            selectedMood = selectedMood,
            onMoodSelect = { selectedMood = it },
            onNavigateToHarmony = { currentTab = 1 }
          )
          1 -> FamilyHarmonyView(
            currentUserMood = selectedMood ?: "Tired"
          )
          2 -> ReconnectionQuestsView(
            questsChecked = questsChecked,
            onQuestToggle = { index ->
              val newList = questsChecked.toMutableList()
              newList[index] = !newList[index]
              questsChecked = newList
            }
          )
        }
      }
    }
  }
}

@Composable
fun HomeDashboardView(
  selectedMood: String?,
  onMoodSelect: (String) -> Unit,
  onNavigateToHarmony: () -> Unit
) {
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
            text = "Good morning, Sami 👋",
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
            .background(AccentBlueSoft),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(AccentBlue.copy(alpha = 0.2f))
          )
          Text(
            text = "S",
            color = AccentBlue,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp
          )
        }
      }
    }

    // High Burnout Warning Card (Clean Utility / Minimal Light/Soft Style)
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
          .background(
            Brush.linearGradient(
              colors = listOf(Color(0xFFFFF5F5), Color(0xFFFFFBEB))
            )
          )
          .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(32.dp))
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
                .background(Color(0xFFEF4444).copy(alpha = pulseAlpha))
            )
            Text(
              text = "High Burnout Risk",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDC2626),
                fontSize = 11.sp,
                letterSpacing = 1.sp
              )
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Digital Fatigue Detected",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = DarkSlate,
              fontSize = 18.sp
            )
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Late-night screen usage + low sleep detected. Your focus might be 30% lower today.",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MutedGray,
              fontSize = 14.sp,
              lineHeight = 20.sp
            )
          )
        }
      }
    }

    // Quick Grid Row
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricItemCard(
          modifier = Modifier.weight(1f),
          value = "7.5h",
          title = "Screen",
          valueColor = AccentBlue
        )
        MetricItemCard(
          modifier = Modifier.weight(1f),
          value = "5.5h",
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
  currentUserMood: String
) {
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
        // SAMI (Highest Screen warning highlighted)
        HouseholdMemberRow(
          name = "Sami (Student)",
          moodDescription = "Mood: $currentUserMood",
          screenTime = "7.5 hrs screen",
          isHighest = true,
          avatarInitials = "S",
          avatarColor = AccentBlue
        )

        // MOM
        HouseholdMemberRow(
          name = "Mom (Working Parent)",
          moodDescription = "Mood: Okay",
          screenTime = "4.0 hrs screen",
          isHighest = false,
          avatarInitials = "M",
          avatarColor = AccentGreen
        )

        // DAD
        HouseholdMemberRow(
          name = "Dad (Work Mode)",
          moodDescription = "Mood: Happy",
          screenTime = "5.0 hrs screen",
          isHighest = false,
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
fun ReconnectionQuestsView(
  questsChecked: List<Boolean>,
  onQuestToggle: (Int) -> Unit
) {
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

    // List of Quest Items
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
              .clickable { onQuestToggle(index) }
              .then(
                if (isChecked) Modifier.border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                else Modifier
              ),
            colors = CardDefaults.cardColors(
              containerColor = if (isChecked) AccentGreenSoft.copy(alpha = 0.25f) else Color.White
            ),
            shape = RoundedCornerShape(22.dp)
          ) {
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
                onCheckedChange = { onQuestToggle(index) },
                colors = CheckboxDefaults.colors(
                  checkedColor = AccentGreen,
                  uncheckedColor = Color.LightGray
                )
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
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

private fun mapMoodToEmoji(mood: String?): String {
  return when (mood) {
    "Tired" -> "😴"
    "Stressed" -> "😟"
    "Okay" -> "🙂"
    "Happy" -> "😄"
    else -> "😴" // Default starting state
  }
}

