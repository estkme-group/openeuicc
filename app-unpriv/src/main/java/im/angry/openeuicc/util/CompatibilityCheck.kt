package im.angry.openeuicc.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.se.omapi.SEService
import android.telephony.TelephonyManager
import im.angry.easyeuicc.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun getCompatibilityChecks(context: Context): List<CompatibilityCheck> =
    listOf(
        HasSystemFeaturesCheck(context),
        OmapiConnCheck(context),
        KnownBrokenCheck(context)
    )

suspend fun List<CompatibilityCheck>.executeAll(callback: () -> Unit) = withContext(Dispatchers.IO) {
    forEach {
        it.run()
        withContext(Dispatchers.Main) {
            callback()
        }
    }
}

abstract class CompatibilityCheck(context: Context) {
    enum class State {
        NOT_STARTED,
        IN_PROGRESS,
        SUCCESS,
        FAILURE
    }

    var state = State.NOT_STARTED

    abstract val title: String
    protected abstract val defaultDescription: String
    protected lateinit var failureDescription: String

    val description: String
        get() = when {
            state == State.FAILURE && this::failureDescription.isInitialized -> failureDescription
            else -> defaultDescription
        }

    protected abstract suspend fun doCheck(): Boolean

    suspend fun run() {
        state = State.IN_PROGRESS
        delay(200)
        state = try {
            if (doCheck()) {
                State.SUCCESS
            } else {
                State.FAILURE
            }
        } catch (_: Exception) {
            State.FAILURE
        }
    }
}

internal class HasSystemFeaturesCheck(private val context: Context): CompatibilityCheck(context) {
    override val title: String
        get() = context.getString(R.string.compatibility_check_system_features)
    override val defaultDescription: String
        get() = context.getString(R.string.compatibility_check_system_features_desc)

    override suspend fun doCheck(): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            failureDescription = context.getString(R.string.compatibility_check_system_features_no_telephony)
            return false
        }

        // We can check OMAPI UICC availability on R or later (if before R, we check OMAPI connectivity later)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_SE_OMAPI_UICC
        )) {
            failureDescription = context.getString(R.string.compatibility_check_system_features_no_omapi)
            return false
        }

        return true
    }
}

internal class OmapiConnCheck(private val context: Context): CompatibilityCheck(context) {
    override val title: String
        get() = context.getString(R.string.compatibility_check_omapi_connectivity)
    override val defaultDescription: String
        get() = context.getString(R.string.compatibility_check_omapi_connectivity_desc)

    private suspend fun getSEService(): SEService = suspendCoroutine { cont ->
        var service: SEService? = null
        var resumed = false
        val resume = {
            if (!resumed && service != null) {
                cont.resume(service!!)
                resumed = true
            }
        }
        service = SEService(context, { it.run() }, { resume() })
        Thread.sleep(1000)
        resume()
    }

    override suspend fun doCheck(): Boolean {
        val seService = getSEService()
        if (!seService.isConnected) {
            failureDescription = context.getString(R.string.compatibility_check_omapi_connectivity_fail)
            return false
        }

        val tm = context.getSystemService(TelephonyManager::class.java)
        val simReaders = seService.readers.filter { it.name.startsWith("SIM") }
        if (simReaders.size < tm.activeModemCountCompat) {
            failureDescription = context.getString(R.string.compatibility_check_omapi_connectivity_fail_sim_number,
                simReaders.map { (it.name.replace("SIM", "").toIntOrNull() ?: 1) - 1 }
                    .joinToString(", "))
            return false
        }

        return true
    }
}

internal class KnownBrokenCheck(private val context: Context): CompatibilityCheck(context) {
    companion object {
        val BROKEN_MANUFACTURERS = arrayOf("xiaomi")
    }

    override val title: String
        get() = context.getString(R.string.compatibility_check_known_broken)
    override val defaultDescription: String
        get() = context.getString(R.string.compatibility_check_known_broken_desc)

    init {
        failureDescription = context.getString(R.string.compatibility_check_known_broken_fail)
    }

    override suspend fun doCheck(): Boolean =
        Build.MANUFACTURER.lowercase() !in BROKEN_MANUFACTURERS
}