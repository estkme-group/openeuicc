package im.angry.openeuicc.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.se.omapi.Reader
import android.telephony.TelephonyManager
import im.angry.easyeuicc.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException

fun getCompatibilityChecks(context: Context): List<CompatibilityCheck> =
    listOf(
        HasSystemFeaturesCheck(context),
        OmapiConnCheck(context),
        IsdrChannelAccessCheck(context),
        KnownBrokenCheck(context),
        UsbCheck(context),
        Verdict(context),
    )

inline fun <reified T: CompatibilityCheck> List<CompatibilityCheck>.findCheck(): T? =
    find { it.javaClass == T::class.java }?.let { it as T }

suspend fun List<CompatibilityCheck>.executeAll(callback: () -> Unit) = withContext(Dispatchers.IO) {
    forEach {
        it.run(this@executeAll)
        withContext(Dispatchers.Main) {
            callback()
        }
    }
}

private val Reader.isSIM: Boolean
    get() = name.startsWith("SIM")

private val Reader.slotIndex: Int
    get() = (name.replace("SIM", "").toIntOrNull() ?: 1)

abstract class CompatibilityCheck(context: Context) {
    enum class State {
        NOT_STARTED,
        IN_PROGRESS,
        SUCCESS,
        FAILURE_UNKNOWN, // The check technically failed, but no conclusion can be drawn
        FAILURE
    }

    var state = State.NOT_STARTED

    abstract val title: String
    protected abstract val defaultDescription: String
    protected lateinit var successDescription: String
    protected lateinit var failureDescription: String

    val description: String
        get() = when {
            (state == State.FAILURE || state == State.FAILURE_UNKNOWN) && this::failureDescription.isInitialized -> failureDescription
            state == State.SUCCESS && this::successDescription.isInitialized -> successDescription
            else -> defaultDescription
        }

    protected abstract suspend fun doCheck(allChecks: List<CompatibilityCheck>): State

    suspend fun run(allChecks: List<CompatibilityCheck>) {
        state = State.IN_PROGRESS
        delay(200)
        state = try {
            doCheck(allChecks)
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

    override suspend fun doCheck(allChecks: List<CompatibilityCheck>): State {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            failureDescription = context.getString(R.string.compatibility_check_system_features_no_telephony)
            return State.FAILURE
        }

        // We can check OMAPI UICC availability on R or later (if before R, we check OMAPI connectivity later)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_SE_OMAPI_UICC
        )) {
            failureDescription = context.getString(R.string.compatibility_check_system_features_no_omapi)
            return State.FAILURE_UNKNOWN
        }

        return State.SUCCESS
    }
}

internal class OmapiConnCheck(private val context: Context): CompatibilityCheck(context) {
    override val title: String
        get() = context.getString(R.string.compatibility_check_omapi_connectivity)
    override val defaultDescription: String
        get() = context.getString(R.string.compatibility_check_omapi_connectivity_desc)

    override suspend fun doCheck(allChecks: List<CompatibilityCheck>): State {
        val seService = connectSEService(context)
        if (!seService.isConnected) {
            failureDescription = context.getString(R.string.compatibility_check_omapi_connectivity_fail)
            return State.FAILURE
        }

        val tm = context.getSystemService(TelephonyManager::class.java)
        val simReaders = seService.readers.filter { it.isSIM }
        if (simReaders.isEmpty()) {
            failureDescription = context.getString(R.string.compatibility_check_omapi_connectivity_fail)
            return State.FAILURE_UNKNOWN
        } else if (simReaders.size < tm.activeModemCountCompat) {
            successDescription = context.getString(R.string.compatibility_check_omapi_connectivity_partial_success_sim_number,
                simReaders.map { it.slotIndex }.joinToString(", "))
            return State.SUCCESS
        }

        return State.SUCCESS
    }
}

internal class IsdrChannelAccessCheck(private val context: Context): CompatibilityCheck(context) {
    companion object {
        val ISDR_AID = "A0000005591010FFFFFFFF8900000100".decodeHex()
    }

    override val title: String
        get() = context.getString(R.string.compatibility_check_isdr_channel)
    override val defaultDescription: String
        get() = context.getString(R.string.compatibility_check_isdr_channel_desc)

    override suspend fun doCheck(allChecks: List<CompatibilityCheck>): State {
        val seService = connectSEService(context)
        val readers = seService.readers.filter { it.isSIM }
        if (readers.isEmpty()) {
            failureDescription = context.getString(R.string.compatibility_check_isdr_channel_desc_unknown)
            return State.FAILURE_UNKNOWN
        }

        val (validSlotIds, result) = readers.map {
            try {
                it.openSession().openLogicalChannel(ISDR_AID)?.close()
                Pair(it.slotIndex, State.SUCCESS)
            } catch (_: SecurityException) {
                // Ignore; this is expected when everything works
                // ref: https://android.googlesource.com/platform/frameworks/base/+/4fe64fb4712a99d5da9c9a0eb8fd5169b252e1e1/omapi/java/android/se/omapi/Session.java#305
                // SecurityException is only thrown when Channel is constructed, which means everything else needs to succeed
                Pair(it.slotIndex, State.SUCCESS)
            } catch (e: IOException) {
                e.printStackTrace()
                if (e.message?.contains("Secure Element is not present") == true) {
                    failureDescription = context.getString(R.string.compatibility_check_isdr_channel_desc_unknown)
                    Pair(it.slotIndex, State.FAILURE_UNKNOWN)
                } else {
                    Pair(it.slotIndex, State.FAILURE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(it.slotIndex, State.FAILURE)
            }
        }.fold(Pair(mutableListOf<Int>(), State.SUCCESS)) { (ids, result), (id, ok) ->
            if (ok != State.SUCCESS) {
                Pair(ids, ok)
            } else {
                Pair(ids.apply { add(id) }, result)
            }
        }

        if (result != State.SUCCESS && validSlotIds.size > 0) {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_EUICC)) {
                failureDescription = context.getString(
                    R.string.compatibility_check_isdr_channel_desc_partial_fail,
                    validSlotIds.joinToString(", ")
                )
            } else {
                // If the device has embedded eSIMs, we can likely ignore the failure here;
                // the OMAPI failure likely resulted from trying to access internal eSIMs.
                return State.SUCCESS
            }
        }

        return result
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

    override suspend fun doCheck(allChecks: List<CompatibilityCheck>): State =
        if (Build.MANUFACTURER.lowercase() in BROKEN_MANUFACTURERS) {
            State.FAILURE
        } else {
            State.SUCCESS
        }
}

internal class UsbCheck(private val context: Context) : CompatibilityCheck(context) {
    override val title: String
        get() = context.getString(R.string.compatibility_check_usb)
    override val defaultDescription: String
        get() = context.getString(R.string.compatibility_check_usb_desc)

    init {
        successDescription = context.getString(R.string.compatibility_check_usb_ok)
        failureDescription = context.getString(R.string.compatibility_check_usb_fail)
    }

    override suspend fun doCheck(allChecks: List<CompatibilityCheck>): State =
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            State.SUCCESS
        } else {
            State.FAILURE
        }

}

internal class Verdict(private val context: Context) : CompatibilityCheck(context) {
    override val title: String
        get() = context.getString(R.string.compatibility_check_verdict)
    override val defaultDescription: String
        get() = context.getString(R.string.compatibility_check_verdict_desc)

    override suspend fun doCheck(allChecks: List<CompatibilityCheck>): State {
        if (allChecks.findCheck<KnownBrokenCheck>()?.state == State.FAILURE) {
            failureDescription = context.getString(
                R.string.compatibility_check_verdict_known_broken,
                context.getString(R.string.compatibility_check_verdict_fail_shared)
            )
            return State.FAILURE
        }

        if (allChecks.findCheck<OmapiConnCheck>()?.state == State.SUCCESS &&
            allChecks.findCheck<IsdrChannelAccessCheck>()?.state == State.SUCCESS
        ) {
            successDescription = context.getString(R.string.compatibility_check_verdict_ok)
            return State.SUCCESS
        }

        if (allChecks.findCheck<OmapiConnCheck>()?.state == State.FAILURE_UNKNOWN ||
            allChecks.findCheck<IsdrChannelAccessCheck>()?.state == State.FAILURE_UNKNOWN
        ) {
            // We are not sure because we can't fully check OMAPI
            // however we can guess based on feature flags
            // TODO: We probably need a "known-good" list for these devices as well?
            failureDescription = context.getString(
                if (allChecks.findCheck<HasSystemFeaturesCheck>()?.state == State.SUCCESS) {
                    R.string.compatibility_check_verdict_unknown_likely_ok
                } else {
                    R.string.compatibility_check_verdict_unknown_likely_fail
                },
                context.getString(R.string.compatibility_check_verdict_fail_shared)
            )
            return State.FAILURE_UNKNOWN
        }

        failureDescription = context.getString(
            R.string.compatibility_check_verdict_unknown,
            context.getString(R.string.compatibility_check_verdict_fail_shared)
        )
        return State.FAILURE_UNKNOWN
    }
}