package net.typeblog.lpac_jni

/*
 * Should reflect euicc_apdu_interface in lpac/euicc/interface.h
 */
interface ApduInterface {
    fun connect()
    fun disconnect()
    fun logicalChannelOpen(aid: ByteArray): Int
    fun logicalChannelClose(handle: Int)
    fun transmit(tx: ByteArray): ByteArray

    /**
     * Is this APDU connection still valid?
     * Note that even if this returns true, the underlying connection might be broken anyway;
     * callers should further check with the LPA to fully determine the validity of a channel
     */
    val valid: Boolean
}