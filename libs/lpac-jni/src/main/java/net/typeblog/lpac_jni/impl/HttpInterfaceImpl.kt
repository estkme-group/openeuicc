package net.typeblog.lpac_jni.impl

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.typeblog.lpac_jni.HttpInterface
import java.net.URL
import java.security.SecureRandom
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory

class HttpInterfaceImpl(
    private val verboseLoggingFlow: Flow<Boolean>,
    private val ignoreTLSCertificateFlow: Flow<Boolean>
) : HttpInterface {
    companion object {
        private const val TAG = "HttpInterfaceImpl"
    }

    private lateinit var trustManagers: Array<TrustManager>

    override fun transmit(
        url: String,
        tx: ByteArray,
        headers: Array<String>
    ): HttpInterface.HttpResponse {
        Log.d(TAG, "transmit(url = $url)")

        if (runBlocking { verboseLoggingFlow.first() }) {
            Log.d(TAG, "HTTP tx = ${tx.decodeToString(throwOnInvalidSequence = false)}")
        }

        val parsedUrl = URL(url)
        if (parsedUrl.protocol != "https") {
            throw IllegalArgumentException("SM-DP+ servers must use the HTTPS protocol")
        }

        try {
            val conn = parsedUrl.openConnection() as HttpsURLConnection
            conn.connectTimeout = 2000

            if (url.contains("handleNotification")) {
                conn.connectTimeout = 1000
                conn.readTimeout = 1000
            }

            conn.sslSocketFactory = getSocketFactory()
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = true

            for (h in headers) {
                val s = h.split(":", limit = 2)
                conn.setRequestProperty(s[0], s[1])
            }

            conn.outputStream.write(tx)
            conn.outputStream.flush()
            conn.outputStream.close()

            Log.d(TAG, "transmit responseCode = ${conn.responseCode}")

            val bytes = conn.inputStream.readBytes().also {
                if (runBlocking { verboseLoggingFlow.first() }) {
                    Log.d(
                        TAG,
                        "HTTP response body = ${it.decodeToString(throwOnInvalidSequence = false)}"
                    )
                }
            }

            return HttpInterface.HttpResponse(conn.responseCode, bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun getSocketFactory(): SSLSocketFactory {
        val trustManagers =
            if (runBlocking { ignoreTLSCertificateFlow.first() }) {
                arrayOf(AllowAllTrustManager())
            } else {
                this.trustManagers
            }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagers, SecureRandom())
        return sslContext.socketFactory
    }

    override fun usePublicKeyIds(pkids: Array<String>) {
        val trustManagerFactory = TrustManagerFactory.getInstance("PKIX").apply {
            init(keyIdToKeystore(pkids))
        }
        trustManagers = trustManagerFactory.trustManagers
    }
}