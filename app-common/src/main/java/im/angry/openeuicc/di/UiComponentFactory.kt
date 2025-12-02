package im.angry.openeuicc.di

import androidx.fragment.app.Fragment
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.ui.EuiccManagementFragment

interface UiComponentFactory {
    fun createEuiccManagementFragment(
        slotId: Int,
        portId: Int,
        seId: EuiccChannel.SecureElementId
    ): EuiccManagementFragment

    fun createNoEuiccPlaceholderFragment(): Fragment
    fun createSettingsFragment(): Fragment
}
