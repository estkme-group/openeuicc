package im.angry.openeuicc.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.ArrayRes
import im.angry.easyeuicc.R
import im.angry.openeuicc.core.EuiccChannelManager

class SIMToolkit(private val context: Context) {
    private val slotSelection = getComponentNames(R.array.sim_toolkit_slot_selection)

    private val slots = buildMap {
        put(0, getComponentNames(R.array.sim_toolkit_slot_1))
        put(1, getComponentNames(R.array.sim_toolkit_slot_2))
    }

    private val packageNames = buildSet {
        addAll(slotSelection.map { it.packageName })
        addAll(slots.values.flatten().map { it.packageName })
    }

    private val activities = packageNames.flatMap(::getActivities).toSet()

    private val launchIntent by lazy {
        packageNames.firstNotNullOfOrNull(::getLaunchIntent)
    }

    private fun getLaunchIntent(packageName: String) = try {
        val pm = context.packageManager
        pm.getLaunchIntentForPackage(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun getActivities(packageName: String) = try {
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        packageInfo.activities!!.filter { it.exported }
            .map { ComponentName(it.packageName, it.name) }
    } catch (_: PackageManager.NameNotFoundException) {
        emptyList()
    }

    private fun getComponentNames(@ArrayRes id: Int) =
        context.resources.getStringArray(id).mapNotNull(ComponentName::unflattenFromString)

    fun isAvailable(slotId: Int) = when (slotId) {
        -1 -> false
        EuiccChannelManager.USB_CHANNEL_ID -> false
        else -> intent(slotId) != null
    }

    fun intent(slotId: Int): Intent? {
        val components = slots.getOrDefault(slotId, emptySet()) + slotSelection
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            component = components.find(activities::contains)
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return if (intent.component != null) intent else launchIntent
    }
}
