package im.angry.openeuicc.di

import androidx.fragment.app.Fragment
import im.angry.openeuicc.ui.EuiccManagementFragment
import im.angry.openeuicc.ui.QuickCompatibilityFragment
import im.angry.openeuicc.ui.UnprivilegedEuiccManagementFragment
import im.angry.openeuicc.ui.UnprivilegedNoEuiccPlaceholderFragment
import im.angry.openeuicc.ui.UnprivilegedSettingsFragment

open class UnprivilegedUiComponentFactory : DefaultUiComponentFactory() {
    override fun createEuiccManagementFragment(slotId: Int, portId: Int): EuiccManagementFragment =
        UnprivilegedEuiccManagementFragment.newInstance(slotId, portId)

    override fun createNoEuiccPlaceholderFragment(): Fragment =
        UnprivilegedNoEuiccPlaceholderFragment()

    override fun createSettingsFragment(): Fragment =
        UnprivilegedSettingsFragment()

    open fun createQuickCompatibilityFragment(): Fragment =
        QuickCompatibilityFragment()
}