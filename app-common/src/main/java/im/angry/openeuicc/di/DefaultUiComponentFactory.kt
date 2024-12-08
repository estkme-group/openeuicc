package im.angry.openeuicc.di

import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import im.angry.openeuicc.ui.EuiccManagementFragment
import im.angry.openeuicc.ui.NoEuiccPlaceholderFragment
import im.angry.openeuicc.ui.SettingsFragment

open class DefaultUiComponentFactory : UiComponentFactory {
    override fun createEuiccManagementFragment(slotId: Int, portId: Int): EuiccManagementFragment =
        EuiccManagementFragment.newInstance(slotId, portId)

    override fun createNoEuiccPlaceholderFragment(): Fragment = NoEuiccPlaceholderFragment()

    override fun createSettingsFragment(): Fragment = SettingsFragment()
}