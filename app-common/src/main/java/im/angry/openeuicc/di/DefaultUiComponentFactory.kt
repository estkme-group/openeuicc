package im.angry.openeuicc.di

import androidx.fragment.app.Fragment
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.ui.EuiccManagementFragment
import im.angry.openeuicc.ui.NoEuiccPlaceholderFragment

open class DefaultUiComponentFactory : UiComponentFactory {
    override fun createEuiccManagementFragment(channel: EuiccChannel): EuiccManagementFragment =
        EuiccManagementFragment.newInstance(channel.slotId, channel.portId)

    override fun createNoEuiccPlaceholderFragment(): Fragment = NoEuiccPlaceholderFragment()
}