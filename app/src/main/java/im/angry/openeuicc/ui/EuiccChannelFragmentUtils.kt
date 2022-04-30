package im.angry.openeuicc.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import im.angry.openeuicc.OpenEUICCApplication
import im.angry.openeuicc.core.EuiccChannel

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

val <T> T.channel: EuiccChannel where T: Fragment, T: EuiccFragmentMarker
    get() =
        (requireActivity().application as OpenEUICCApplication).euiccChannelRepo.availableChannels[slotId]

interface EuiccProfilesChangedListener {
    fun onEuiccProfilesChanged()
}