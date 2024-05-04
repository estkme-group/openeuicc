package im.angry.openeuicc.core

import android.app.Service

interface EuiccChannelManagerFactory {
    fun createEuiccChannelManager(serviceContext: Service): EuiccChannelManager
}