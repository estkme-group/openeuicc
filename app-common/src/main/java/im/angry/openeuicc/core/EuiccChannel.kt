package im.angry.openeuicc.core

import im.angry.openeuicc.util.*
import net.typeblog.lpac_jni.LocalProfileAssistant

interface EuiccChannel {
    val port: UiccPortInfoCompat

    val slotId: Int // PHYSICAL slot
    val logicalSlotId: Int
    val portId: Int

    val lpa: LocalProfileAssistant

    val valid: Boolean

    fun close()
}