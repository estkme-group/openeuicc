package net.typeblog.lpac_jni.impl

import android.util.Log
import net.typeblog.lpac_jni.HttpInterface
import java.net.URL
import java.security.SecureRandom
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory

class HttpInterfaceImpl: HttpInterface {
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

        val parsedUrl = URL(url)
        if (parsedUrl.protocol != "https") {
            throw IllegalArgumentException("SM-DP+ servers must use the HTTPS protocol")
        }

        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagers, SecureRandom())

            val conn = parsedUrl.openConnection() as HttpsURLConnection
            conn.connectTimeout = 2000
            conn.sslSocketFactory = sslContext.socketFactory
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

            return HttpInterface.HttpResponse(conn.responseCode, conn.inputStream.readBytes())
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun usePublicKeyIds(pkids: Array<String>) {
        val trustManagerFactory = TrustManagerFactory.getInstance("PKIX").apply {
            init(keyIdToKeystore(pkids))
        }
        trustManagers = trustManagerFactory.trustManagers
    }
}