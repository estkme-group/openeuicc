package im.angry.openeuicc.di

import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.ui.EuiccManagementFragment

open class DefaultUiComponentFactory : UiComponentFactory {
    override fun createEuiccManagementFragment(channel: EuiccChannel): EuiccManagementFragment =
        EuiccManagementFragment.newInstance(channel.slotId, channel.portId)
}