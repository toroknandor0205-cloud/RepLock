package com.replock.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.data.TimeQuotaRepository
import com.replock.app.ui.theme.RepLockTheme
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private lateinit var quotaRepo: TimeQuotaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        quotaRepo = TimeQuotaRepository(this)

        setContent {
            RepLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    MainScreen(quotaRepo = quotaRepo)
                }
            }
        }
    }
}

private enum class Screen { HOME, PUSHUP, SETTINGS }

@Composable
fun MainScreen(quotaRepo: TimeQuotaRepository) {
    val context = LocalContext.current
    var remainingSeconds by remember { mutableIntStateOf(quotaRepo.getRemainingSeconds()) }
    var usedSeconds by remember { mutableIntStateOf(quotaRepo.getUsedSecondsToday()) }
    var streak by remember { mutableIntStateOf(quotaRepo.getStreak()) }
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    var isAccessibilityEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }
    var canDrawOverlays by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    LaunchedEffect(currentScreen) {
        remainingSeconds = quotaRepo.getRemainingSeconds()
        usedSeconds = quotaRepo.getUsedSecondsToday()
        streak = quotaRepo.getStreak()
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        canDrawOverlays = Settings.canDrawOverlays(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated Mesh Gradient Background
        MeshGradientBackground()

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (fadeIn(tween(320)) + scaleIn(initialScale = 0.96f))
                    .togetherWith(fadeOut(tween(220)) + scaleOut(targetScale = 0.96f))
            },
            label = "screen"
        ) { screen ->
            when (screen) {
                Screen.HOME -> HomeContent(
                    remainingSeconds = remainingSeconds,
                    usedSeconds = usedSeconds,
                    streak = streak,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    canDrawOverlays = canDrawOverlays,
                    onOpenAccessibility = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenOverlay = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp)
                )

                Screen.PUSHUP -> PushupDetectorScreen(
                    onPushupsCompleted = { count ->
                        remainingSeconds = quotaRepo.addFromPushups(count)
                    },
                    onClose = { currentScreen = Screen.HOME }
                )

                Screen.SETTINGS -> SettingsContent(
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    canDrawOverlays = canDrawOverlays,
                    streak = streak,
                    onOpenAccessibility = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenOverlay = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp)
                )
            }
        }

        if (currentScreen != Screen.PUSHUP) {
            LiquidDock(
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 34.dp)
            )
        }
    }
}

// ====================== MESH GRADIENT BACKGROUND ======================

@Composable
private fun MeshGradientBackground() {
    val infinite = rememberInfiniteTransition(label = "mesh")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base
        drawRect(Color(0xFF03030A))

        // Moving blobs
        val cx1 = w * (0.3f + 0.15f * sin(Math.toRadians(t.toDouble())).toFloat())
        val cy1 = h * (0.25f + 0.1f * cos(Math.toRadians(t * 0.8).toDouble())).toFloat())
        val cx2 = w * (0.75f + 0.12f * cos(Math.toRadians(t * 1.1).toDouble())).toFloat())
        val cy2 = h * (0.65f + 0.12f * sin(Math.toRadians(t * 0.9).toDouble())).toFloat())
        val cx3 = w * (0.5f + 0.2f * sin(Math.toRadians(t * 0.6).toDouble())).toFloat())
        val cy3 = h * (0.5f + 0.15f * cos(Math.toRadians(t * 1.3).toDouble())).toFloat())

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A1A3A).copy(alpha = 0.55f), Color.Transparent),
                center = Offset(cx1, cy1),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(cx1, cy1)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0F2A3A).copy(alpha = 0.45f), Color.Transparent),
                center = Offset(cx2, cy2),
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = Offset(cx2, cy2)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2A1535).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(cx3, cy3),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(cx3, cy3)
        )
    }
}

// ====================== HOME ======================

@Composable
private fun HomeContent(
    remainingSeconds: Int,
    usedSeconds: Int,
    streak: Int,
    isAccessibilityEnabled: Boolean,
    canDrawOverlays: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remainingSeconds.toFloat() / TimeQuotaRepository.DAILY_LIMIT_SECONDS

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RepLock",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            text = "45 minutes a day. Earn more with push-ups.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Circular progress with glass
        Box(contentAlignment = Alignment.Center) {
            // Outer glass ring background
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Canvas(modifier = Modifier.size(220.dp)) {
                val stroke = 14.dp.toPx()
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    style = Stroke(width = stroke)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFF6B8CFF),
                            Color(0xFF8AB4FF),
                            Color(0xFF6B8CFF)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(remainingSeconds),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassStatCard(
                title = "Used today",
                value = formatTime(usedSeconds),
                modifier = Modifier.weight(1f)
            )
            GlassStatCard(
                title = "Streak",
                value = "\( streak day \){if (streak == 1) "" else "s"}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (remainingSeconds <= 0) {
            GlassCard {
                Text(
                    text = "Time is up.\nDo push-ups to earn more minutes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF8A9B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        StatusRow("Accessibility", isAccessibilityEnabled, onOpenAccessibility)
        Spacer(modifier = Modifier.height(10.dp))
        StatusRow("Overlay permission", canDrawOverlays, onOpenOverlay)
    }
}

@Composable
private fun SettingsContent(
    isAccessibilityEnabled: Boolean,
    canDrawOverlays: Boolean,
    streak: Int,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(32.dp))

        StatusRow("Accessibility Service", isAccessibilityEnabled, onOpenAccessibility)
        Spacer(modifier = Modifier.height(10.dp))
        StatusRow("Display over other apps", canDrawOverlays, onOpenOverlay)

        Spacer(modifier = Modifier.height(24.dp))

        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Rules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• 45 minutes per day\n• 2 correct push-ups = +1 minute\n• Resets every midnight\n• Current streak: $streak days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ====================== DOCK ======================

@Composable
private fun LiquidDock(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val positions = remember { mutableStateListOf(0f, 0f, 0f) }
    val indicatorX = remember { Animatable(0f) }

    LaunchedEffect(currentScreen, positions.toList()) {
        val index = when (currentScreen) {
            Screen.HOME -> 0
            Screen.PUSHUP -> 1
            Screen.SETTINGS -> 2
        }
        if (positions[index] != 0f) {
            indicatorX.animateTo(
                positions[index],
                spring(dampingRatio = 0.72f, stiffness = 400f)
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(44.dp))
            .background(Color(0xFF0E0E18).copy(alpha = 0.75f))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(0.22f), Color.White.copy(0.05f))
                ),
                RoundedCornerShape(44.dp)
            )
            .padding(8.dp)
    ) {
        // Sliding glass indicator
        Box(
            modifier = Modifier
                .offset { IntOffset(indicatorX.value.roundToInt(), 0) }
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(0.20f),
                            Color.White.copy(0.05f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(Color.White.copy(0.4f), Color.White.copy(0.08f))
                    ),
                    CircleShape
                )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DockButton(Icons.Rounded.Home, currentScreen == Screen.HOME, {
                onNavigate(Screen.HOME)
            }) { x, _ -> positions[0] = x }

            DockButton(Icons.Rounded.FitnessCenter, currentScreen == Screen.PUSHUP, {
                onNavigate(Screen.PUSHUP)
            }) { x, _ -> positions[1] = x }

            DockButton(Icons.Rounded.Settings, currentScreen == Screen.SETTINGS, {
                onNavigate(Screen.SETTINGS)
            }) { x, _ -> positions[2] = x }
        }
    }
}

@Composable
private fun DockButton(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    onPositioned: (Float, Float) -> Unit
) {
    val scale by animateFloatAsState(
        if (selected) 1.1f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .onGloballyPositioned {
                onPositioned(it.positionInParent().x, it.size.width.toFloat())
            }
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) Color(0xFF8AB4FF) else Color.White.copy(0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}

// ====================== GLASS COMPONENTS ======================

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(0.16f), Color.White.copy(0.03f))
                ),
                RoundedCornerShape(28.dp)
            )
    ) {
        content()
    }
}

@Composable
private fun GlassStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(0.14f), Color.White.copy(0.03f))
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.5f))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun StatusRow(title: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(0.14f), Color.White.copy(0.03f))
                ),
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White)
            Text(
                if (enabled) "Enabled" else "Disabled",
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color(0xFF8AB4FF) else Color(0xFFFF8A9B)
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val list = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    return list.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}
