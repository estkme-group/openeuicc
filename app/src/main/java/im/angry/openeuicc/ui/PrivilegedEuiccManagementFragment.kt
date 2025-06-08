package im.angry.openeuicc.ui

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import im.angry.openeuicc.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.*
import net.typeblog.lpac_jni.LocalProfileInfo

class PrivilegedEuiccManagementFragment : EuiccManagementFragment() {
    companion object {
        fun newInstance(
            slotId: Int,
            portId: Int,
            seId: EuiccChannel.SecureElementId
        ): EuiccManagementFragment =
            newInstanceEuicc(PrivilegedEuiccManagementFragment::class.java, slotId, portId, seId)
    }

    private var isMEP = false
    private var isRemovable = false

    override suspend fun onCreateFooterViews(
        parent: ViewGroup,
        profiles: List<LocalProfileInfo>
    ): List<View> =
        super.onCreateFooterViews(parent, profiles).let { footers ->
            withEuiccChannel { channel ->
                isMEP = channel.isMEP
                isRemovable = channel.port.card.isRemovable
            }

            if (isMEP) {
                val view = layoutInflater.inflate(R.layout.footer_mep, parent, false)
                view.requireViewById<Button>(R.id.footer_mep_slot_mapping).setOnClickListener {
                    (requireActivity() as PrivilegedMainActivity).showSlotMappingFragment()
                }
                footers + view
            } else {
                footers
            }
        }

    override fun populatePopupWithProfileActions(popup: PopupMenu, profile: LocalProfileInfo) {
        super.populatePopupWithProfileActions(popup, profile)
        if (profile.isEnabled && !isRemovable) {
            // Only show the disable option for non-removable eUICCs
            // Some devices without internal eUICCs have the "optimization" of ignoring SIM
            // slots without a valid profile. This can lead to "bricking" of external eUICCs
            // at least for that specific device.
            // TODO: Maybe we can still make this option available in some sort of "advanced" mode.
            popup.menu.findItem(im.angry.openeuicc.common.R.id.disable).isVisible = true
        }
    }
}