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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.app.data.VideoQuotaRepository
import com.replock.app.ui.theme.RepLockTheme

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF05050A),
                            Color(0xFF0A0A18),
                            Color(0xFF05050A)
                        )
                    )
                )
        )

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (fadeIn(tween(280)) + scaleIn(initialScale = 0.96f, animationSpec = tween(280)))
                    .togetherWith(fadeOut(tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)))
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
                        .padding(bottom = 110.dp)
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
                        .padding(bottom = 110.dp)
                )
            }
        }

        if (currentScreen != Screen.PUSHUP) {
            FloatingIslandDock(
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
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
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RepLock",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Stop the scroll. Earn every video.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Remaining today",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$remaining",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (remaining > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "of ${VideoQuotaRepository.DAILY_LIMIT}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (remaining <= 0) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Daily limit reached. Perform push-ups to unlock more videos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        StatusGlassRow(
            title = "Accessibility",
            enabled = isAccessibilityEnabled,
            onClick = onOpenAccessibilitySettings
        )
        Spacer(modifier = Modifier.height(12.dp))
        StatusGlassRow(
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
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(32.dp))

        StatusGlassRow(
            title = "Accessibility Service",
            enabled = isAccessibilityEnabled,
            onClick = onOpenAccessibilitySettings
        )
        Spacer(modifier = Modifier.height(12.dp))
        StatusGlassRow(
            title = "Display over other apps",
            enabled = canDrawOverlays,
            onClick = onOpenOverlaySettings
        )
        Spacer(modifier = Modifier.height(24.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "RepLock watches TikTok and Instagram Reels through the Accessibility Service. After 75 videos the feed is blocked. Each correct push-up detected by the front camera restores one video.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FloatingIslandDock(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .background(Color(0xFF12121E).copy(alpha = 0.82f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(40.dp)
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IslandButton(
            icon = Icons.Rounded.Home,
            selected = currentScreen == Screen.HOME,
            onClick = { onNavigate(Screen.HOME) }
        )
        IslandButton(
            icon = Icons.Rounded.FitnessCenter,
            selected = currentScreen == Screen.PUSHUP,
            onClick = { onNavigate(Screen.PUSHUP) }
        )
        IslandButton(
            icon = Icons.Rounded.Settings,
            selected = currentScreen == Screen.SETTINGS,
            onClick = { onNavigate(Screen.SETTINGS) }
        )
    }
}

@Composable
private fun IslandButton(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    val bgColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    else Color.Transparent

    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
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
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF141422).copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        content()
    }
}

@Composable
private fun StatusGlassRow(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (enabled) "Enabled" else "Disabled",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary
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
