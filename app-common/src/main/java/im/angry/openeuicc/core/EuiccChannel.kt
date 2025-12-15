package im.angry.openeuicc.core

import android.os.Parcel
import android.os.Parcelable
import im.angry.openeuicc.util.*
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant

interface EuiccChannel {
    val type: String

    val port: UiccPortInfoCompat

    val slotId: Int // PHYSICAL slot
    val logicalSlotId: Int
    val portId: Int

    /**
     * A semi-obscure wrapper over the integer ID of a secure element on a card.
     *
     * Because the ID is arbitrary, this is intended to discourage the use of the
     * integer value directly. Additionally, it prevents accidentally calling the
     * wrong function in EuiccChannelManager with a ton of integer parameters.
     */
    class SecureElementId private constructor(val id: Int) : Parcelable {
        companion object {
            val DEFAULT = SecureElementId(0)

            /**
             * Create a SecureElementId from an integer ID. You should not call this directly
             * unless you know what you're doing.
             *
             * This is currently only ever used in the download flow.
             */
            fun createFromInt(id: Int): SecureElementId =
                SecureElementId(id)

            @Suppress("unused")
            @JvmField
            val CREATOR = object : Parcelable.Creator<SecureElementId> {
                override fun createFromParcel(parcel: Parcel): SecureElementId =
                    createFromInt(parcel.readInt())

                override fun newArray(size: Int): Array<SecureElementId?> = arrayOfNulls(size)
            }
        }

        override fun hashCode(): Int =
            id.hashCode()

        override fun equals(other: Any?): Boolean =
            if (other is SecureElementId) {
                this.id == other.id
            } else {
                super.equals(other)
            }

        override fun describeContents(): Int = id

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(id)
        }
    }

    /**
     * Some chips support multiple SEs on one chip. The seId here is intended
     * to distinguish channels opened from these different SEs.
     */
    val seId: SecureElementId

    /**
     * Does this channel belong to a chip that supports multiple SEs?
     * Note that this is only made `var` to make initialization a bit less annoying --
     * this should never be set again after the channel is originally opened.
     * Attempting to do so will yield an exception.
     */
    var hasMultipleSE: Boolean

    val lpa: LocalProfileAssistant

    val valid: Boolean

    /**
     * Answer to Reset (ATR) value of the underlying interface, if any
     */
    val atr: ByteArray?

    /**
     * Intrinsic name of this channel. For device-internal SIM slots,
     * this should be null; for USB readers, this should be the name of
     * the reader device.
     */
    val intrinsicChannelName: String?

    /**
     * The underlying APDU interface for this channel
     */
    val apduInterface: ApduInterface

    /**
     * The AID of the ISD-R channel currently in use
     */
    val isdrAid: ByteArray

    fun close()
}
