package im.angry.openeuicc.di

import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.ui.EuiccManagementFragment

interface UiComponentFactory {
    fun createEuiccManagementFragment(channel: EuiccChannel): EuiccManagementFragment
}