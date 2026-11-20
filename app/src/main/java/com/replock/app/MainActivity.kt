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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.data.VideoQuotaRepository
import com.replock.app.ui.theme.RepLockTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var quotaRepo: VideoQuotaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        quotaRepo = VideoQuotaRepository(this)

        setContent {
            RepLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(quotaRepo = quotaRepo)
                }
            }
        }
    }
}

private enum class Screen {
    HOME, PUSHUP, SETTINGS
}

@Composable
fun MainScreen(quotaRepo: VideoQuotaRepository) {
    val context = LocalContext.current
    var remaining by remember { mutableIntStateOf(quotaRepo.getRemaining()) }
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var isAccessibilityEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }
    var canDrawOverlays by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    LaunchedEffect(currentScreen) {
        remaining = quotaRepo.getRemaining()
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        canDrawOverlays = Settings.canDrawOverlays(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Deep background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF030308),
                            Color(0xFF0A0A16),
                            Color(0xFF05050C)
                        )
                    )
                )
        )

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (fadeIn(tween(300)) + scaleIn(initialScale = 0.97f, animationSpec = tween(300)))
                    .togetherWith(fadeOut(tween(220)) + scaleOut(targetScale = 0.97f, animationSpec = tween(220)))
            },
            label = "screen"
        ) { screen ->
            when (screen) {
                Screen.HOME -> HomeContent(
                    remaining = remaining,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    canDrawOverlays = canDrawOverlays,
                    onOpenAccessibilitySettings = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenOverlaySettings = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp)
                )

                Screen.PUSHUP -> PushupDetectorScreen(
                    onPushupDetected = {
                        remaining = quotaRepo.addFromPushup()
                    },
                    onClose = { currentScreen = Screen.HOME }
                )

                Screen.SETTINGS -> SettingsContent(
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    canDrawOverlays = canDrawOverlays,
                    onOpenAccessibilitySettings = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenOverlaySettings = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp)
                )
            }
        }

        if (currentScreen != Screen.PUSHUP) {
            LiquidGlassDock(
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun HomeContent(
    remaining: Int,
    isAccessibilityEnabled: Boolean,
    canDrawOverlays: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RepLock",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            text = "Stop the scroll. Earn every video.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(44.dp))

        // Big glass counter
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Remaining today",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$remaining",
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (remaining > 0) Color(0xFF8AB4FF) else Color(0xFFFF8A9B)
                )
                Text(
                    text = "of ${VideoQuotaRepository.DAILY_LIMIT}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (remaining <= 0) {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Daily limit reached.\nPerform push-ups to unlock more videos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF8A9B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        StatusRow(
            title = "Accessibility",
            enabled = isAccessibilityEnabled,
            onClick = onOpenAccessibilitySettings
        )
        Spacer(modifier = Modifier.height(12.dp))
        StatusRow(
            title = "Overlay permission",
            enabled = canDrawOverlays,
            onClick = onOpenOverlaySettings
        )
    }
}

@Composable
private fun SettingsContent(
    isAccessibilityEnabled: Boolean,
    canDrawOverlays: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(36.dp))

        StatusRow(
            title = "Accessibility Service",
            enabled = isAccessibilityEnabled,
            onClick = onOpenAccessibilitySettings
        )
        Spacer(modifier = Modifier.height(12.dp))
        StatusRow(
            title = "Display over other apps",
            enabled = canDrawOverlays,
            onClick = onOpenOverlaySettings
        )
        Spacer(modifier = Modifier.height(28.dp))

        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "RepLock monitors TikTok and Instagram Reels. After 75 videos the feed is blocked. Every correct push-up detected by the camera restores one video.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ==================== LIQUID GLASS DOCK WITH SLIDER ====================

@Composable
private fun LiquidGlassDock(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Positions of the three buttons
    val buttonPositions = remember { mutableStateListOf(0f, 0f, 0f) }
    val buttonWidth = remember { mutableStateOf(0f) }

    val indicatorOffset = remember { Animatable(0f) }

    // Animate indicator when screen changes
    LaunchedEffect(currentScreen, buttonPositions.toList()) {
        val index = when (currentScreen) {
            Screen.HOME -> 0
            Screen.PUSHUP -> 1
            Screen.SETTINGS -> 2
        }
        if (buttonPositions[index] != 0f) {
            indicatorOffset.animateTo(
                targetValue = buttonPositions[index],
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(42.dp))
            .background(Color(0xFF10101C).copy(alpha = 0.78f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = RoundedCornerShape(42.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        // Liquid glass sliding indicator
        Box(
            modifier = Modifier
                .offset { IntOffset(indicatorOffset.value.roundToInt(), 0) }
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockIconButton(
                icon = Icons.Rounded.Home,
                selected = currentScreen == Screen.HOME,
                onClick = { onNavigate(Screen.HOME) },
                onPositioned = { x, width ->
                    buttonPositions[0] = x
                    buttonWidth.value = width
                }
            )
            DockIconButton(
                icon = Icons.Rounded.FitnessCenter,
                selected = currentScreen == Screen.PUSHUP,
                onClick = { onNavigate(Screen.PUSHUP) },
                onPositioned = { x, _ -> buttonPositions[1] = x }
            )
            DockIconButton(
                icon = Icons.Rounded.Settings,
                selected = currentScreen == Screen.SETTINGS,
                onClick = { onNavigate(Screen.SETTINGS) },
                onPositioned = { x, _ -> buttonPositions[2] = x }
            )
        }
    }
}

@Composable
private fun DockIconButton(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    onPositioned: (x: Float, width: Float) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconScale"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .onGloballyPositioned { coordinates ->
                onPositioned(
                    coordinates.positionInParent().x,
                    coordinates.size.width.toFloat()
                )
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
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color(0xFF8AB4FF) else Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(24.dp)
        )
    }
}

// ==================== GLASS COMPONENTS ====================

@Composable
private fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF141422).copy(alpha = 0.65f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
    ) {
        content()
    }
}

@Composable
private fun StatusRow(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = if (enabled) "Enabled" else "Disabled",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color(0xFF8AB4FF) else Color(0xFFFF8A9B)
            )
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices =
        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}
