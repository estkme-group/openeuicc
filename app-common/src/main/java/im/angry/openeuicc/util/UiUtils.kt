package im.angry.openeuicc.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.telephony.TelephonyManager
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import im.angry.openeuicc.OpenEuiccApplication
import im.angry.openeuicc.core.EuiccChannelManager

interface OpenEuiccUIContextMarker

val OpenEuiccUIContextMarker.context: Context
    get() = when (this) {
        is Context -> this
        is Fragment -> requireContext()
        else -> throw RuntimeException("OpenEuiccUIContextMarker shall only be used on Fragments or UI types that derive from Context")
    }

val OpenEuiccUIContextMarker.openEuiccApplication: OpenEuiccApplication
    get() = context.applicationContext as OpenEuiccApplication

val OpenEuiccUIContextMarker.euiccChannelManager: EuiccChannelManager
    get() = openEuiccApplication.euiccChannelManager

val OpenEuiccUIContextMarker.telephonyManager: TelephonyManager
    get() = openEuiccApplication.telephonyManager

// Source: <https://stackoverflow.com/questions/12478520/how-to-set-dialogfragments-width-and-height>
/**
 * Call this method (in onActivityCreated or later) to set
 * the width of the dialog to a percentage of the current
 * screen width.
 */
fun DialogFragment.setWidthPercent(percentage: Int) {
    val percent = percentage.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * percent
    dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
}

/**
 * Call this method (in onActivityCreated or later)
 * to make the dialog near-full screen.
 */
fun DialogFragment.setFullScreen() {
    dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}
