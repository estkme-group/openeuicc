package im.angry.openeuicc.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.openEuiccApplication

interface EuiccFragmentMarker

fun <T> newInstanceEuicc(clazz: Class<T>, slotId: Int): T where T: Fragment, T: EuiccFragmentMarker {
    val instance = clazz.newInstance()
    instance.arguments = Bundle().apply {
        putInt("slotId", slotId)
    }
    return instance
}

val <T> T.slotId: Int where T: Fragment, T: EuiccFragmentMarker
    get() = requireArguments().getInt("slotId")

val <T> T.euiccChannelManager: EuiccChannelManager where T: Fragment, T: EuiccFragmentMarker
    get() = openEuiccApplication.euiccChannelManager

val <T> T.channel: EuiccChannel where T: Fragment, T: EuiccFragmentMarker
    get() =
        euiccChannelManager.findEuiccChannelBySlotBlocking(slotId)!!

interface EuiccProfilesChangedListener {
    fun onEuiccProfilesChanged()
}