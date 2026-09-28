package im.angry.openeuicc.service

import android.content.Intent
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.service.EuiccChannelManagerService.ForegroundTaskState
import im.angry.openeuicc.testutil.MockEuiccChannel
import im.angry.openeuicc.testutil.MockEuiccChannelManager
import im.angry.openeuicc.testutil.MockLpa
import im.angry.openeuicc.testutil.TestOpenEuiccApplication
import im.angry.openeuicc.testutil.awaitMainLooper
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows.shadowOf
import net.typeblog.lpac_jni.ProfileDownloadInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests the real EuiccChannelManagerService (Robolectric) with only the manager /
 * channel / LPA mocked -- i.e. the exact service that runs in production.
 *
 * These tests pin the profile-download confirmation contract that regressed
 * during the "merge back-communication channels" refactor: the task MUST block
 * at ProfileDownloadState.ConfirmingDownload until a Boolean is sent on the
 * handle's back channel. `true` continues the download, `false` cancels it.
 * UI callers are expected to send that signal through launchProfileDownload()
 * (see DownloadTaskLauncherTest).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestOpenEuiccApplication::class)
class EuiccChannelManagerServiceTest {

    private val seId = EuiccChannel.SecureElementId.DEFAULT
    private val input = ProfileDownloadInput(
        address = "smdp.example.com",
        matchingId = "matching-id",
        imei = "imei",
        confirmationCode = "conf-code"
    )

    private lateinit var service: EuiccChannelManagerService
    private lateinit var lpa: MockLpa

    @Before
    fun setUp() {
        val channel = MockEuiccChannel(slotId = 1, portId = 2)
        lpa = channel.lpa as MockLpa
        TestOpenEuiccApplication.mockEuiccChannelManager = MockEuiccChannelManager(channel)
        service = Robolectric.buildService(EuiccChannelManagerService::class.java).get()
    }

    /**
     * The service self-starts via startForegroundService(); Robolectric does not
     * deliver that to onStartCommand() on its own, so simulate the system call.
     * Must be invoked after the launch coroutine has had a chance to subscribe to
     * foregroundStarted (otherwise the emission is dropped -- foregroundStarted
     * has no replay). In production the system delivers the start asynchronously,
     * so the subscriber is always ready by then.
     */
    private fun startService() {
        shadowOf(Looper.getMainLooper()).idle()
        service.onStartCommand(Intent(), 0, 1)
    }

    private suspend fun awaitTaskDone(
        handle: EuiccChannelManagerService.ForegroundTaskHandle
    ): ForegroundTaskState.Done = coroutineScope {
        // Written by the collector on Dispatchers.Default while awaitMainLooper() reads it
        val states = java.util.concurrent.CopyOnWriteArrayList<ForegroundTaskState>()
        val collector = async(Dispatchers.Default) { handle.stateFlow.collect { states += it } }
        try {
            awaitMainLooper {
                states.any { it is ForegroundTaskState.Done }
            }
        } finally {
            collector.cancel()
        }
        states.last() as ForegroundTaskState.Done
    }

    @Test
    fun `download task blocks at ConfirmingDownload until the subscriber sends true`() = runBlocking {
        val handle = service.launchProfileDownloadTask(1, 2, seId, input)
        startService()

        // The task must reach the LPA and stall at the confirmation step.
        awaitMainLooper { lpa.downloadStarted.isCompleted }
        assertFalse(
            "download must be waiting for confirmation",
            lpa.downloadReturned.isCompleted
        )

        // A subscriber (e.g. DownloadWizardProgressFragment) confirms via the back channel.
        handle.backChannel.send(true)

        // The LPA callback must have been told to continue.
        assertEquals(true, lpa.awaitDownloadResult())

        // The task must then complete successfully.
        val done = awaitTaskDone(handle)
        assertNull(done.error)
    }

    @Test
    fun `download task cancels when the subscriber sends false`() = runBlocking {
        val handle = service.launchProfileDownloadTask(1, 2, seId, input)
        startService()

        awaitMainLooper { lpa.downloadStarted.isCompleted }
        assertFalse(
            "download must be waiting for confirmation",
            lpa.downloadReturned.isCompleted
        )

        // A subscriber decides to cancel.
        handle.backChannel.send(false)

        // The LPA callback must have been told to cancel.
        assertEquals(false, lpa.awaitDownloadResult())

        // The task itself still completes (the cancellation is communicated via the
        // callback result, not an exception).
        val done = awaitTaskDone(handle)
        assertNull(done.error)
    }

    @Test
    fun `download task cancels when no confirmation arrives within the timeout`() = runBlocking {
        val handle = service.launchProfileDownloadTask(
            1, 2, seId, input,
            confirmationTimeoutMillis = 100
        )
        startService()

        awaitMainLooper { lpa.downloadStarted.isCompleted }

        // Nothing is sent on the back channel.
        assertEquals(false, lpa.awaitDownloadResult())
        // A late confirmation is refused
        assertFalse(handle.backChannel.trySend(true).isSuccess)
        assertNull(awaitTaskDone(handle).error)
    }
}
