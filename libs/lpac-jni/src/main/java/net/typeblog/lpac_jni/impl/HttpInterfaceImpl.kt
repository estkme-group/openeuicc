package net.typeblog.lpac_jni.impl

import net.typeblog.lpac_jni.HttpInterface
import java.net.HttpURLConnection
import java.net.URL

class HttpInterfaceImpl: HttpInterface {
    override fun transmit(
        url: String,
        tx: ByteArray,
        headers: Array<String>
    ): HttpInterface.HttpResponse {
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

        return HttpInterface.HttpResponse(conn.responseCode, conn.inputStream.readBytes())
    }
}