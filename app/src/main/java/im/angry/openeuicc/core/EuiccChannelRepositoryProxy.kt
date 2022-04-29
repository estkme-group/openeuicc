package im.angry.openeuicc.core

import android.content.Context
import im.angry.openeuicc.core.omapi.OmapiEuiccChannelRepository

class EuiccChannelRepositoryProxy(context: Context) : EuiccChannelRepository {
    // TODO: Make this pluggable
    private val inner: EuiccChannelRepository = OmapiEuiccChannelRepository(context)

    override suspend fun load() = inner.load()

    override val availableChannels: List<EuiccChannel>?
        get() = inner.availableChannels
}