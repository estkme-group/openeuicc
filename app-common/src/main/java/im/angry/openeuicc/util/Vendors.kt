package im.angry.openeuicc.util

import android.util.Log
import im.angry.openeuicc.core.ApduInterfaceAtrProvider
import im.angry.openeuicc.core.EuiccChannel
import net.typeblog.lpac_jni.Version

data class EuiccVendorInfo(
    val skuName: String? = null,
    val serialNumber: String? = null,
    val firmwareVersion: String? = null,
)

private val EUICC_VENDORS: Array<EuiccVendor> = arrayOf(ESTKme(), SIMLink())

fun EuiccChannel.tryParseEuiccVendorInfo(): EuiccVendorInfo? =
    EUICC_VENDORS.firstNotNullOfOrNull { it.tryParseEuiccVendorInfo(this) }

fun EuiccChannel.queryVendorAidListTransformation(aidList: List<ByteArray>): Pair<List<ByteArray>, VendorAidDecider>? =
    EUICC_VENDORS.firstNotNullOfOrNull { it.transformAidListIfNeeded(this, aidList) }

fun interface VendorAidDecider {
    /**
     * Given a list of already opened AIDs, should we still attempt to open the next?
     */
    fun shouldOpenMore(openedAids: List<ByteArray>, nextAid: ByteArray): Boolean
}

interface EuiccVendor {
    fun tryParseEuiccVendorInfo(channel: EuiccChannel): EuiccVendorInfo?

    /**
     * Removable eSIM products from some vendors may prefer a vendor-specific list of AIDs or
     * a specific ordering. For example, multi-SE products from eSTK.me might prefer us trying
     * SE0 and SE1 AIDs first instead of the generic GSMA ISD-R AID. This method is intended
     * to implement these vendor-specific cases.
     *
     * This method is called on an already opened `EuiccChannel`. If the method returns a non-null
     * value, the channel will be closed and the process that attempts to open all channels will
     * be restarted from the beginning. The method will not be called again for the same chip,
     * but it should still ensure idempotency when called with an already-transformed input.
     *
     * The second return value of this method is used to decide when we should stop attempting more
     * AIDs from the list.
     */
    fun transformAidListIfNeeded(
        referenceChannel: EuiccChannel,
        aidList: List<ByteArray>
    ): Pair<List<ByteArray>, VendorAidDecider>? = null
}

class ESTKme : EuiccVendor {
    companion object {
        private val PRODUCT_AID = "A06573746B6D65FFFFFFFFFFFF6D6774".decodeHex()

        val ESTK_SE0_AID = "A06573746B6D65FFFF4953442D522030".decodeHex()
        val ESTK_SE1_AID = "A06573746B6D65FFFF4953442D522031".decodeHex()
    }

    private fun decodeAsn1String(b: ByteArray): String? {
        if (b.size < 2) return null
        if (b[b.size - 2] != 0x90.toByte() || b[b.size - 1] != 0x00.toByte()) return null
        return b.sliceArray(0 until b.size - 2).decodeToString()
    }

    override fun tryParseEuiccVendorInfo(channel: EuiccChannel): EuiccVendorInfo? {
        val iface = channel.apduInterface
        return try {
            iface.withLogicalChannel(PRODUCT_AID) { transmit ->
                fun invoke(p1: Byte) =
                    decodeAsn1String(transmit(byteArrayOf(0x00, 0x00, p1, 0x00, 0x00)))
                EuiccVendorInfo(
                    skuName = invoke(0x03),
                    serialNumber = invoke(0x00),
                    firmwareVersion = run {
                        val bl = invoke(0x01) // bootloader version
                        val fw = invoke(0x02) // firmware version
                        if (bl == null || fw == null) null else "$bl-$fw"
                    },
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to get ESTKmeInfo", e)
            null
        }
    }

    override fun transformAidListIfNeeded(
        referenceChannel: EuiccChannel,
        aidList: List<ByteArray>
    ): Pair<List<ByteArray>, VendorAidDecider>? {
        try {
            referenceChannel.apduInterface.withLogicalChannel(PRODUCT_AID) {}
        } catch (_: Exception) {
            // Not eSTK!
            return null
        }

        // If we get here, this is eSTK, and we need to rearrange aidList such that:
        //   1. SE0 and SE1 AIDs are _always_ included in the list
        //   2. SE0 and SE1 AIDs are always sorted at the beginning of the list
        val expected = listOf(ESTK_SE0_AID, ESTK_SE1_AID, *aidList.filter {
            !it.contentEquals(ESTK_SE0_AID) && !it.contentEquals(ESTK_SE1_AID)
        }.toTypedArray())

        return if (expected == aidList) {
            null
        } else {
            Pair(expected, VendorAidDecider { openedAids, nextAid ->
                // Don't open any more channels if we have reached the GSMA default AID and at least 1
                // eSTK AID has been opened (note that above we re-sorted them to the top of the list)
                !(openedAids.isNotEmpty() && nextAid.contentEquals(EUICC_DEFAULT_ISDR_AID.decodeHex()))
            })
        }
    }
}

class SIMLink : EuiccVendor {
    companion object {
        private val EID_PATTERN = Regex("^89044045(84|21)67274948")
    }

    override fun tryParseEuiccVendorInfo(channel: EuiccChannel): EuiccVendorInfo? {
        val eid = channel.lpa.eID
        val version = channel.lpa.euiccInfo2?.euiccFirmwareVersion
        if (version == null || EID_PATTERN.find(eid, 0) == null) return null
        val versionName = when {
            // @formatter:off
            version >= Version(37,  4,  3) -> "v3.2 (beta 1)"
            version >= Version(37,  1, 41) -> "v3.1 (beta 1)"
            version >= Version(36, 18,  5) -> "v3 (final)"
            version >= Version(36, 17, 39) -> "v3 (beta)"
            version >= Version(36, 17,  4) -> "v2s"
            version >= Version(36,  9,  3) -> "v2.1"
            version >= Version(36,  7,  2) -> "v2"
            // @formatter:on
            else -> null
        }

        val skuName = if (versionName == null) {
            "9eSIM"
        } else {
            "9eSIM $versionName"
        }

        return EuiccVendorInfo(skuName = skuName)
    }
}