package com.example.tinyplants

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import java.util.Calendar

class MainActivity : ComponentActivity() {

    // Android 13+
    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        askNotificationPermissionIfNeeded()

        setContent {
            val vm = remember {
                PlantViewModel(
                    repo = PlantRepository(),
                    storage = PlantStorage(applicationContext),
                    appContext = applicationContext
                )
            }
            TinyPlantsApp(vm)
        }
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestNotifPermission.launch(permission)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tiny_plants_channel",
                "Tiny Plants Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}

/** Notification receiver */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val notif = NotificationCompat.Builder(context, "tiny_plants_channel")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Tiny Plants 💚")
            .setContentText("Time to water your plant! 🪴")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || nm.areNotificationsEnabled()) {
            nm.notify(1001, notif)
        }
    }
}

@Composable
fun TinyPlantsApp(vm: PlantViewModel) {
    val navController = rememberNavController()
    val items = listOf(
        BottomItem("plants", "Plants", Icons.Default.Home),
        BottomItem("favorites", "Favorites", Icons.Default.Favorite),
        BottomItem("reminder", "Reminder", Icons.Default.Notifications)
    )

    // 🌿 Pastel
    val pastelGreenScheme = lightColorScheme(
        primary = Color(0xFF6FBF73),
        onPrimary = Color.White,
        secondary = Color(0xFFF6C1D0),
        onSecondary = Color(0xFF3A2C2C),
        background = Color(0xFFF8FFF6),
        onBackground = Color(0xFF1F2D1F),
        surface = Color(0xFFF8FFF6),
        onSurface = Color(0xFF1F2D1F),
        surfaceVariant = Color(0xFFE6F5E6),
        onSurfaceVariant = Color(0xFF233323),
        tertiary = Color(0xFFFFE6A7),
        onTertiary = Color(0xFF3B2E10)
    )

    MaterialTheme(colorScheme = pastelGreenScheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "plants",
                modifier = Modifier.padding(padding)
            ) {
                composable("plants") { PlantsScreen(vm) }
                composable("favorites") { FavoritesScreen(vm) }
                composable("reminder") { ReminderScreen(vm) }
            }
        }
    }
}

data class BottomItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantsScreen(vm: PlantViewModel) {
    val state by vm.state.collectAsState()

    // 🫧
    val bg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            Color(0xFFFFF3F7)
        )
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tiny Plants 🪴", fontWeight = FontWeight.Bold)
                    Text(
                        "your cute green buddies",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            actions = {
                if (!state.loading) {
                    TextButton(onClick = { vm.refreshPlants() }) {
                        Text("Refresh ✨")
                    }
                }
            }
        )

        // 🌟
        CuteHeaderCard(
            count = state.plants.size,
            favCount = state.favorites.size
        )

        when {
            state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Growing plants...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Oops: ${state.error}")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { vm.refreshPlants() }) { Text("Retry 🌿") }
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.plants) { plant ->
                        CutePlantCard(
                            plant = plant,
                            liked = state.favorites.contains(plant.id),
                            onLike = { vm.toggleFavorite(plant.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CuteHeaderCard(count: Int, favCount: Int) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌱", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Welcome back!", fontWeight = FontWeight.SemiBold)
                Text(
                    "You have $count tiny buddies",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            AssistChip(
                onClick = {},
                label = { Text("Fav $favCount 💚") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            )
        }
    }
}

@Composable
fun CutePlantCard(plant: Plant, liked: Boolean, onLike: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(plant.emoji, style = MaterialTheme.typography.titleLarge)
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        plant.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (plant.note.isNotBlank()) {
                        Text(
                            plant.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onLike) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "favorite",
                        tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(10.dp))


            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = {}, label = { Text("Tiny 🌱") })
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            when {
                                plant.note.contains("sun", true) -> "Sunny ☀️"
                                plant.note.contains("water", true) -> "Weekly Water 💧"
                                plant.note.contains("humidity", true) -> "Humid Love 🌫️"
                                else -> "Easy Care ✨"
                            }
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(vm: PlantViewModel) {
    val state by vm.state.collectAsState()
    val favPlants = state.plants.filter { state.favorites.contains(it.id) }

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFFEFF9EF)
                    )
                )
            )
    ) {
        CenterAlignedTopAppBar(title = { Text("My Favorites 💚") })

        if (favPlants.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No favorites yet 🌱\nGo like some plants!",
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favPlants) { plant ->
                    CutePlantCard(
                        plant = plant,
                        liked = true,
                        onLike = { vm.toggleFavorite(plant.id) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(vm: PlantViewModel) {
    val state by vm.state.collectAsState()
    val reminder = state.reminder

    var hour by remember(reminder.hour) { mutableStateOf(reminder.hour) }
    var minute by remember(reminder.minute) { mutableStateOf(reminder.minute) }
    var enabled by remember(reminder.enabled) { mutableStateOf(reminder.enabled) }

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFFFFF7E8)
                    )
                )
            )
    ) {
        CenterAlignedTopAppBar(title = { Text("Water Reminder 💧") })

        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            vm.updateReminder(enabled, hour, minute)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (enabled) "Reminder On 🌿" else "Reminder Off 😴")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Time:")
                    OutlinedTextField(
                        value = hour.toString().padStart(2, '0'),
                        onValueChange = {
                            hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour
                            if (enabled) vm.updateReminder(enabled, hour, minute)
                        },
                        label = { Text("HH") },
                        modifier = Modifier.width(90.dp)
                    )
                    OutlinedTextField(
                        value = minute.toString().padStart(2, '0'),
                        onValueChange = {
                            minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute
                            if (enabled) vm.updateReminder(enabled, hour, minute)
                        },
                        label = { Text("MM") },
                        modifier = Modifier.width(90.dp)
                    )
                }

                Text(
                    "You’ll get a cute daily ping to water your plant 🪴",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
