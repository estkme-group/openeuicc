package im.angry.openeuicc.ui

import android.view.Menu
import android.view.MenuInflater
import im.angry.easyeuicc.R
import im.angry.openeuicc.util.SIMToolkit
import im.angry.openeuicc.util.newInstanceEuicc
import im.angry.openeuicc.util.slotId


class UnprivilegedEuiccManagementFragment : EuiccManagementFragment() {
    companion object {
        const val TAG = "UnprivilegedEuiccManagementFragment"

        fun newInstance(slotId: Int, portId: Int): EuiccManagementFragment =
            newInstanceEuicc(UnprivilegedEuiccManagementFragment::class.java, slotId, portId)
    }

    private val stk by lazy {
        SIMToolkit(requireContext())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_sim_toolkit, menu)
        menu.findItem(R.id.open_sim_toolkit).apply {
            isVisible = stk.isAvailable(slotId)
            intent = stk.intent(slotId)
        }
    }
}