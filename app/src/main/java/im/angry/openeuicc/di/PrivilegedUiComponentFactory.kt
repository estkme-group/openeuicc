package im.angry.openeuicc.di

import im.angry.openeuicc.ui.EuiccManagementFragment
import im.angry.openeuicc.ui.PrivilegedEuiccManagementFragment

class PrivilegedUiComponentFactory : DefaultUiComponentFactory() {
    override fun createEuiccManagementFragment(slotId: Int, portId: Int): EuiccManagementFragment =
        PrivilegedEuiccManagementFragment.newInstance(slotId, portId)
}