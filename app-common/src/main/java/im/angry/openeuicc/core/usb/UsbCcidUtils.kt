// Adapted from <https://github.com/open-keychain/open-keychain/blob/master/OpenKeychain/src/main/java/org/sufficientlysecure/keychain/securitytoken/usb>
package im.angry.openeuicc.core.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface

class UsbTransportException(msg: String) : Exception(msg)

fun UsbInterface.getIoEndpoints(): Pair<UsbEndpoint?, UsbEndpoint?> {
    var bulkIn: UsbEndpoint? = null
    var bulkOut: UsbEndpoint? = null
    for (i in 0 until endpointCount) {
        val endpoint = getEndpoint(i)
        if (endpoint.type != UsbConstants.USB_ENDPOINT_XFER_BULK) {
            continue
        }
        if (endpoint.direction == UsbConstants.USB_DIR_IN) {
            bulkIn = endpoint
        } else if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
            bulkOut = endpoint
        }
    }
    return Pair(bulkIn, bulkOut)
}

fun UsbDevice.getSmartCardInterface(): UsbInterface? {
    for (i in 0 until interfaceCount) {
        val anInterface = getInterface(i)
        if (anInterface.interfaceClass == UsbConstants.USB_CLASS_CSCID) {
            return anInterface
        }
    }
    return null
}