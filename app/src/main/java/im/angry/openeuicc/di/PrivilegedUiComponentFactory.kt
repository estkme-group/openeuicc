package im.angry.openeuicc.di

import androidx.fragment.app.Fragment
import im.angry.openeuicc.ui.EuiccManagementFragment
import im.angry.openeuicc.ui.PrivilegedEuiccManagementFragment
import im.angry.openeuicc.ui.PrivilegedSettingsFragment

class PrivilegedUiComponentFactory : DefaultUiComponentFactory() {
    override fun createEuiccManagementFragment(slotId: Int, portId: Int): EuiccManagementFragment =
        PrivilegedEuiccManagementFragment.newInstance(slotId, portId)

    override fun createSettingsFragment(): Fragment =
        PrivilegedSettingsFragment()
}