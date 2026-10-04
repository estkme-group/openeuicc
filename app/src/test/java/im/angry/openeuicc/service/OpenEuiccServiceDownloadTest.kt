package im.angry.openeuicc.service

import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.euicc.EuiccService
import android.service.euicc.GetEuiccProfileInfoListResult
import android.telephony.TelephonyManager
import android.telephony.UiccAccessRule
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import im.angry.openeuicc.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.testutil.MockEuiccChannel
import im.angry.openeuicc.testutil.MockEuiccChannelManager
import im.angry.openeuicc.testutil.TestOpenEuiccApplication
import im.angry.openeuicc.testutil.awaitMainLooper
import im.angry.openeuicc.ui.EuiccResolutionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.typeblog.lpac_jni.EuiccInfo2
import net.typeblog.lpac_jni.HttpInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.LocalProfileInfo
import net.typeblog.lpac_jni.LocalProfileNotification
import net.typeblog.lpac_jni.ProfileClass
import net.typeblog.lpac_jni.ProfileDownloadCallback
import net.typeblog.lpac_jni.ProfileDownloadInput
import net.typeblog.lpac_jni.ProfileDownloadState
import net.typeblog.lpac_jni.RemoteProfileAccessRule
import net.typeblog.lpac_jni.RemoteProfileInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowEuiccManager
import org.robolectric.shadows.UiccCardInfoBuilder
import org.robolectric.shadows.UiccPortInfoBuilder
import org.robolectric.util.ReflectionHelpers
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Carrier-app eSIM downloads through the platform (EuiccManager.downloadSubscription() →
 * EuiccController → OpenEuiccService), tested only through the public EuiccService entry points
 * and the contract of EuiccResolutionActivity (the user's answer → continueOperation()).
 *
 * Everything in OpenEUICC runs for real (OpenEuiccService, EuiccChannelManagerService and its
 * foreground tasks); only the LPA is faked. [CarrierLpa] replays what lpac reported against real
 * SM-DP+ servers on a Galaxy S21 (LineageOS 23.2 / Android 16, September 2026), with made-up
 * ICCIDs, activation codes and certificate hashes.
 *
 * The platform called the service in this order for an unprivileged carrier app:
 *   downloadSubscription() → RESOLVE_NO_PRIVILEGES → continueOperation(consent)
 *   → onGetDownloadableSubscriptionMetadata(forceDeactivateSim = true)
 *   → "Calling package has carrier privilege to this profile" (access rules match)
 *   → onDownloadSubscription(forceDeactivateSim = true)
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    application = TestOpenEuiccApplication::class,
    shadows = [ContinueOperationRecorder::class]
)
class OpenEuiccServiceDownloadTest {
    companion object {
        const val SLOT = 1 // physical slot of the S21's built-in eUICC
        const val PORT = 0
        const val CARRIER_APP = "com.example.carrierapp"
        const val EID = "89000000000000000000000000000000"

        val activationCode: DownloadableSubscription =
            DownloadableSubscription.forActivationCode("LPA:1\$smdp.example.com\$TEST-MATCHING-ID")

        // SGP.22 StoreMetadataRequest as received from the Simyo SM-DP+: provider "simyo",
        // one REF-AR-DO (certificate hash, here a fake SHA-256, plus package name)
        val operatorProfile = RemoteProfileInfo(
            iccid = "89000000000000000011",
            name = "Simyo Android eSIM",
            providerName = "simyo",
            profileClass = ProfileClass.Operational,
            accessRules = listOf(RemoteProfileAccessRule("AB".repeat(32), CARRIER_APP)),
        )

        // Nomad (travel eSIM, Thales SM-DP+): provider and name "NOMAD", one access rule
        val travelProfile = RemoteProfileInfo(
            iccid = "89000000000000000022",
            name = "NOMAD",
            providerName = "NOMAD",
            profileClass = ProfileClass.Operational,
            accessRules = listOf(RemoteProfileAccessRule("CD".repeat(32), null)),
        )
    }

    private val app = RuntimeEnvironment.getApplication()
    private lateinit var lpa: CarrierLpa
    private lateinit var channelManager: CarrierChannelManager
    private lateinit var channelManagerService: VirtualClockEuiccChannelManagerService
    private lateinit var service: OpenEuiccService

    @Before
    fun setUp() {
        lpa = CarrierLpa()
        channelManager = CarrierChannelManager(lpa)
        TestOpenEuiccApplication.mockEuiccChannelManager = channelManager
        channelManagerService =
            Robolectric.buildService(VirtualClockEuiccChannelManagerService::class.java).create().get()
        shadowOf(app).setComponentNameAndServiceForBindService(
            ComponentName(app, EuiccChannelManagerService::class.java),
            channelManagerService.onBind(Intent())
        )
        shadowOf(app.getSystemService(TelephonyManager::class.java)).setUiccCardsInfo(
            listOf(
                UiccCardInfoBuilder.newBuilder()
                    .setIsEuicc(true)
                    .setIsRemovable(false)
                    .setPhysicalSlotIndex(SLOT)
                    .setPorts(listOf(UiccPortInfoBuilder.newBuilder().setPortIndex(PORT).build()))
                    .build()
            )
        )
        service = Robolectric.buildService(OpenEuiccService::class.java).create().get()
        ContinueOperationRecorder.extras.clear()
    }

    /**
     * The platform calls EuiccService methods on a binder thread and waits for the result. Do the
     * same, while this (main) thread runs the main looper and delivers EuiccChannelManagerService's
     * self-start (startForegroundService()) like the system would. A call that blocks fails the
     * test after 30 s (its thread is left behind).
     */
    private fun <T> platformCall(call: OpenEuiccService.() -> T): T = runBlocking {
        val result = CoroutineScope(Dispatchers.IO).async { service.call() }
        awaitMainLooper(30_000) {
            shadowOf(app).nextStartedService?.let {
                // Let the task subscribe to the start command first (it's not replayed)
                shadowOf(Looper.getMainLooper()).idle()
                channelManagerService.onStartCommand(it, 0, 1)
            }
            result.isCompleted
        }
        result.await()
    }

    /**
     * Start EuiccResolutionActivity as com.android.phone does, press [button] (after typing
     * [confirmationCode], if given) and return the extras it passed to continueOperation().
     */
    private fun userAnswers(intent: Intent, button: Int, confirmationCode: String? = null): Bundle {
        Robolectric.buildActivity(EuiccResolutionActivity::class.java, intent).setup()
        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        confirmationCode?.let { dialog.findViewById<EditText>(R.id.euicc_resolution_code)!!.setText(it) }
        dialog.getButton(button).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        return ContinueOperationRecorder.extras.single()
    }

    // Mijn Simyo app, "Allow", switchAfterDownload = true
    @Test
    fun `operator app - one ES9+ session from metadata to download, then enable with refresh`() {
        val consent = userAnswers(
            Intent(EuiccService.ACTION_RESOLVE_NO_PRIVILEGES)
                .putExtra(EuiccService.EXTRA_RESOLUTION_CALLING_PACKAGE, CARRIER_APP),
            DialogInterface.BUTTON_POSITIVE
        )
        assertTrue(consent.getBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT))

        val metadata = platformCall { onGetDownloadableSubscriptionMetadata(SLOT, PORT, activationCode, true) }
        assertEquals(EuiccService.RESULT_OK, metadata.result)
        // The platform matches the calling package against these rules
        val subscription = metadata.downloadableSubscription
        assertEquals("simyo", ReflectionHelpers.callInstanceMethod<String>(subscription, "getCarrierName"))
        val rule = ReflectionHelpers.callInstanceMethod<List<UiccAccessRule>>(subscription, "getAccessRules").single()
        assertEquals("AB".repeat(32), rule.certificateHexString)
        assertEquals(CARRIER_APP, rule.packageName)
        // The session waits at the metadata step
        assertEquals(1, lpa.sessions.get())
        assertEquals(emptyList<Boolean>(), lpa.metadataStepAnswers)

        val download = platformCall {
            onDownloadSubscription(SLOT, PORT, activationCode, true, true, Bundle())
        }
        assertEquals(EuiccService.RESULT_OK, download.result)
        // The same session continued: many SM-DP+ servers refuse a second one for the same order
        assertEquals(1, lpa.sessions.get())
        assertEquals(listOf(true), lpa.metadataStepAnswers)
        assertEquals(listOf(operatorProfile.iccid to true), lpa.enableCalls)
    }

    // The SIM refresh of the switch makes the platform ask for the profiles
    // (SubscriptionManagerService.updateEmbeddedSubscriptions) while the switch waits for the eUICC
    // to come back. Without the new profile, it stores the new subscription as a physical SIM.
    @Test
    fun `profile list requested during the switch after a download has the new profile`() {
        lateinit var query: Deferred<GetEuiccProfileInfoListResult>
        channelManager.onRefresh = {
            query = CoroutineScope(Dispatchers.IO).async { service.onGetEuiccProfileInfoList(SLOT) }
        }

        platformCall { onGetDownloadableSubscriptionMetadata(SLOT, PORT, activationCode, true) }
        val download = platformCall {
            onDownloadSubscription(SLOT, PORT, activationCode, true, true, Bundle())
        }
        assertEquals(EuiccService.RESULT_OK, download.result)
        assertEquals(listOf(operatorProfile.iccid to true), lpa.enableCalls)

        val profiles = runBlocking {
            awaitMainLooper(30_000) { query.isCompleted }
            query.await()
        }
        assertEquals(EuiccService.RESULT_OK, profiles.result)
        assertEquals(listOf(operatorProfile.iccid), profiles.profiles.map { it.iccid })
    }

    // The order where the metadata is fetched before the user declines (Mijn Simyo asked for
    // consent first, see the next test); the waiting session must not be left behind
    @Test
    fun `don't allow after the metadata step cancels the waiting session`() = runBlocking {
        val metadata = platformCall { onGetDownloadableSubscriptionMetadata(SLOT, PORT, activationCode, false) }
        assertEquals(EuiccService.RESULT_OK, metadata.result)

        val consent = userAnswers(
            Intent(EuiccService.ACTION_RESOLVE_NO_PRIVILEGES)
                .putExtra(EuiccService.EXTRA_RESOLUTION_CALLING_PACKAGE, CARRIER_APP),
            DialogInterface.BUTTON_NEGATIVE
        )
        // The platform reports the error to the carrier app upon consent = false
        assertEquals(false, consent.getBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT, true))

        awaitMainLooper { lpa.metadataStepAnswers.isNotEmpty() }
        assertEquals(listOf(false), lpa.metadataStepAnswers)
        assertEquals(emptyList<LocalProfileInfo>(), lpa.profiles)
    }

    // Mijn Simyo app, "Don't allow" (consent is asked before the metadata step)
    @Test
    fun `don't allow before the metadata step starts no ES9+ session`() {
        val consent = userAnswers(
            Intent(EuiccService.ACTION_RESOLVE_NO_PRIVILEGES)
                .putExtra(EuiccService.EXTRA_RESOLUTION_CALLING_PACKAGE, CARRIER_APP),
            DialogInterface.BUTTON_NEGATIVE
        )
        assertEquals(false, consent.getBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT, true))
        assertEquals(0, lpa.sessions.get())
    }

    // Nomad app, switchAfterDownload = false, enabled later in Settings
    @Test
    fun `travel eSIM app - download without switching`() {
        lpa.metadata = travelProfile

        val metadata = platformCall { onGetDownloadableSubscriptionMetadata(SLOT, -1, activationCode, true) }
        assertEquals(EuiccService.RESULT_OK, metadata.result)
        assertEquals(
            "NOMAD",
            ReflectionHelpers.callInstanceMethod<String>(metadata.downloadableSubscription, "getCarrierName")
        )

        val download = platformCall {
            onDownloadSubscription(SLOT, PORT, activationCode, false, true, Bundle())
        }
        assertEquals(EuiccService.RESULT_OK, download.result)
        assertEquals(1, lpa.sessions.get())
        assertEquals(listOf(travelProfile.iccid), lpa.profiles.map { it.iccid })
        assertEquals(emptyList<Pair<String, Boolean>>(), lpa.enableCalls)
    }

    // Not observed with a real carrier (Simyo and Nomad need no confirmation code)
    @Test
    fun `confirmation code requested after the metadata step continues the same session`() {
        val subscription =
            DownloadableSubscription.forActivationCode("LPA:1\$smdp.example.com\$TEST-MATCHING-ID\$\$1")

        val metadata = platformCall { onGetDownloadableSubscriptionMetadata(SLOT, PORT, subscription, true) }
        assertEquals(EuiccService.RESULT_OK, metadata.result)

        val needsCode = platformCall {
            onDownloadSubscription(SLOT, PORT, subscription, false, true, Bundle())
        }
        assertEquals(EuiccService.RESULT_RESOLVABLE_ERRORS, needsCode.result)
        assertEquals(
            EuiccService.RESOLVABLE_ERROR_CONFIRMATION_CODE,
            needsCode.resolvableErrors and EuiccService.RESOLVABLE_ERROR_CONFIRMATION_CODE
        )

        val answer = userAnswers(
            Intent(EuiccService.ACTION_RESOLVE_RESOLVABLE_ERRORS)
                .putExtra(EuiccService.EXTRA_RESOLUTION_CALLING_PACKAGE, CARRIER_APP)
                .putExtra(EuiccService.EXTRA_RESOLVABLE_ERRORS, EuiccService.RESOLVABLE_ERROR_CONFIRMATION_CODE),
            DialogInterface.BUTTON_POSITIVE,
            confirmationCode = "1234"
        )
        assertTrue(answer.getBoolean(EuiccService.EXTRA_RESOLUTION_CONSENT))
        assertEquals("1234", answer.getString(EuiccService.EXTRA_RESOLUTION_CONFIRMATION_CODE))

        // EuiccController then runs the operation again, with the code
        val withCode = DownloadableSubscription.Builder(subscription).setConfirmationCode("1234").build()
        assertEquals(
            EuiccService.RESULT_OK,
            platformCall { onGetDownloadableSubscriptionMetadata(SLOT, PORT, withCode, true) }.result
        )
        val download = platformCall {
            onDownloadSubscription(SLOT, PORT, withCode, false, true, Bundle())
        }
        assertEquals(EuiccService.RESULT_OK, download.result)
        assertEquals(1, lpa.sessions.get())
        assertEquals("1234", lpa.prepareDownloadConfirmationCode)
    }

    // Not observed like this with a real carrier: a modem that rejects EnableProfile with refresh
    // (e.g. catBusy), so it only works without refresh and the switch task reports
    // SwitchingProfilesRefreshException after the profile has already been installed
    @Test
    fun `failed enable after a finished download still reports success`() {
        lpa.refreshWorks = false

        platformCall { onGetDownloadableSubscriptionMetadata(SLOT, PORT, activationCode, true) }
        val download = platformCall {
            onDownloadSubscription(SLOT, PORT, activationCode, true, true, Bundle())
        }
        // An error would make the carrier app request a new activation code for a profile
        // that is already installed
        assertEquals(EuiccService.RESULT_OK, download.result)
        assertEquals(listOf(operatorProfile.iccid), lpa.profiles.map { it.iccid })
        assertEquals(
            listOf(operatorProfile.iccid to true, operatorProfile.iccid to false),
            lpa.enableCalls
        )
    }

    @Test
    fun `session waiting at the metadata step survives a slow user`() {
        platformCall { onGetDownloadableSubscriptionMetadata(SLOT, PORT, activationCode, true) }

        // E.g. a consent or confirmation code dialog in between
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(1))

        val download = platformCall {
            onDownloadSubscription(SLOT, PORT, activationCode, false, true, Bundle())
        }
        assertEquals(EuiccService.RESULT_OK, download.result)
        assertEquals(1, lpa.sessions.get())
        assertEquals(listOf(true), lpa.metadataStepAnswers)
    }

    // The timeout itself runs on the virtual clock (VirtualClockEuiccChannelManagerService); this
    // checks that the service asks for one and cancels the session when it fires
    @Test
    fun `session waiting at the metadata step is cancelled after the timeout`() = runBlocking {
        platformCall { onGetDownloadableSubscriptionMetadata(SLOT, PORT, activationCode, true) }

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(6))
        awaitMainLooper { lpa.metadataStepAnswers.isNotEmpty() }
        assertEquals(listOf(false), lpa.metadataStepAnswers)

        // A late download request starts a new session
        val download = platformCall {
            onDownloadSubscription(SLOT, PORT, activationCode, false, true, Bundle())
        }
        assertEquals(EuiccService.RESULT_OK, download.result)
        assertEquals(2, lpa.sessions.get())
        assertEquals(listOf(false, true), lpa.metadataStepAnswers)
    }

    // The waiting session holds the eUICC (the LPA lock). The platform calls these synchronously,
    // e.g. on the telephony worker thread; they block until the session is over and only then
    // touch the eUICC. Nothing confirms the session here, so the confirmation timeout cancels it.
    @Test
    fun `EID and profile list requests block until the waiting session is over`() = runBlocking {
        platformCall { onGetDownloadableSubscriptionMetadata(SLOT, PORT, activationCode, true) }
        assertEquals(1, lpa.sessions.get())

        // Both block until the waiting session is over; nothing confirms it, so the confirmation
        // timeout cancels it while they wait (idleFor() advances the virtual clock)
        val eid = CoroutineScope(Dispatchers.IO).async { service.onGetEid(SLOT) }
        val profiles = CoroutineScope(Dispatchers.IO).async { service.onGetEuiccProfileInfoList(SLOT) }
        awaitMainLooper(30_000) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(1))
            eid.isCompleted && profiles.isCompleted
        }

        assertEquals(EID, eid.await())
        assertEquals(EuiccService.RESULT_OK, profiles.await().result)
        assertEquals(1, lpa.sessions.get())
        assertEquals(listOf(false), lpa.metadataStepAnswers)
    }

    // An invalid matching ID, refused by the SM-DP+ in
    // ES9+.AuthenticateClient with subjectCode 8.2.6, reasonCode 3.8
    @Test
    fun `server rejection is reported and leaves no waiting session`() {
        lpa.rejection = LocalProfileAssistant.ProfileDownloadException(
            lpaErrorReason = "ES10B_ERROR_REASON_UNDEFINED",
            lastHttpResponse = HttpInterface.HttpResponse(
                200,
                """{"header":{"functionExecutionStatus":{"status":"Failed","statusCodeData":{"subjectCode":"8.2.6","reasonCode":"3.8","subjectIdentifier":"Matching ID","message":"Refused"}}}}""".toByteArray()
            ),
            lastHttpException = null,
            lastApduResponse = null,
            lastApduException = null,
        )

        val metadata = platformCall { onGetDownloadableSubscriptionMetadata(SLOT, PORT, activationCode, true) }
        assertNotEquals(EuiccService.RESULT_OK, metadata.result)
        assertNull(metadata.downloadableSubscription)

        val download = platformCall {
            onDownloadSubscription(SLOT, PORT, activationCode, true, true, Bundle())
        }
        assertNotEquals(EuiccService.RESULT_OK, download.result)
        assertEquals(2, lpa.sessions.get())
        assertEquals(emptyList<Boolean>(), lpa.metadataStepAnswers)
        assertEquals(emptyList<Pair<String, Boolean>>(), lpa.enableCalls)
    }
}

/**
 * A LocalProfileAssistant replaying lpac against a real SM-DP+. Each downloadProfile() call is
 * one ES9+ session: ES9+.InitiateAuthentication and AuthenticateClient (Preparing, Connecting,
 * Authenticating), then the metadata step (ConfirmingDownload with the StoreMetadataRequest
 * content) and, only if confirmed there, PrepareDownload, GetBoundProfilePackage and
 * LoadBoundProfilePackage (Downloading, Finalizing). Like LocalProfileAssistantImpl, it throws a
 * ProfileDownloadException when the session is declined at the metadata step or fails.
 */
class CarrierLpa : LocalProfileAssistant {
    /** Profile metadata sent by the SM-DP+ */
    @Volatile
    var metadata = OpenEuiccServiceDownloadTest.operatorProfile

    /** If set, the SM-DP+ refuses the order during authentication with this error */
    @Volatile
    var rejection: LocalProfileAssistant.ProfileDownloadException? = null

    /** Whether EnableProfile with refresh works; if not, it only works without refresh */
    @Volatile
    var refreshWorks = true

    /** Number of ES9+ sessions (downloadProfile() calls) */
    val sessions = AtomicInteger()

    /** What the callback answered at the metadata step, per session (false = cancelled) */
    val metadataStepAnswers = CopyOnWriteArrayList<Boolean>()

    /** The confirmation code the last ES10b.PrepareDownload was called with */
    @Volatile
    var prepareDownloadConfirmationCode: String? = null

    /** (ICCID, refresh) of every EnableProfile */
    val enableCalls = CopyOnWriteArrayList<Pair<String, Boolean>>()

    private val installed = CopyOnWriteArrayList<LocalProfileInfo>()

    // Like LocalProfileAssistantImpl: a download holds the eUICC for all other operations
    val lock = ReentrantLock()

    override fun downloadProfile(input: ProfileDownloadInput, callback: ProfileDownloadCallback) = lock.withLock {
        sessions.incrementAndGet()
        callback.onStatusUpdate(ProfileDownloadState.Preparing())
        callback.onStatusUpdate(ProfileDownloadState.Connecting())
        callback.onStatusUpdate(ProfileDownloadState.Authenticating())
        rejection?.let { throw it }

        val confirming = ProfileDownloadState.ConfirmingDownload(metadata)
        val confirmed = callback.onStatusUpdate(confirming)
        metadataStepAnswers += confirmed
        if (!confirmed) {
            throw LocalProfileAssistant.ProfileDownloadException(
                "ES10B_ERROR_REASON_UNDEFINED", null, null, null, null
            )
        }

        prepareDownloadConfirmationCode = confirming.confirmationCode ?: input.confirmationCode
        callback.onStatusUpdate(ProfileDownloadState.Downloading())
        callback.onStatusUpdate(ProfileDownloadState.Finalizing())
        installed += LocalProfileInfo(
            metadata.iccid,
            LocalProfileInfo.State.Disabled,
            metadata.name,
            "",
            metadata.providerName,
            "",
            metadata.profileClass
        )
    }

    override fun enableProfile(iccid: String, refresh: Boolean): Boolean {
        enableCalls += iccid to refresh
        return refreshWorks || !refresh
    }

    override val profiles: List<LocalProfileInfo>
        get() = lock.withLock { installed.toList() }
    override val valid = true
    override val notifications = emptyList<LocalProfileNotification>()
    override val eID: String
        get() = lock.withLock { OpenEuiccServiceDownloadTest.EID }
    override val euiccInfo2: EuiccInfo2? = null
    override fun setEs10xMss(mss: Byte) {}
    override fun disableProfile(iccid: String, refresh: Boolean): Boolean = true
    override fun deleteProfile(iccid: String): Boolean = true
    override fun deleteNotification(seqNumber: Long): Boolean = true
    override fun handleNotification(seqNumber: Long): Boolean = true
    override fun euiccMemoryReset() {}
    override fun setNickname(iccid: String, nickname: String) {}
    override fun close() {}
}

/**
 * Like DefaultEuiccChannelManager: waitForReconnect() and invalidate() close the channel, even while
 * someone else uses it, and the next withEuiccChannel() opens a new one. Using a closed channel
 * throws here; lpac itself uses its freed context.
 */
class CarrierChannelManager(private val lpa: CarrierLpa) : EuiccChannelManager by MockEuiccChannelManager(
    MockEuiccChannel(OpenEuiccServiceDownloadTest.SLOT, OpenEuiccServiceDownloadTest.PORT, lpa)
) {
    /** How the platform reacts to the SIM refresh of a profile switch */
    @Volatile
    var onRefresh: (() -> Unit)? = null

    private var channel = ChannelLpa(lpa)

    override suspend fun <R> withEuiccChannel(
        physicalSlotId: Int,
        portId: Int,
        seId: EuiccChannel.SecureElementId,
        fn: suspend (EuiccChannel) -> R
    ): R {
        val channelLpa = synchronized(this) {
            channel.takeIf { !it.closed } ?: ChannelLpa(lpa).also { channel = it }
        }
        return fn(MockEuiccChannel(physicalSlotId, portId, channelLpa))
    }

    override suspend fun waitForReconnect(physicalSlotId: Int, portId: Int, timeoutMillis: Long) {
        onRefresh?.invoke()
        invalidate()
    }

    override fun invalidate() = lpa.lock.withLock {
        // Like LocalProfileAssistantImpl.close(), with the eUICC held; with the platform around,
        // let whoever got the channel before wait for the eUICC meanwhile (up to 1 s)
        val deadline = System.currentTimeMillis() + if (onRefresh != null) 1000 else 0
        while (!lpa.lock.hasQueuedThreads() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
        synchronized(this) { channel.close() }
    }
}

/** The LPA of one channel */
class ChannelLpa(private val lpa: CarrierLpa) : LocalProfileAssistant by lpa {
    @Volatile
    var closed = false

    override val profiles: List<LocalProfileInfo>
        get() = lpa.lock.withLock {
            check(!closed) { "Channel closed" }
            lpa.profiles
        }

    override fun close() {
        closed = true
    }
}

/**
 * EuiccChannelManagerService with the download confirmation timeout on Robolectric's clock (the
 * main looper) instead of the wall clock, so that tests can let minutes pass instantly.
 */
class VirtualClockEuiccChannelManagerService : EuiccChannelManagerService() {
    override fun launchProfileDownloadTask(
        slotId: Int,
        portId: Int,
        seId: EuiccChannel.SecureElementId,
        input: ProfileDownloadInput,
        confirmationTimeoutMillis: Long,
    ): ForegroundTaskHandle =
        super.launchProfileDownloadTask(slotId, portId, seId, input, Long.MAX_VALUE).also {
            Handler(Looper.getMainLooper()).postDelayed(
                { it.backChannel.trySend(false) },
                confirmationTimeoutMillis
            )
        }
}

/** Records EuiccManager.continueOperation() (a system API that talks to EuiccController) */
@Implements(EuiccManager::class)
class ContinueOperationRecorder : ShadowEuiccManager() {
    companion object {
        val extras = CopyOnWriteArrayList<Bundle>()
    }

    @Implementation
    protected fun continueOperation(resolutionIntent: Intent, resolutionExtras: Bundle) {
        extras += resolutionExtras
    }
}
