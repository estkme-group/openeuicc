package im.angry.openeuicc.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.ArrayRes
import im.angry.easyeuicc.R
import im.angry.openeuicc.core.EuiccChannelManager

class SIMToolkit(private val context: Context) {
    private val slots = buildMap {
        fun getComponentNames(@ArrayRes id: Int) = context.resources
            .getStringArray(id).mapNotNull(ComponentName::unflattenFromString)
        put(-1, getComponentNames(R.array.sim_toolkit_slot_selection))
        put(0, getComponentNames(R.array.sim_toolkit_slot_1))
        put(1, getComponentNames(R.array.sim_toolkit_slot_2))
    }

    operator fun get(slotId: Int): Slot? = when (slotId) {
        -1, EuiccChannelManager.USB_CHANNEL_ID -> null
        else -> Slot(context.packageManager, buildSet {
            addAll(slots.getOrDefault(slotId, emptySet()))
            addAll(slots.getOrDefault(-1, emptySet()))
        })
    }

    data class Slot(private val packageManager: PackageManager, private val components: Set<ComponentName>) {
        private val packageNames: Iterable<String>
            get() = components.map(ComponentName::getPackageName).toSet()

        private val launchIntent: Intent?
            get() = packageNames.firstNotNullOfOrNull(packageManager::getLaunchIntent)

        private val activities: Iterable<ComponentName>
            get() = packageNames.flatMap(packageManager::getActivities)
                .filter(ActivityInfo::exported).map { ComponentName(it.packageName, it.name) }

        private fun getActivityIntent(): Intent? {
            for (activity in activities) {
                if (!components.contains(activity)) continue
                if (isDisabledState(packageManager.getComponentEnabledSetting(activity))) continue
                return Intent.makeMainActivity(activity)
            }
            return launchIntent
        }

        private fun getDisabledPackageIntent(): Intent? {
            val disabledPackageName = packageNames.find {
                try {
                    isDisabledState(packageManager.getApplicationEnabledSetting(it))
                } catch (_: IllegalArgumentException) {
                    false
                }
            }
            if (disabledPackageName == null) return null
            return Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", disabledPackageName, null)
            )
        }

        val intent: Intent?
            get() = getActivityIntent() ?: getDisabledPackageIntent()
    }
}

private fun isDisabledState(state: Int) = when (state) {
    PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> true
    PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER -> true
    else -> false
}

private fun PackageManager.getLaunchIntent(packageName: String) = try {
    getLaunchIntentForPackage(packageName)
} catch (_: PackageManager.NameNotFoundException) {
    null
}

private fun PackageManager.getActivities(packageName: String) = try {
    getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        .activities?.toList() ?: emptyList()
} catch (_: PackageManager.NameNotFoundException) {
    emptyList()
}
