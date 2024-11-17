package net.typeblog.lpac_jni.impl

import android.annotation.SuppressLint
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

@SuppressLint("CustomX509TrustManager")
class IgnoreTLSCertificate : X509TrustManager {
    @SuppressLint("TrustAllX509TrustManager")
    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        return
    }

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        return
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return emptyArray()
    }
}