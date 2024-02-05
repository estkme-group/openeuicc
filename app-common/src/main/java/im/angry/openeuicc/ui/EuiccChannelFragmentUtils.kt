package im.angry.openeuicc.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.*

interface EuiccFragmentMarker: OpenEuiccContextMarker

fun <T> newInstanceEuicc(clazz: Class<T>, slotId: Int, portId: Int, addArguments: Bundle.() -> Unit = {}): T where T: Fragment, T: EuiccFragmentMarker {
    val instance = clazz.newInstance()
    instance.arguments = Bundle().apply {
        putInt("slotId", slotId)
        putInt("portId", portId)
        addArguments()
    }
    return instance
}

val <T> T.slotId: Int where T: Fragment, T: EuiccFragmentMarker
    get() = requireArguments().getInt("slotId")
val <T> T.portId: Int where T: Fragment, T: EuiccFragmentMarker
    get() = requireArguments().getInt("portId")

val <T> T.channel: EuiccChannel where T: Fragment, T: EuiccFragmentMarker
    get() =
        euiccChannelManager.findEuiccChannelByPortBlocking(slotId, portId)!!

interface EuiccProfilesChangedListener {
    fun onEuiccProfilesChanged()
}