package im.angry.openeuicc

import android.app.Application
import com.google.android.material.color.DynamicColors
import im.angry.openeuicc.di.AppContainer
import im.angry.openeuicc.di.DefaultAppContainer

open class OpenEuiccApplication : Application() {
    open val appContainer: AppContainer by lazy {
        DefaultAppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Observe dynamic colors changes
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}