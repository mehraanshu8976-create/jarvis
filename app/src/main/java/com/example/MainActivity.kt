package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.configureAppCheck
import com.example.ui.AiResponseState
import com.example.ui.JarvisViewModel
import com.example.ui.components.JarvisBottomBar
import com.example.ui.components.JarvisTab
import com.example.ui.components.VoiceCommandOverlay
import com.example.ui.screens.AiCoreScreen
import com.example.ui.screens.AiCoreSubTab
import com.example.ui.screens.BootScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CommandDashboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.ObsidianDeep

private const val ROUTE_BOOT = "boot"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check with launch intent before any Compose UI, ViewModel, or Firebase call.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            JarvisTheme {
                JarvisAppRoot()
            }
        }
    }
}

@Composable
fun JarvisAppRoot(
    viewModel: JarvisViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_BOOT

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val skills by viewModel.skills.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val flashlightOn by viewModel.flashlightOn.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isPlayingSample by viewModel.isPlayingSample.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var showVoiceOverlay by remember { mutableStateOf(false) }
    var requestedAiCoreSubTab by remember { mutableStateOf(AiCoreSubTab.MODELS) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        viewModel.startTelemetry()
        onDispose { viewModel.stopTelemetry() }
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { banner ->
            snackbarHostState.showSnackbar(banner)
            viewModel.clearStatusBanner()
        }
    }

    val isHudVisible = currentRoute != ROUTE_BOOT

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianDeep,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (isHudVisible) {
                JarvisBottomBar(
                    currentRoute = currentRoute,
                    isListeningOrSpeaking = isSpeaking || aiState is AiResponseState.Thinking || showVoiceOverlay,
                    onSelectTab = { tab ->
                        if (tab == JarvisTab.AI_CORE) {
                            requestedAiCoreSubTab = AiCoreSubTab.MODELS
                        }
                        navController.navigate(tab.route) {
                            popUpTo(JarvisTab.COMMAND.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOrbClick = { showVoiceOverlay = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_BOOT
            ) {
                composable(ROUTE_BOOT) {
                    BootScreen(
                        onBootComplete = {
                            navController.navigate(JarvisTab.COMMAND.route) {
                                popUpTo(ROUTE_BOOT) { inclusive = true }
                            }
                        }
                    )
                }

                composable(JarvisTab.COMMAND.route) {
                    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                        CommandDashboardScreen(
                            settings = settings,
                            telemetry = telemetry,
                            skills = skills,
                            flashlightOn = flashlightOn,
                            isSpeaking = isSpeaking,
                            statusBanner = statusBanner,
                            onOpenVoiceOverlay = { showVoiceOverlay = true },
                            onToggleFpsBoost = viewModel::triggerFpsBoost,
                            onToggleFlashlight = viewModel::toggleFlashlight,
                            onQuickYouTubeSearch = viewModel::quickYouTubeSearch,
                            onNavigateToVoiceLab = {
                                requestedAiCoreSubTab = AiCoreSubTab.VOICE_LAB
                                navController.navigate(JarvisTab.AI_CORE.route) {
                                    popUpTo(JarvisTab.COMMAND.route) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToSkills = {
                                requestedAiCoreSubTab = AiCoreSubTab.SKILLS
                                navController.navigate(JarvisTab.AI_CORE.route) {
                                    popUpTo(JarvisTab.COMMAND.route) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToProfile = {
                                navController.navigate(JarvisTab.PROFILE.route) {
                                    popUpTo(JarvisTab.COMMAND.route) { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }

                composable(JarvisTab.CHAT.route) {
                    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                        ChatScreen(
                            messages = messages,
                            aiState = aiState,
                            onSendMessage = viewModel::sendCommand,
                            onSpeakText = { text -> viewModel.voiceEngine.speakHindi(text) },
                            onOpenVoiceOverlay = { showVoiceOverlay = true },
                            onClearChat = viewModel::clearChatHistory
                        )
                    }
                }

                composable(JarvisTab.AI_CORE.route) {
                    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                        AiCoreScreen(
                            initialSubTab = requestedAiCoreSubTab,
                            settings = settings,
                            skills = skills,
                            isPlayingSample = isPlayingSample,
                            onSelectModel = viewModel::setSelectedModel,
                            onSelectBehavior = viewModel::setModelBehavior,
                            onVoicePitchChange = viewModel::setVoicePitch,
                            onVoiceSpeedChange = viewModel::setVoiceSpeed,
                            onVoiceAccentSelect = { accent ->
                                viewModel.setVoiceAccent(accent, applyRecommendedTuning = true)
                            },
                            onAutoSpeakToggle = viewModel::setAutoSpeak,
                            onTestJarvisVoice = viewModel::speakVoiceTest,
                            onToggleReferenceSample = viewModel.voiceEngine::toggleReferenceVoiceSample,
                            onLinkPlatform = viewModel::linkPlatformSkill,
                            onUnlinkSkill = viewModel::unlinkSkill,
                            onToggleSkillEnabled = viewModel::toggleSkillEnabled,
                            onAddCustomSkill = viewModel::addCustomSkill,
                            onTestSkill = viewModel::testSkill,
                            onDeleteCustomSkill = viewModel::deleteCustomSkill,
                            onExportManifest = viewModel::exportSkillsManifestJson,
                            onImportManifest = viewModel::importSkillManifestJson
                        )
                    }
                }

                composable(JarvisTab.PROFILE.route) {
                    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                        ProfileScreen(
                            settings = settings,
                            skills = skills,
                            installedApps = installedApps,
                            onUpdateCommanderName = viewModel::setCommanderName,
                            onOpenVoiceLab = {
                                requestedAiCoreSubTab = AiCoreSubTab.VOICE_LAB
                                navController.navigate(JarvisTab.AI_CORE.route) {
                                    popUpTo(JarvisTab.COMMAND.route) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onOpenSkillsHub = {
                                requestedAiCoreSubTab = AiCoreSubTab.SKILLS
                                navController.navigate(JarvisTab.AI_CORE.route) {
                                    popUpTo(JarvisTab.COMMAND.route) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onLaunchApp = viewModel::launchInstalledApp
                        )
                    }
                }
            }
        }
    }

    if (showVoiceOverlay) {
        VoiceCommandOverlay(
            aiState = aiState,
            isSpeaking = isSpeaking,
            onDismiss = { showVoiceOverlay = false },
            onSubmitCommand = { cmd -> viewModel.sendCommand(cmd, activity) },
            onStopSpeaking = { viewModel.voiceEngine.stopSpeaking() }
        )
    }
}
