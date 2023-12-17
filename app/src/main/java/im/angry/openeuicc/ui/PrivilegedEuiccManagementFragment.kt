package im.angry.openeuicc.ui

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import im.angry.openeuicc.R

class PrivilegedEuiccManagementFragment: EuiccManagementFragment() {
    companion object {
        fun newInstance(slotId: Int, portId: Int): EuiccManagementFragment =
            newInstanceEuicc(PrivilegedEuiccManagementFragment::class.java, slotId, portId)
    }

    override suspend fun onCreateFooterViews(parent: ViewGroup): List<View> =
        if (channel.isMEP) {
            val view = layoutInflater.inflate(R.layout.footer_mep, parent, false)
            view.findViewById<Button>(R.id.footer_mep_slot_mapping).setOnClickListener {
                (requireActivity() as PrivilegedMainActivity).showSlotMappingFragment()
            }
            listOf(view)
        } else {
            listOf()
        }
}