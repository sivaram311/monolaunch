package buzz.delena.monolaunch.viewmodel

import android.app.Application
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import buzz.delena.monolaunch.model.AppInfo
import buzz.delena.monolaunch.model.PrimaryApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LauncherScreen { HOME, DRAWER }

data class LauncherUiState(
    val screen: LauncherScreen = LauncherScreen.HOME,
    /** 0f = resting on Home, 1f = resting on Drawer; interpolated live while dragging. */
    val dragProgress: Float = 0f,
    val allApps: List<AppInfo> = emptyList(),
    val frequentApps: List<AppInfo> = emptyList(),
    val primaryApps: PrimaryApps = PrimaryApps(),
    val searchQuery: String = "",
    val batteryPercent: Int = 0,
    val isLoadingApps: Boolean = true,
)

/**
 * Owns all launcher state: the Home/Drawer transition, the installed-app
 * list, battery level, and app-launch intents. Holds [Application] context
 * only (never an Activity), and unregisters its battery receiver in
 * [onCleared] — the two things that would otherwise leak an Activity.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val packageManager: PackageManager = application.packageManager
    private val prefs = application.getSharedPreferences("monolaunch_prefs", Context.MODE_PRIVATE)

    // Persisted launch tally driving the FREQUENT row.
    private val launchCounts = mutableMapOf<String, Int>().apply {
        prefs.all.forEach { (key, value) ->
            if (value is Int) {
                put(key, value)
            }
        }
    }

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                _uiState.update { it.copy(batteryPercent = level * 100 / scale) }
            }
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            loadApps()
        }
    }

    init {
        ContextCompat.registerReceiver(
            application,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        application.registerReceiver(packageReceiver, packageFilter)

        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true) }
            val apps = withContext(Dispatchers.Default) { queryLaunchableApps() }
            val primary = withContext(Dispatchers.Default) { resolvePrimaryApps(apps) }
            _uiState.update {
                it.copy(
                    allApps = apps,
                    frequentApps = frequentFrom(apps),
                    primaryApps = primary,
                    isLoadingApps = false,
                )
            }
        }
    }

    fun openDrawer() {
        _uiState.update { it.copy(screen = LauncherScreen.DRAWER, dragProgress = 1f) }
    }

    fun showHome() {
        _uiState.update { it.copy(screen = LauncherScreen.HOME, dragProgress = 0f, searchQuery = "") }
    }

    /** Live progress while a vertical drag is in flight; 0f..1f, Home to Drawer. */
    fun updateDragProgress(progress: Float) {
        _uiState.update { it.copy(dragProgress = progress.coerceIn(0f, 1f)) }
    }

    /** Called when a drag gesture ends: settles fully onto whichever screen is closer. */
    fun settleDrag(towardsDrawer: Boolean) {
        if (towardsDrawer) openDrawer() else showHome()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun launchApp(app: AppInfo) {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityClassName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "Unable to launch ${app.componentKey}", it) }
            .isSuccess

        if (launched) {
            val newCount = (launchCounts[app.packageName] ?: 0) + 1
            launchCounts[app.packageName] = newCount
            prefs.edit().putInt(app.packageName, newCount).apply()
            _uiState.update { state ->
                val updatedApps = state.allApps.map {
                    if (it.packageName == app.packageName) it.copy(launchCount = newCount) else it
                }
                state.copy(
                    allApps = updatedApps,
                    frequentApps = frequentFrom(updatedApps),
                    screen = LauncherScreen.HOME,
                    dragProgress = 0f,
                    searchQuery = "",
                )
            }
        }
    }

    private fun frequentFrom(apps: List<AppInfo>): List<AppInfo> =
        apps.filter { it.launchCount > 0 }
            .sortedByDescending { it.launchCount }
            .take(MAX_FREQUENT_APPS)

    private fun queryLaunchableApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val selfPackage = getApplication<Application>().packageName

        val resolveInfos: List<ResolveInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }

        return resolveInfos
            .asSequence()
            .filter { it.activityInfo.packageName != selfPackage }
            .distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name}" }
            .map { info ->
                AppInfo(
                    label = info.loadLabel(packageManager).toString(),
                    packageName = info.activityInfo.packageName,
                    activityClassName = info.activityInfo.name,
                    icon = info.loadIcon(packageManager),
                    launchCount = launchCounts[info.activityInfo.packageName] ?: 0,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun resolvePrimaryApps(all: List<AppInfo>): PrimaryApps {
        fun resolve(intent: Intent): AppInfo? {
            val info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
            val pkg = info.activityInfo.packageName
            return all.firstOrNull { it.packageName == pkg } ?: AppInfo(
                label = info.loadLabel(packageManager).toString(),
                packageName = pkg,
                activityClassName = info.activityInfo.name,
                icon = info.loadIcon(packageManager),
            )
        }
        return PrimaryApps(
            phone = resolve(Intent(Intent.ACTION_DIAL)),
            messages = resolve(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", "", null))),
            camera = resolve(Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
            settings = resolve(Intent(Settings.ACTION_SETTINGS)),
        )
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { getApplication<Application>().unregisterReceiver(batteryReceiver) }
        runCatching { getApplication<Application>().unregisterReceiver(packageReceiver) }
    }

    fun requestDefaultLauncher(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    if (context is android.app.Activity) {
                        context.startActivityForResult(intent, 1001)
                        return
                    } else {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return
                    }
                }
            }
        }
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun expandNotifications(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val method = statusBarManagerClass.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to expand notifications panel", e)
        }
    }

    fun openAppSettings(app: AppInfo, context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun uninstallApp(app: AppInfo, context: Context) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun getVersionInfo(context: Context): Pair<String, Int> {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = pInfo.versionName ?: "0.1.0"
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
            Pair(version, code)
        } catch (e: Exception) {
            Pair("0.1.0", 1)
        }
    }

    private companion object {
        const val TAG = "LauncherViewModel"
        const val MAX_FREQUENT_APPS = 4
    }
}
