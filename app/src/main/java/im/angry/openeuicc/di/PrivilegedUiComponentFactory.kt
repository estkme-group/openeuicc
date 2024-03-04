package im.angry.openeuicc.di

import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.ui.EuiccManagementFragment
import im.angry.openeuicc.ui.PrivilegedEuiccManagementFragment

class PrivilegedUiComponentFactory : DefaultUiComponentFactory() {
    override fun createEuiccManagementFragment(channel: EuiccChannel): EuiccManagementFragment =
        PrivilegedEuiccManagementFragment.newInstance(channel.slotId, channel.portId)
}