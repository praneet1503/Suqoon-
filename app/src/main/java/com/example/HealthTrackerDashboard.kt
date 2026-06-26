package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun HealthTrackerDashboard(modifier: Modifier = Modifier, onClose: () -> Unit = {}) {
    var steps by remember { mutableStateOf(8432) }
    var stressScore by remember { mutableStateOf(42) }
    var isBreathingSessionActive by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        // App Bar with close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .safeDrawingPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF1E293B)
                )
            }
            Text(
                text = "Watch Data",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            // Top Sync Status Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = "Watch Icon",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Huawei Watch Fit Connected",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155),
                        fontSize = 14.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)) // Emerald green
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sync 2 mins ago",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Activity & Movement Card (Phone Sensor Data)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Activity & Movement",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(140.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { steps / 10000f },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF3B82F6),
                            strokeWidth = 12.dp,
                            trackColor = Color(0xFFE2E8F0)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$steps",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                fontSize = 28.sp
                            )
                            Text(
                                text = "/ 10,000 steps",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("340", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 18.sp)
                            Text("Calories (kcal)", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("45", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 18.sp)
                            Text("Active (mins)", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Huawei TruSleep™ Deep Analytics Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Huawei TruSleep™",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "7h 45m Total Sleep",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1), // Indigo
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Segmented horizontal bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.25f)
                                .fillMaxHeight()
                                .background(Color(0xFF4C1D95)) // Dark Purple (Deep)
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                                .background(Color(0xFFA78BFA)) // Lavender (Light)
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.20f)
                                .fillMaxHeight()
                                .background(Color(0xFF60A5FA)) // Light Blue (REM)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4C1D95)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Deep (25%)", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFA78BFA)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Light (55%)", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF60A5FA)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("REM (20%)", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)) // Indigo light
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("✨", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Usra AI: Your Deep Sleep dropped by 12% due to late screen activity. Try stepping away earlier tonight.",
                            color = Color(0xFF4338CA),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Huawei TruRelax™ Stress Monitor Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Huawei TruRelax™",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$stressScore",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B), // Amber
                            fontSize = 36.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Stress Score", fontWeight = FontWeight.Medium, color = Color(0xFF64748B), fontSize = 14.sp)
                            Text("Normal", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { isBreathingSessionActive = !isBreathingSessionActive },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBreathingSessionActive) Color(0xFF10B981) else Color(0xFFF1F5F9),
                            contentColor = if (isBreathingSessionActive) Color.White else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBreathingSessionActive) "Calming... (Tap to End)" else "🧘 Start 2-Min Suqoon Session")
                    }
                }
            }
        }

        // Biometrics Sub-Row (Heart Rate & SpO2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Heart Rate Card
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Heart Rate", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("72 BPM", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Mock sparkline
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEE2E2))
                        ) {
                            Text("Live Graph", color = Color(0xFFEF4444), fontSize = 10.sp, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }

                // Blood Oxygen Card
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bloodtype, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Blood Oxygen", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("98% SpO2", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD1FAE5))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Optimal", color = Color(0xFF047857), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
    }
}
