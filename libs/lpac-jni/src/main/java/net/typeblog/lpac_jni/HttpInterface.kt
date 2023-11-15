package net.typeblog.lpac_jni

/*
 * Should reflect euicc_http_interface in lpac/euicc/interface.h
 */
sealed interface HttpInterface {
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

    fun transmit(url: String, tx: ByteArray): HttpResponse
}