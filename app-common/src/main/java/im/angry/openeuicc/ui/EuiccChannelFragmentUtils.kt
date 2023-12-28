package im.angry.openeuicc.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.openEuiccApplication

interface EuiccFragmentMarker

fun <T> newInstanceEuicc(clazz: Class<T>, slotId: Int, portId: Int): T where T: Fragment, T: EuiccFragmentMarker {
    val instance = clazz.newInstance()
    instance.arguments = Bundle().apply {
        putInt("slotId", slotId)
        putInt("portId", portId)
    }
    return instance
}

val <T> T.slotId: Int where T: Fragment, T: EuiccFragmentMarker
    get() = requireArguments().getInt("slotId")
val <T> T.portId: Int where T: Fragment, T: EuiccFragmentMarker
    get() = requireArguments().getInt("portId")

val <T> T.euiccChannelManager: EuiccChannelManager where T: Fragment, T: EuiccFragmentMarker
    get() = openEuiccApplication.euiccChannelManager

val <T> T.channel: EuiccChannel where T: Fragment, T: EuiccFragmentMarker
    get() =
        euiccChannelManager.findEuiccChannelByPortBlocking(slotId, portId)!!

interface EuiccProfilesChangedListener {
    fun onEuiccProfilesChanged()
}