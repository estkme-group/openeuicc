package im.angry.openeuicc.ui

import android.content.pm.PackageManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.widget.Toast
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
            val slot = stk[slotId] ?: return@apply
            isVisible = slot.intent != null
            setOnMenuItemClickListener {
                val intent = slot.intent ?: return@setOnMenuItemClickListener false
                if (intent.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                    val packageName = intent.data!!.schemeSpecificPart
                    val label = requireContext().packageManager.getApplicationLabel(packageName)
                    val message = requireContext().getString(R.string.toast_prompt_to_enable_sim_toolkit, label)
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                startActivity(intent)
                true
            }
        }
    }
}

private fun PackageManager.getApplicationLabel(packageName: String): CharSequence =
    getApplicationLabel(getApplicationInfo(packageName, 0))
