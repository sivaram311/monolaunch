package buzz.delena.monolaunch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import buzz.delena.monolaunch.model.AppInfo
import buzz.delena.monolaunch.ui.AppDrawerScreen
import buzz.delena.monolaunch.ui.HomeScreen
import buzz.delena.monolaunch.ui.theme.MonolaunchTheme
import buzz.delena.monolaunch.viewmodel.LauncherScreen
import buzz.delena.monolaunch.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.uiState.value.screen == LauncherScreen.DRAWER) {
                viewModel.showHome()
            } else {
                moveTaskToBack(true)
            }
        }

        setContent {
            MonolaunchTheme {
                LauncherApp(viewModel)
            }
        }
    }
}

@Composable
private fun LauncherApp(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isDragging by remember { mutableStateOf(false) }

    val activity = context as? Activity
    LaunchedEffect(uiState.isScreenAlwaysOn) {
        activity?.runOnUiThread {
            if (uiState.isScreenAlwaysOn) {
                activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    var totalHomeDragDown by remember { mutableStateOf(0f) }
    var hasTriggeredNotificationSwipe by remember { mutableStateOf(false) }

    var showAboutDialog by remember { mutableStateOf(false) }
    var selectedAppForActions by remember { mutableStateOf<AppInfo?>(null) }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spokenText.isNotEmpty()) {
                viewModel.onSearchQueryChange(spokenText)
                viewModel.openDrawer()
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.dragProgress,
        animationSpec = if (isDragging) snap() else spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "home-drawer-transition",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            isDragging = true
                            totalHomeDragDown = 0f
                            hasTriggeredNotificationSwipe = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val current = viewModel.uiState.value.dragProgress
                            if (current == 0f && dragAmount > 0f) {
                                totalHomeDragDown += dragAmount
                                if (totalHomeDragDown > 80f && !hasTriggeredNotificationSwipe) {
                                    viewModel.expandNotifications(context)
                                    hasTriggeredNotificationSwipe = true
                                }
                            } else {
                                viewModel.updateDragProgress(current - dragAmount / heightPx)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            if (!hasTriggeredNotificationSwipe) {
                                viewModel.settleDrag(viewModel.uiState.value.dragProgress > 0.3f)
                            } else {
                                viewModel.settleDrag(false)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            viewModel.settleDrag(viewModel.uiState.value.screen == LauncherScreen.DRAWER)
                        },
                    )
                },
        ) {
            HomeScreen(
                uiState = uiState,
                onOpenDrawer = viewModel::openDrawer,
                onLaunchApp = viewModel::launchApp,
                onVoiceSearch = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_search_prompt))
                    }
                    runCatching {
                        voiceSearchLauncher.launch(intent)
                    }.onFailure {
                        Toast.makeText(context, R.string.voice_search_prompt, Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClickClock = {
                    showAboutDialog = true
                },
                onToggleScreenAlwaysOn = {
                    viewModel.toggleScreenAlwaysOn(context)
                },
                modifier = Modifier.graphicsLayer { translationY = -animatedProgress * heightPx },
            )
            AppDrawerScreen(
                uiState = uiState,
                onQueryChange = viewModel::onSearchQueryChange,
                onLaunchApp = viewModel::launchApp,
                onLongClickApp = { app ->
                    selectedAppForActions = app
                },
                onClose = viewModel::showHome,
                modifier = Modifier.graphicsLayer { translationY = (1f - animatedProgress) * heightPx },
            )
        }
    }

    if (showAboutDialog) {
        val (versionName, versionCode) = viewModel.getVersionInfo(context)
        AboutDialog(
            versionName = versionName,
            versionCode = versionCode,
            onDismiss = { showAboutDialog = false },
            onSetDefault = {
                viewModel.requestDefaultLauncher(context)
                showAboutDialog = false
            }
        )
    }

    selectedAppForActions?.let { app ->
        AppActionsDialog(
            app = app,
            onDismiss = { selectedAppForActions = null },
            onOpenInfo = { viewModel.openAppSettings(app, context) },
            onUninstall = { viewModel.uninstallApp(app, context) }
        )
    }
}

@Composable
private fun AboutDialog(
    versionName: String,
    versionCode: Int,
    onDismiss: () -> Unit,
    onSetDefault: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Monolaunch", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(text = "An ultra-minimalist AMOLED launcher.", color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Version: $versionName ($versionCode)", color = Color.Gray, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onSetDefault) {
                Text("Set Default", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = Color.Black,
        modifier = Modifier.border(BorderStroke(1.dp, Color.White))
    )
}

@Composable
private fun AppActionsDialog(
    app: AppInfo,
    onDismiss: () -> Unit,
    onOpenInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = app.label, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Text(text = "Actions for package: ${app.packageName}", color = Color.Gray, fontSize = 12.sp)
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    onOpenInfo()
                    onDismiss()
                }) {
                    Text("App Info", color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    onUninstall()
                    onDismiss()
                }) {
                    Text("Uninstall", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Color.Black,
        modifier = Modifier.border(BorderStroke(1.dp, Color.White))
    )
}
