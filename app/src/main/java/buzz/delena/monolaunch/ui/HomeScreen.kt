package buzz.delena.monolaunch.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import buzz.delena.monolaunch.R
import buzz.delena.monolaunch.model.AppInfo
import buzz.delena.monolaunch.ui.components.AnalogClock
import buzz.delena.monolaunch.viewmodel.LauncherUiState
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Home: clock + date + battery, a 4-icon primary row, and the floating
 * search pill that opens the App Drawer. Pure black background — no
 * wallpaper, no blur, flat surfaces only (AMOLED + 120Hz budget).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: LauncherUiState,
    onOpenDrawer: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onVoiceSearch: () -> Unit,
    onLongClickClock: () -> Unit,
    onToggleScreenAlwaysOn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        ClockAndStatus(
            batteryPercent = uiState.batteryPercent,
            onLongClickClock = onLongClickClock,
            isScreenAlwaysOn = uiState.isScreenAlwaysOn,
            onToggleScreenAlwaysOn = onToggleScreenAlwaysOn,
            xauusdPrice = uiState.xauusdPrice
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryAppRow(
            phone = uiState.primaryApps.phone,
            messages = uiState.primaryApps.messages,
            camera = uiState.primaryApps.camera,
            settings = uiState.primaryApps.settings,
            onLaunchApp = onLaunchApp,
        )

        Spacer(modifier = Modifier.height(24.dp))
        SearchPill(onOpenDrawer = onOpenDrawer, onVoiceSearch = onVoiceSearch)
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClockAndStatus(
    batteryPercent: Int,
    onLongClickClock: () -> Unit,
    isScreenAlwaysOn: Boolean,
    onToggleScreenAlwaysOn: () -> Unit,
    xauusdPrice: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClickClock
            )
    ) {
        AnalogClock(size = 128.dp)
        Spacer(modifier = Modifier.height(16.dp))
        val dateText = SimpleDateFormat("EEE dd MMM", Locale.getDefault())
            .format(java.util.Date())
            .uppercase(Locale.getDefault())
        val batteryContentDescription = stringResource(R.string.cd_battery, batteryPercent)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dateText,
                color = Color.White,
                fontSize = 14.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "  ·  ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
            Text(
                text = "$batteryPercent%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.semantics {
                    contentDescription = batteryContentDescription
                },
            )
            Text(
                text = "  ·  ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
            Icon(
                imageVector = if (isScreenAlwaysOn) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                contentDescription = "Always on",
                tint = if (isScreenAlwaysOn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onToggleScreenAlwaysOn() }
            )
        }
        if (xauusdPrice.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = xauusdPrice,
                color = Color.LightGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun PrimaryAppRow(
    phone: AppInfo?,
    messages: AppInfo?,
    camera: AppInfo?,
    settings: AppInfo?,
    onLaunchApp: (AppInfo) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        PrimaryIcon(icon = Icons.Outlined.Call, contentDescription = stringResource(R.string.cd_phone), app = phone, onLaunchApp = onLaunchApp)
        PrimaryIcon(icon = Icons.Outlined.Sms, contentDescription = stringResource(R.string.cd_messages), app = messages, onLaunchApp = onLaunchApp)
        PrimaryIcon(icon = Icons.Outlined.PhotoCamera, contentDescription = stringResource(R.string.cd_camera), app = camera, onLaunchApp = onLaunchApp)
        PrimaryIcon(icon = Icons.Outlined.Settings, contentDescription = stringResource(R.string.cd_settings), app = settings, onLaunchApp = onLaunchApp)
    }
}

@Composable
private fun RowScope.PrimaryIcon(
    icon: ImageVector,
    contentDescription: String,
    app: AppInfo?,
    onLaunchApp: (AppInfo) -> Unit,
) {
    val enabled = app != null
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .let { if (enabled) it.clickable { onLaunchApp(app!!) } else it },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun SearchPill(onOpenDrawer: () -> Unit, onVoiceSearch: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onOpenDrawer),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { /* reserved for a future add-shortcut Bolt, see docs/aidlc/BOLTS.md */ }) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_shortcut),
                    tint = Color.White,
                )
            }
            Text(
                text = stringResource(R.string.search_apps_placeholder),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onVoiceSearch() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = stringResource(R.string.voice_search),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
