package im.angry.openeuicc.di

import android.content.Context
import im.angry.openeuicc.util.*

class UnprivilegedAppContainer(context: Context) : DefaultAppContainer(context) {
    override val uiComponentFactory by lazy {
        UnprivilegedUiComponentFactory()
    }

    override val customizableTextProvider by lazy {
        UnprivilegedCustomizableTextProvider(context)
    }

    override val preferenceRepository by lazy {
        UnprivilegedPreferenceRepository(context)
    }
}
