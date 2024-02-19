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

fun getCompatibilityChecks(context: Context): List<CompatibilityCheck> =
    listOf(
        HasSystemFeaturesCheck(context),
        OmapiConnCheck(context),
        IsdrChannelAccessCheck(context),
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

private val Reader.isSIM: Boolean
    get() = name.startsWith("SIM")

private val Reader.slotIndex: Int
    get() = (name.replace("SIM", "").toIntOrNull() ?: 1)

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

    protected abstract suspend fun doCheck(): State

    suspend fun run() {
        state = State.IN_PROGRESS
        delay(200)
        state = try {
            doCheck()
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

    override suspend fun doCheck(): State {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            failureDescription = context.getString(R.string.compatibility_check_system_features_no_telephony)
            return State.FAILURE
        }

        // We can check OMAPI UICC availability on R or later (if before R, we check OMAPI connectivity later)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_SE_OMAPI_UICC
        )) {
            failureDescription = context.getString(R.string.compatibility_check_system_features_no_omapi)
            return State.FAILURE
        }

        return State.SUCCESS
    }
}

internal class OmapiConnCheck(private val context: Context): CompatibilityCheck(context) {
    override val title: String
        get() = context.getString(R.string.compatibility_check_omapi_connectivity)
    override val defaultDescription: String
        get() = context.getString(R.string.compatibility_check_omapi_connectivity_desc)

    override suspend fun doCheck(): State {
        val seService = connectSEService(context)
        if (!seService.isConnected) {
            failureDescription = context.getString(R.string.compatibility_check_omapi_connectivity_fail)
            return State.FAILURE
        }

        val tm = context.getSystemService(TelephonyManager::class.java)
        val simReaders = seService.readers.filter { it.isSIM }
        if (simReaders.isEmpty()) {
            failureDescription = context.getString(R.string.compatibility_check_omapi_connectivity_fail)
            return State.FAILURE
        } else if (simReaders.size < tm.activeModemCountCompat) {
            failureDescription = context.getString(R.string.compatibility_check_omapi_connectivity_fail_sim_number,
                simReaders.map { it.slotIndex }.joinToString(", "))
            return State.FAILURE
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

    override suspend fun doCheck(): State {
        val seService = connectSEService(context)
        val (validSlotIds, result) = seService.readers.filter { it.isSIM }.map {
            try {
                it.openSession().openLogicalChannel(ISDR_AID)?.close()
                Pair(it.slotIndex, State.SUCCESS)
            } catch (_: SecurityException) {
                // Ignore; this is expected when everything works
                // ref: https://android.googlesource.com/platform/frameworks/base/+/4fe64fb4712a99d5da9c9a0eb8fd5169b252e1e1/omapi/java/android/se/omapi/Session.java#305
                // SecurityException is only thrown when Channel is constructed, which means everything else needs to succeed
                Pair(it.slotIndex, State.SUCCESS)
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

    override suspend fun doCheck(): State =
        if (Build.MANUFACTURER.lowercase() in BROKEN_MANUFACTURERS) {
            State.FAILURE
        } else {
            State.SUCCESS
        }
}