package im.angry.openeuicc.core

import android.content.Context
import android.se.omapi.SEService
import android.util.Log
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.usb.UsbApduInterface
import im.angry.openeuicc.core.usb.UsbCcidContext
import im.angry.openeuicc.util.*
import java.lang.IllegalArgumentException

open class DefaultEuiccChannelFactory(protected val context: Context) : EuiccChannelFactory {
    private var seService: SEService? = null

    private suspend fun ensureSEService() {
        if (seService == null || !seService!!.isConnected) {
            seService = connectSEService(context)
        }
    }

    override suspend fun tryOpenEuiccChannel(
        port: UiccPortInfoCompat,
        isdrAid: ByteArray,
        seId: EuiccChannel.SecureElementId,
    ): EuiccChannel? = try {
        if (port.portIndex != 0) {
            Log.w(
                DefaultEuiccChannelManager.TAG,
                "OMAPI channel attempted on non-zero portId, this may or may not work."
            )
        }

        ensureSEService()

        Log.i(
            DefaultEuiccChannelManager.TAG,
            "Trying OMAPI for physical slot ${port.card.physicalSlotIndex}"
        )
        EuiccChannelImpl(
            context.getString(R.string.channel_type_omapi),
            port,
            intrinsicChannelName = null,
            OmapiApduInterface(
                seService!!,
                port,
                context.preferenceRepository.verboseLoggingFlow
            ),
            isdrAid,
            seId,
            context.preferenceRepository.verboseLoggingFlow,
            context.preferenceRepository.ignoreTLSCertificateFlow,
            context.preferenceRepository.es10xMssFlow,
        )
    } catch (_: IllegalArgumentException) {
        // Failed
        Log.w(
            DefaultEuiccChannelManager.TAG,
            "OMAPI APDU interface unavailable for physical slot ${port.card.physicalSlotIndex} with ISD-R AID: ${isdrAid.encodeHex()}."
        )
        null
    }

    override fun tryOpenUsbEuiccChannel(
        ccidCtx: UsbCcidContext,
        isdrAid: ByteArray,
        seId: EuiccChannel.SecureElementId
    ): EuiccChannel? = try {
        EuiccChannelImpl(
            context.getString(R.string.channel_type_usb),
            FakeUiccPortInfoCompat(FakeUiccCardInfoCompat(EuiccChannelManager.USB_CHANNEL_ID)),
            intrinsicChannelName = ccidCtx.productName,
            UsbApduInterface(
                ccidCtx
            ),
            isdrAid,
            seId,
            context.preferenceRepository.verboseLoggingFlow,
            context.preferenceRepository.ignoreTLSCertificateFlow,
            context.preferenceRepository.es10xMssFlow,
        )
    } catch (_: IllegalArgumentException) {
        // Failed
        Log.w(
            DefaultEuiccChannelManager.TAG,
            "USB APDU interface unavailable for ISD-R AID: ${isdrAid.encodeHex()}."
        )
        null
    }

    override fun cleanup() {
        seService?.shutdown()
        seService = null
    }
}