package im.angry.openeuicc.di

import androidx.fragment.app.Fragment
import im.angry.openeuicc.ui.UnprivilegedNoEuiccPlaceholderFragment

class UnprivilegedUiComponentFactory : DefaultUiComponentFactory() {
    override fun createNoEuiccPlaceholderFragment(): Fragment =
        UnprivilegedNoEuiccPlaceholderFragment()
}