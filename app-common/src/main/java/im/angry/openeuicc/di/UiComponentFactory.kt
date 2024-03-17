package im.angry.openeuicc.di

import androidx.fragment.app.Fragment
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.ui.EuiccManagementFragment

interface UiComponentFactory {
    fun createEuiccManagementFragment(channel: EuiccChannel): EuiccManagementFragment
    fun createNoEuiccPlaceholderFragment(): Fragment
}