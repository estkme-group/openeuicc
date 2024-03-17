package im.angry.openeuicc.di

import android.content.Context

class UnprivilegedAppContainer(context: Context) : DefaultAppContainer(context) {
    override val uiComponentFactory by lazy {
        UnprivilegedUiComponentFactory()
    }
}