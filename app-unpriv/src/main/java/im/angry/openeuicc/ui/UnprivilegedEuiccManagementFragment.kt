package im.angry.openeuicc.ui

import android.content.pm.PackageManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import im.angry.easyeuicc.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.SIMToolkit
import im.angry.openeuicc.util.newInstanceEuicc
import im.angry.openeuicc.util.slotId


class UnprivilegedEuiccManagementFragment : EuiccManagementFragment() {
    companion object {
        const val TAG = "UnprivilegedEuiccManagementFragment"

        fun newInstance(
            slotId: Int,
            portId: Int,
            seId: EuiccChannel.SecureElementId
        ): EuiccManagementFragment =
            newInstanceEuicc(UnprivilegedEuiccManagementFragment::class.java, slotId, portId, seId)
    }

    private val stk by lazy {
        SIMToolkit(requireContext())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_sim_toolkit, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.open_sim_toolkit).apply {
            intent = stk[slotId]?.intent
            isVisible = intent != null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.open_sim_toolkit -> {
            SIMToolkit.getDisabledPackageName(item.intent)?.also { packageName ->
                val label = requireContext().packageManager.getApplicationLabel(packageName)
                val message = getString(R.string.toast_prompt_to_enable_sim_toolkit, label)
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
            super.onOptionsItemSelected(item) // handling intent
        }

        else -> super.onOptionsItemSelected(item)
    }
}

private fun PackageManager.getApplicationLabel(packageName: String): CharSequence =
    getApplicationLabel(getApplicationInfo(packageName, 0))
