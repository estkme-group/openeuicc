// Adapted from <https://github.com/open-keychain/open-keychain/blob/master/OpenKeychain/src/main/java/org/sufficientlysecure/keychain/securitytoken/usb>
package im.angry.openeuicc.core.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface

class UsbTransportException(message: String) : Exception(message)

val UsbDevice.interfaces: Iterable<UsbInterface>
    get() = (0 until interfaceCount).map(::getInterface)

val Iterable<UsbInterface>.smartCard: UsbInterface?
    get() = find { it.interfaceClass == UsbConstants.USB_CLASS_CSCID }

val UsbInterface.endpoints: Iterable<UsbEndpoint>
    get() = (0 until endpointCount).map(::getEndpoint)

val Iterable<UsbEndpoint>.bulkPair: Pair<UsbEndpoint?, UsbEndpoint?>
    get() {
        val endpoints = filter { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK }
        return Pair(
            endpoints.find { it.direction == UsbConstants.USB_DIR_IN },
            endpoints.find { it.direction == UsbConstants.USB_DIR_OUT },
        )
    }
