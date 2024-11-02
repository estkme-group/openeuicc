package im.angry.openeuicc.di

import androidx.fragment.app.Fragment
import im.angry.openeuicc.ui.EuiccManagementFragment
import im.angry.openeuicc.ui.NoEuiccPlaceholderFragment

open class DefaultUiComponentFactory : UiComponentFactory {
    override fun createEuiccManagementFragment(slotId: Int, portId: Int): EuiccManagementFragment =
        EuiccManagementFragment.newInstance(slotId, portId)

    override fun createNoEuiccPlaceholderFragment(): Fragment = NoEuiccPlaceholderFragment()
}