package im.angry.openeuicc.core

import android.content.Context
import im.angry.openeuicc.core.omapi.OmapiEuiccChannelRepository

class EuiccChannelRepositoryProxy(context: Context) : EuiccChannelRepository {
    // TODO: Make this pluggable
    private val inner: EuiccChannelRepository = OmapiEuiccChannelRepository(context)

    private var loaded = false

    override suspend fun load() {
        inner.load()
        loaded = true
    }

    override val availableChannels: List<EuiccChannel>
        get() = if (loaded) {
            inner.availableChannels
        } else {
            throw IllegalStateException("Not loaded yet")
        }
}