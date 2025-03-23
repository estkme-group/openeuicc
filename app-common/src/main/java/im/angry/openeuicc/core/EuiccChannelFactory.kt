package im.angry.openeuicc.core

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import im.angry.openeuicc.util.*

// This class is here instead of inside DI because it contains a bit more logic than just
// "dumb" dependency injection.
interface EuiccChannelFactory {
    suspend fun tryOpenEuiccChannel(port: UiccPortInfoCompat, isdrAid: ByteArray): EuiccChannel?

    fun tryOpenUsbEuiccChannel(
        usbDevice: UsbDevice,
        usbInterface: UsbInterface,
        isdrAid: ByteArray
    ): EuiccChannel?

    /**
     * Release all resources used by this EuiccChannelFactory
     * Note that the same instance may be reused; any resources allocated must be automatically
     * re-acquired when this happens
     */
    fun cleanup()
}