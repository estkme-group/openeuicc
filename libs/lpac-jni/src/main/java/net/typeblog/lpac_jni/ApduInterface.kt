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
}