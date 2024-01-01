package net.typeblog.lpac_jni.impl

import android.util.Log
import net.typeblog.lpac_jni.HttpInterface
import java.net.HttpURLConnection
import java.net.URL

class HttpInterfaceImpl: HttpInterface {
    companion object {
        private const val TAG = "HttpInterfaceImpl"
    }

    override fun transmit(
        url: String,
        tx: ByteArray,
        headers: Array<String>
    ): HttpInterface.HttpResponse {
        Log.d(TAG, "transmit(url = $url)")

        try {
            val conn = URL(url).openConnection() as HttpURLConnection
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
}