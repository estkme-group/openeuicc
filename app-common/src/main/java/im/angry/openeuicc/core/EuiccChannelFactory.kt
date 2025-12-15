package im.angry.openeuicc.core

import im.angry.openeuicc.core.usb.UsbCcidContext
import im.angry.openeuicc.util.*

// This class is here instead of inside DI because it contains a bit more logic than just
// "dumb" dependency injection.
interface EuiccChannelFactory {
    suspend fun tryOpenEuiccChannel(
        port: UiccPortInfoCompat,
        isdrAid: ByteArray,
        seId: EuiccChannel.SecureElementId
    ): EuiccChannel?

    fun tryOpenUsbEuiccChannel(
        ccidCtx: UsbCcidContext,
        isdrAid: ByteArray,
        seId: EuiccChannel.SecureElementId
    ): EuiccChannel?

    /**
     * Release all resources used by this EuiccChannelFactory
     * Note that the same instance may be reused; any resources allocated must be automatically
     * re-acquired when this happens
     */
    fun cleanup()
}
