package com.example.wearosdashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.wearosdashboard.data.BusScheduleRepository

@Composable
fun BusScheduleScreen(onBack: () -> Unit) {
    val destinations = remember { BusScheduleRepository.getDestinations() }
    var selectedDestination by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        if (selectedDestination == null) {
            // List of Destinations
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                anchorType = ScalingLazyListAnchorType.ItemCenter
            ) {
                item {
                    Text(
                        text = "Destinations",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.primary
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                items(destinations.size) { index ->
                    val dest = destinations[index]
                    Chip(
                        onClick = { selectedDestination = dest },
                        label = { 
                            Text(
                                text = dest, 
                                maxLines = 1,
                                style = MaterialTheme.typography.caption2
                            ) 
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
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
            // List of Departures for Selected Destination
            val departures = remember(selectedDestination) {
                BusScheduleRepository.getDeparturesForDestination(selectedDestination!!)
            }
            
            // "Next Bus" Logic
            var currentTime by remember { mutableStateOf(java.time.LocalTime.now()) }
            
            LaunchedEffect(Unit) {
                while (true) {
                    currentTime = java.time.LocalTime.now()
                    kotlinx.coroutines.delay(1000)
                }
            }

            // Find next departure
            val nextDeparture = remember(currentTime, departures) {
                departures.firstOrNull { 
                    try {
                        val busTime = java.time.LocalTime.parse(it.time)
                        busTime.isAfter(currentTime)
                    } catch (e: Exception) { false }
                } ?: departures.firstOrNull() // Wrap around to first bus of next day if none left today
            }
            
            // Calculate time difference string
            val timeToNextBus = remember(currentTime, nextDeparture) {
                if (nextDeparture != null) {
                    try {
                        val busTime = java.time.LocalTime.parse(nextDeparture.time)
                        var seconds = java.time.temporal.ChronoUnit.SECONDS.between(currentTime, busTime)
                        if (seconds < 0) {
                            // If bus is "tomorrow" (wrapped around), add 24 hours (86400 seconds)
                            seconds += 86400
                        }
                        
                        val h = seconds / 3600
                        val m = (seconds % 3600) / 60
                        val s = seconds % 60
                        String.format("%02d:%02d:%02d", h, m, s)
                    } catch (e: Exception) { "--:--:--" }
                } else {
                    "--:--:--"
                }
            }
            
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
                                    text = nextDeparture.time + ":00", // "Arrival timing with seconds" interpretation 1
                                    style = MaterialTheme.typography.title3,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "in $timeToNextBus",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.Yellow
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
                    // Highlight the next bus in the list as well
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
