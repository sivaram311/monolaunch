package buzz.delena.monolaunch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import buzz.delena.monolaunch.ui.AppDrawerScreen
import buzz.delena.monolaunch.ui.HomeScreen
import buzz.delena.monolaunch.ui.theme.MonolaunchTheme
import buzz.delena.monolaunch.viewmodel.LauncherScreen
import buzz.delena.monolaunch.viewmodel.LauncherViewModel

/**
 * Sole activity. Registered as a HOME app in AndroidManifest.xml. The
 * system back gesture never finishes a launcher — it returns to Home
 * (from Drawer) or backgrounds the task (from Home), matching stock
 * launcher behavior.
 */
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isDragging by remember { mutableStateOf(false) }

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
                        onDragStart = { isDragging = true },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val current = viewModel.uiState.value.dragProgress
                            viewModel.updateDragProgress(current - dragAmount / heightPx)
                        },
                        onDragEnd = {
                            isDragging = false
                            viewModel.settleDrag(viewModel.uiState.value.dragProgress > 0.3f)
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
                modifier = Modifier.graphicsLayer { translationY = -animatedProgress * heightPx },
            )
            AppDrawerScreen(
                uiState = uiState,
                onQueryChange = viewModel::onSearchQueryChange,
                onLaunchApp = viewModel::launchApp,
                onClose = viewModel::showHome,
                modifier = Modifier.graphicsLayer { translationY = (1f - animatedProgress) * heightPx },
            )
        }
    }
}
