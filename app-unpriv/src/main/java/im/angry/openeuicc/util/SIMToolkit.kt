package im.angry.openeuicc.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException

object SIMToolkit {
    private val slot1activities = arrayOf(
        ComponentName("com.android.stk", "com.android.stk.StkMain1"),
    )

    private val slot2activities = arrayOf(
        ComponentName("com.android.stk", "com.android.stk.StkMain2"),
    )

    fun getComponentName(context: Context, slotId: Int): ComponentName? {
        val components = when (slotId) {
            0 -> slot1activities
            1 -> slot2activities
            else -> return null
        }
        return components.find {
            try {
                context.packageManager.getActivityIcon(it)
                true
            } catch (_: NameNotFoundException) {
                false
            }
        }
    }

    fun intent(context: Context, slotId: Int) = Intent(Intent.ACTION_MAIN, null).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        component = getComponentName(context, slotId)
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
}
