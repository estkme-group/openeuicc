package im.angry.openeuicc.core

import android.content.Context
import android.util.Log
import im.angry.openeuicc.R
import im.angry.openeuicc.util.PrivilegedEuiccContextMarker
import im.angry.openeuicc.util.RealUiccPortInfoCompat
import im.angry.openeuicc.util.UiccPortInfoCompat
import im.angry.openeuicc.util.encodeHex
import im.angry.openeuicc.util.preferenceRepository
import kotlinx.coroutines.flow.first

class PrivilegedEuiccChannelFactory(context: Context) : DefaultEuiccChannelFactory(context),
    PrivilegedEuiccContextMarker {
    override val openEuiccMarkerContext: Context
        get() = context

    @Suppress("NAME_SHADOWING")
    override suspend fun tryOpenEuiccChannel(
        port: UiccPortInfoCompat,
        isdrAid: ByteArray,
        seId: EuiccChannel.SecureElementId,
    ): EuiccChannel? {
        val port = port as RealUiccPortInfoCompat
        if (port.card.isRemovable) {
            // Attempt unprivileged (OMAPI) before TelephonyManager
            // but still try TelephonyManager in case OMAPI is broken
            super.tryOpenEuiccChannel(port, isdrAid, seId)?.let { return it }
        }

        if (port.card.isEuicc || preferenceRepository.removableTelephonyManagerFlow.first()) {
            Log.i(
                DefaultEuiccChannelManager.TAG,
                "Trying TelephonyManager for slot ${port.card.physicalSlotIndex} port ${port.portIndex}"
            )
            try {
                return EuiccChannelImpl(
                    context.getString(R.string.channel_type_telephony_manager),
                    port,
                    intrinsicChannelName = null,
                    TelephonyManagerApduInterface(
                        port,
                        telephonyManager,
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
                    "TelephonyManager APDU interface unavailable for slot ${port.card.physicalSlotIndex} port ${port.portIndex} with ISD-R AID: ${isdrAid.encodeHex()}."
                )
            }
        }

        return super.tryOpenEuiccChannel(port, isdrAid, seId)
    }
}
