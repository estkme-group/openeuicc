package im.angry.openeuicc.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.se.omapi.SEService
import android.telephony.TelephonyManager
import androidx.fragment.app.Fragment
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import im.angry.openeuicc.OpenEuiccApplication
import im.angry.openeuicc.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.RuntimeException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val Context.selfAppVersion: String
    get() =
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName!!
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }

suspend fun readSelfLog(lines: Int = 2048): String = withContext(Dispatchers.IO) {
    try {
        Runtime.getRuntime().exec("logcat -t $lines").inputStream.readBytes()
            .decodeToString()
    } catch (_: Exception) {
        ""
    }
}

interface OpenEuiccContextMarker {
    val openEuiccMarkerContext: Context
        get() = when (this) {
            is Context -> this
            is Fragment -> requireContext()
            else -> throw RuntimeException("OpenEuiccUIContextMarker shall only be used on Fragments or UI types that derive from Context")
        }

    val openEuiccApplication: OpenEuiccApplication
        get() = openEuiccMarkerContext.applicationContext as OpenEuiccApplication

    val appContainer: AppContainer
        get() = openEuiccApplication.appContainer

    val telephonyManager: TelephonyManager
        get() = appContainer.telephonyManager
}

// Create an instance of OMAPI SEService in a manner that "makes sense" without unpredictable callbacks
suspend fun connectSEService(context: Context): SEService = suspendCoroutine { cont ->
    // Use a Mutex to make sure the continuation is run *after* the "service" variable is assigned
    val lock = Mutex()
    var service: SEService? = null
    val callback = {
        runBlocking {
            lock.withLock {
                cont.resume(service!!)
            }
        }
    }

    runBlocking {
        // If this were not protected by a Mutex, callback might be run before service is even assigned
        // Yes, we are on Android, we could have used something like a Handler, but we cannot really
        // assume the coroutine is run on a thread that has a Handler. We either use our own HandlerThread
        // (and then cleanup becomes an issue), or we use a lock
        lock.withLock {
            try {
                service = SEService(context, { it.run() }, callback)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
    }
}

fun decodeQrFromBitmap(bmp: Bitmap): String? =
     runCatching {
        val pixels = IntArray(bmp.width * bmp.height)
        bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)

        val luminanceSource = RGBLuminanceSource(bmp.width, bmp.height, pixels)
        val binaryBmp = BinaryBitmap(HybridBinarizer(luminanceSource))

        QRCodeReader().decode(binaryBmp).text
    }.getOrNull()
