package im.angry.openeuicc.util

import android.os.Bundle
import androidx.fragment.app.Fragment
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.service.EuiccChannelManagerService
import im.angry.openeuicc.ui.BaseEuiccAccessActivity

private const val FIELD_SLOT_ID = "slotId"
private const val FIELD_PORT_ID = "portId"

interface EuiccChannelFragmentMarker : OpenEuiccContextMarker

private typealias BundleSetter = Bundle.() -> Unit

// We must use extension functions because there is no way to add bounds to the type of "self"
// in the definition of an interface, so the only way is to limit where the extension functions
// can be applied.
fun <T> newInstanceEuicc(clazz: Class<T>, slotId: Int, portId: Int, addArguments: BundleSetter = {}): T
        where T : Fragment, T : EuiccChannelFragmentMarker =
    clazz.getDeclaredConstructor().newInstance().apply {
        arguments = Bundle()
        arguments!!.putInt(FIELD_SLOT_ID, slotId)
        arguments!!.putInt(FIELD_PORT_ID, portId)
        arguments!!.addArguments()
    }

// Convenient methods to avoid using `channel` for these
// `channel` requires that the channel actually exists in EuiccChannelManager, which is
// not always the case during operations such as switching
val <T> T.slotId: Int
        where T : Fragment, T : EuiccChannelFragmentMarker
    get() = requireArguments().getInt(FIELD_SLOT_ID)
val <T> T.portId: Int
        where T : Fragment, T : EuiccChannelFragmentMarker
    get() = requireArguments().getInt(FIELD_PORT_ID)
val <T> T.isUsb: Boolean
        where T : Fragment, T : EuiccChannelFragmentMarker
    get() = slotId == EuiccChannelManager.USB_CHANNEL_ID

private fun <T> T.requireEuiccActivity(): BaseEuiccAccessActivity
        where T : Fragment, T : OpenEuiccContextMarker =
    requireActivity() as BaseEuiccAccessActivity

val <T> T.euiccChannelManager: EuiccChannelManager
        where T : Fragment, T : OpenEuiccContextMarker
    get() = requireEuiccActivity().euiccChannelManager

val <T> T.euiccChannelManagerService: EuiccChannelManagerService
        where T : Fragment, T : OpenEuiccContextMarker
    get() = requireEuiccActivity().euiccChannelManagerService

suspend fun <T, R> T.withEuiccChannel(fn: suspend (EuiccChannel) -> R): R
        where T : Fragment, T : EuiccChannelFragmentMarker {
    ensureEuiccChannelManager()
    return euiccChannelManager.withEuiccChannel(slotId, portId, fn)
}

suspend fun <T> T.ensureEuiccChannelManager() where T : Fragment, T : OpenEuiccContextMarker =
    requireEuiccActivity().euiccChannelManagerLoaded.await()

fun <T> T.notifyEuiccProfilesChanged() where T : Fragment {
    if (this !is EuiccProfilesChangedListener) return
    // Trigger a refresh in the parent fragment -- it should wait until
    // any foreground task is completed before actually doing a refresh
    this.onEuiccProfilesChanged()
}

interface EuiccProfilesChangedListener {
    fun onEuiccProfilesChanged()
}