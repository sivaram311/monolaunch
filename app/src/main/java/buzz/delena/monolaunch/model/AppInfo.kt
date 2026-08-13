package buzz.delena.monolaunch.model

import android.graphics.drawable.Drawable

/**
 * One launchable activity, as resolved from [android.content.pm.PackageManager].
 * [launchCount] is session-scoped (Bolt 2 in docs/aidlc/BOLTS.md persists it).
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityClassName: String,
    val icon: Drawable,
    val launchCount: Int = 0,
) {
    val componentKey: String
        get() = "$packageName/$activityClassName"
}

data class PrimaryApps(
    val phone: AppInfo? = null,
    val messages: AppInfo? = null,
    val camera: AppInfo? = null,
    val settings: AppInfo? = null,
)
