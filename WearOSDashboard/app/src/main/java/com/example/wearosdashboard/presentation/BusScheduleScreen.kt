package com.example.wearosdashboard.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.wearosdashboard.data.BusScheduleRepository

@Composable
fun BusScheduleScreen(onBack: () -> Unit, initialDestination: String? = null) {
    val destinations = remember { BusScheduleRepository.getDestinations() }
    var selectedDestination by remember { mutableStateOf(initialDestination) }
    
    // Global Timer for the screen
    var currentTime by remember { mutableStateOf(java.time.LocalTime.now()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = java.time.LocalTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        if (selectedDestination == null) {
            // Main List: Destination Tiles with Live Countdown
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                anchorType = ScalingLazyListAnchorType.ItemCenter
            ) {
                item {
                    Text(
                        text = "Bus Schedule",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.primary
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                items(destinations.size) { index ->
                    val dest = destinations[index]
                    
                    // Calculate Next Bus for this destination
                    val departures = remember(dest) { BusScheduleRepository.getDeparturesForDestination(dest) }
                    
                    val nextDeparture = remember(currentTime, departures) {
                        departures.firstOrNull { 
                            try {
                                val busTime = java.time.LocalTime.parse(it.time)
                                busTime.isAfter(currentTime)
                            } catch (e: Exception) { false }
                        } ?: departures.firstOrNull()
                    }
                    
                    val timeToNextBus = remember(currentTime, nextDeparture) {
                        if (nextDeparture != null) {
                            try {
                                val busTime = java.time.LocalTime.parse(nextDeparture.time)
                                var seconds = java.time.temporal.ChronoUnit.SECONDS.between(currentTime, busTime)
                                if (seconds < 0) seconds += 86400 // Wrap around
                                
                                val h = seconds / 3600
                                val m = (seconds % 3600) / 60
                                val s = seconds % 60
                                if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
                                else String.format("%02d:%02d", m, s)
                            } catch (e: Exception) { "--" }
                        } else { "--" }
                    }

                    // Color Logic
                    val minutesLeft = remember(currentTime, nextDeparture) {
                        if (nextDeparture != null) {
                            try {
                                val busTime = java.time.LocalTime.parse(nextDeparture.time)
                                var diff = java.time.temporal.ChronoUnit.MINUTES.between(currentTime, busTime)
                                if (diff < 0) diff += 1440
                                diff
                            } catch (e: Exception) { 999L }
                        } else 999L
                    }

                    val timerColor = when {
                        minutesLeft >= 30 -> Color.Green
                        minutesLeft >= 20 -> Color(0xFFFFBF00) // Amber
                        minutesLeft >= 10 -> Color(0xFFFF8C00) // Orange
                        minutesLeft >= 5 -> Color.Red
                        else -> Color.Red
                    }
                    
                    val isPulsing = minutesLeft < 2

                    // Pulsing Animation
                    val infiniteTransition = rememberInfiniteTransition()
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isPulsing) 0.3f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulsing"
                    )

                    Card(
                        onClick = { selectedDestination = dest },
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = Color(0xFF222222),
                            endBackgroundColor = Color(0xFF222222)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = dest,
                                style = MaterialTheme.typography.caption1,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (nextDeparture != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Next: ${nextDeparture.time}",
                                        style = MaterialTheme.typography.caption3,
                                        color = Color(0xFF80CBC4) // Light Teal
                                    )
                                    Text(
                                        text = timeToNextBus,
                                        style = MaterialTheme.typography.caption3,
                                        color = timerColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.alpha(if (isPulsing) alpha else 1f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.iconButtonColors(contentColor = Color.Red)
                    ) {
                        Text("NB") // Navigate Back
                    }
                }
            }
        } else {
            // Detail List for Selected Destination
            val departures = remember(selectedDestination) {
                BusScheduleRepository.getDeparturesForDestination(selectedDestination!!)
            }
            
            // Find next departure (Detail View)
            val nextDeparture = remember(currentTime, departures) {
                departures.firstOrNull { 
                    try {
                        val busTime = java.time.LocalTime.parse(it.time)
                        busTime.isAfter(currentTime)
                    } catch (e: Exception) { false }
                } ?: departures.firstOrNull() 
            }
            
            val timeToNextBus = remember(currentTime, nextDeparture) {
                if (nextDeparture != null) {
                    try {
                        val busTime = java.time.LocalTime.parse(nextDeparture.time)
                        var seconds = java.time.temporal.ChronoUnit.SECONDS.between(currentTime, busTime)
                        if (seconds < 0) seconds += 86400
                        
                        val h = seconds / 3600
                        val m = (seconds % 3600) / 60
                        val s = seconds % 60
                        if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
                        else String.format("%02d:%02d", m, s)
                    } catch (e: Exception) { "--:--:--" }
                } else { "--:--:--" }
            }

            // Color Logic for Detail View
            val minutesLeft = remember(currentTime, nextDeparture) {
                if (nextDeparture != null) {
                    try {
                        val busTime = java.time.LocalTime.parse(nextDeparture.time)
                        var diff = java.time.temporal.ChronoUnit.MINUTES.between(currentTime, busTime)
                        if (diff < 0) diff += 1440
                        diff
                    } catch (e: Exception) { 999L }
                } else 999L
            }

            val timerColor = when {
                minutesLeft >= 30 -> Color.Green
                minutesLeft >= 20 -> Color(0xFFFFBF00) // Amber
                minutesLeft >= 10 -> Color(0xFFFF8C00) // Orange
                minutesLeft >= 5 -> Color.Red
                else -> Color.Red
            }
            
            val isPulsing = minutesLeft < 2

            // Pulsing Animation
            val infiniteTransition = rememberInfiniteTransition()
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isPulsing) 0.3f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulsing_detail"
            )
            
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                anchorType = ScalingLazyListAnchorType.ItemCenter
            ) {
                item {
                    Text(
                        text = selectedDestination!!,
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.secondary
                    )
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // NEXT BUS CARD (Dynamic)
                if (nextDeparture != null) {
                     item {
                        Card(
                            onClick = {},
                            backgroundPainter = CardDefaults.cardBackgroundPainter(
                                startBackgroundColor = Color(0xFF004D40), // Teal Dark
                                endBackgroundColor = Color(0xFF004D40)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "NEXT BUS",
                                    style = MaterialTheme.typography.caption3,
                                    color = Color.Cyan
                                )
                                Text(
                                    text = nextDeparture.time + ":00", 
                                    style = MaterialTheme.typography.title3,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "in $timeToNextBus",
                                    style = MaterialTheme.typography.body2,
                                    color = timerColor,
                                    modifier = Modifier.alpha(if (isPulsing) alpha else 1f)
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item {
                         Text(
                            text = "All Departures",
                            style = MaterialTheme.typography.caption3,
                            color = Color.Gray
                        )
                    }
                }
                
                items(departures.size) { index ->
                    val dep = departures[index]
                    val isNext = dep == nextDeparture
                    
                    Card(
                        onClick = {},
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = if(isNext) Color(0xFF222222) else Color(0xFF111111),
                            endBackgroundColor = if(isNext) Color(0xFF222222) else Color(0xFF111111)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dep.time,
                                    color = if (isNext) Color.Green else Color.Gray,
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = dep.busNumber,
                                    color = Color.White
                                )
                            }
                            if (dep.notes.isNotEmpty()) {
                                Text(
                                    text = dep.notes,
                                    style = MaterialTheme.typography.caption3,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                 item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    Button(
                        onClick = { selectedDestination = null },
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text("Back") 
                    }
                }
            }
        }
    }
}
