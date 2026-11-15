package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Hexagon
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted

enum class JarvisTab(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val testTag: String
) {
    COMMAND("command", "COMMAND", Icons.Rounded.Hexagon, Icons.Outlined.Hexagon, "nav_command"),
    CHAT("chat", "CHAT", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_chat"),
    AI_CORE("aicore", "AI CORE", Icons.Rounded.GridView, Icons.Outlined.GridView, "nav_ai_core"),
    PROFILE("profile", "PROFILE", Icons.Rounded.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun JarvisBottomBar(
    currentRoute: String,
    isListeningOrSpeaking: Boolean,
    onSelectTab: (JarvisTab) -> Unit,
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "orb_pulse")
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_glow"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ObsidianSurface.copy(alpha = 0.96f),
        tonalElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .border(
                    width = 1.dp,
                    color = ObsidianBorder.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    tab = JarvisTab.COMMAND,
                    selected = currentRoute == JarvisTab.COMMAND.route,
                    onClick = { onSelectTab(JarvisTab.COMMAND) }
                )
                NavBarItem(
                    tab = JarvisTab.CHAT,
                    selected = currentRoute == JarvisTab.CHAT.route,
                    onClick = { onSelectTab(JarvisTab.CHAT) }
                )

                // Center Voice Orb placeholder spacing
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-14).dp)
                            .size(58.dp)
                            .shadow(
                                elevation = if (isListeningOrSpeaking) 18.dp else 10.dp,
                                shape = CircleShape,
                                ambientColor = ArcCyan,
                                spotColor = ArcCyan
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ArcCyan.copy(alpha = if (isListeningOrSpeaking) glowAlpha else 0.9f),
                                        ArcBlue,
                                        ObsidianCard
                                    )
                                )
                            )
                            .border(2.dp, ArcCyan, CircleShape)
                            .clickable(onClick = onOrbClick)
                            .minimumInteractiveComponentSize()
                            .testTag("nav_voice_orb"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Talk to Jarvis",
                            tint = ObsidianDeep,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = "TALK TO JARVIS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        ),
                        color = ArcCyan,
                        modifier = Modifier.offset(y = (-10).dp)
                    )
                }

                NavBarItem(
                    tab = JarvisTab.AI_CORE,
                    selected = currentRoute == JarvisTab.AI_CORE.route,
                    onClick = { onSelectTab(JarvisTab.AI_CORE) }
                )
                NavBarItem(
                    tab = JarvisTab.PROFILE,
                    selected = currentRoute == JarvisTab.PROFILE.route,
                    onClick = { onSelectTab(JarvisTab.PROFILE) }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    tab: JarvisTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) ArcCyan else TextMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(tab.testTag)
    ) {
        Icon(
            imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = tint
        )
    }
}
