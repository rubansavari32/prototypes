package com.example.wearosdashboard

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.wearosdashboard.data.PriceRepository
import com.example.wearosdashboard.presentation.BusScheduleScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Settings
import com.example.wearosdashboard.presentation.theme.WearOSDashboardTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    // Enum for Navigation
    enum class Screen {
        Dashboard,
        BusSchedule,
        Settings
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val initialDest = intent.getStringExtra("destination_name")
        val startScreen = if (initialDest != null) Screen.BusSchedule else Screen.Dashboard

        setContent {
            WearOSDashboardTheme {
                var currentScreen by remember { mutableStateOf(startScreen) }

                Scaffold(
                    timeText = { TimeText() },
                    vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
                ) {
                    when (currentScreen) {
                        Screen.Dashboard -> DashboardScreen(
                            context = this@MainActivity,
                            onNavigateToBus = { currentScreen = Screen.BusSchedule },
                            onNavigateToSettings = { currentScreen = Screen.Settings }
                        )
                        Screen.BusSchedule -> BusScheduleScreen(
                            onBack = { currentScreen = Screen.Dashboard },
                            initialDestination = initialDest
                        )
                        Screen.Settings -> com.example.wearosdashboard.presentation.TileSettingsScreen(
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    context: Context, 
    onNavigateToBus: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var weather by remember { mutableStateOf("Loading...") }
    var goldPrice by remember { mutableStateOf("Loading...") }
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableIntStateOf(0) }
    var steps by remember { mutableStateOf("0") }

    // Fetch Prices & Weather
    LaunchedEffect(Unit) {
        while (true) {
            weather = PriceRepository.getWeatherDubai()
            goldPrice = PriceRepository.get22kGoldAed()
            delay(60000) // Update every minute
        }
    }

    // Update Time and Battery
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
            
            // Battery
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            delay(1000) // Update every second for time
        }
    }

    // Step Counter
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) // Use TYPE_STEP_COUNTER
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.values.isNotEmpty()) {
                        steps = it.values[0].toInt().toString()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        stepSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            anchorType = ScalingLazyListAnchorType.ItemCenter
        ) {
            item {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.title2,
                    color = MaterialTheme.colors.primary
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Bus Schedule Chip
            item {
                Chip(
                    onClick = onNavigateToBus,
                    label = { Text("Bus Schedule") },
                    icon = { Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = "Bus") },
                    colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFF00695C)), // Teal Dark
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            // Settings Chip
            item {
                Chip(
                    onClick = onNavigateToSettings,
                    label = { Text("Tile Settings") },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Gold Card
            item {
                DashboardCard(
                    title = "22k Gold (UAE)",
                    value = goldPrice,
                    color = Color(0xFFFFD700) 
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Weather Card
            item {
                DashboardCard(
                    title = "Dubai Weather",
                    value = weather,
                    color = Color(0xFF87CEEB) // Sky Blue
                )
            }

            // Steps Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
            item {
                DashboardCard(
                    title = "Steps",
                    value = steps,
                    color = Color(0xFF98FB98) // Pale Green
                )
            }
            
            // Info Row
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔋 $batteryLevel%", style = MaterialTheme.typography.body2)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = currentDate, style = MaterialTheme.typography.body2)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, value: String, color: Color) {
    Card(
        onClick = { /* Refresh? */ },
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = Color(0xFF222222),
            endBackgroundColor = Color(0xFF222222)
        ),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.caption2,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.title2.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}
