package im.angry.openeuicc.di

import androidx.fragment.app.Fragment
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.ui.EuiccManagementFragment
import im.angry.openeuicc.ui.UnprivilegedEuiccManagementFragment
import im.angry.openeuicc.ui.UnprivilegedNoEuiccPlaceholderFragment

class UnprivilegedUiComponentFactory : DefaultUiComponentFactory() {
    override fun createEuiccManagementFragment(channel: EuiccChannel): EuiccManagementFragment =
        UnprivilegedEuiccManagementFragment.newInstance(channel.slotId, channel.portId)

    override fun createNoEuiccPlaceholderFragment(): Fragment =
        UnprivilegedNoEuiccPlaceholderFragment()
}