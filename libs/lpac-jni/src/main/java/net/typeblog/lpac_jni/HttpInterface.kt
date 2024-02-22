package net.typeblog.lpac_jni

import javax.net.ssl.TrustManager

/*
 * Should reflect euicc_http_interface in lpac/euicc/interface.h
 */
interface HttpInterface {
    data class HttpResponse(val rcode: Int, val data: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HttpResponse

            if (rcode != other.rcode) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = rcode
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    fun transmit(url: String, tx: ByteArray, headers: Array<String>): HttpResponse
    // The LPA is supposed to pass in a list of pkIds supported by the eUICC.
    // HttpInterface is responsible for providing TrustManager implementations that
    // validate based on certificates corresponding to these pkIds
    fun usePublicKeyIds(pkids: Array<String>)
}