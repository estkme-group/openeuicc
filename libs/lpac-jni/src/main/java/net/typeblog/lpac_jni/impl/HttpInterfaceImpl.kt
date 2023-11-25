package net.typeblog.lpac_jni.impl

import net.typeblog.lpac_jni.HttpInterface
import java.net.HttpURLConnection
import java.net.URL

class HttpInterfaceImpl: HttpInterface {
    override fun transmit(url: String, tx: ByteArray): HttpInterface.HttpResponse {
        android.util.Log.d("aaa", url)
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doInput = true
        conn.doOutput = true
        conn.setRequestProperty("User-Agent", "gsma-rsp-lpad")
        conn.setRequestProperty("X-Admin-Protocol", "gsma/rsp/v2.2.0")
        conn.setRequestProperty("Content-Type", "application/json")

        conn.outputStream.write(tx)
        conn.outputStream.flush()
        conn.outputStream.close()

        return HttpInterface.HttpResponse(conn.responseCode, conn.inputStream.readBytes())
    }
}