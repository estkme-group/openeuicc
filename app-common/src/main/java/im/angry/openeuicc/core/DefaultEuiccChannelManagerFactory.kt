package im.angry.openeuicc.core

import android.app.Service
import im.angry.openeuicc.di.AppContainer

class DefaultEuiccChannelManagerFactory(private val appContainer: AppContainer) :
    EuiccChannelManagerFactory {
    override fun createEuiccChannelManager(serviceContext: Service) =
        DefaultEuiccChannelManager(appContainer, serviceContext)
}