/*
 * Copyright 2022 Peter Cai & Pierre-Hugues Husson
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, version 2.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.truphone.lpa.impl.download

import com.truphone.lpa.ApduChannel
import com.truphone.util.LogStub
import com.truphone.lpa.ApduTransmittedListener
import java.util.logging.Logger

class ApduTransmitter(private val apduChannel: ApduChannel) {
    companion object {
        private val LOG = Logger.getLogger(
            ApduTransmitter::class.java.name
        )
    }

    fun transmitApdu(apdu: String): String {
        if (LogStub.getInstance().isDebugEnabled) {
            LogStub.getInstance()
                .logDebug(LOG, LogStub.getInstance().tag + " - APDU to transmit: " + apdu)
        }
        val apduResponse = apduChannel.transmitAPDU(apdu)
        if (LogStub.getInstance().isDebugEnabled) {
            LogStub.getInstance().logDebug(
                LOG,
                LogStub.getInstance().tag + " - Transmit APDU response: " + apduResponse
            )
        }

        if (apduResponse.length < 4) {
            throw RuntimeException("APDU response should at least contain a status code")
        }

        // Last 2 bytes are the status code (should be 0x9000)
        // TODO: Do this properly
        return apduResponse.substring(0, apduResponse.length - 4)
    }

    fun transmitApdus(apdus: List<String?>): String {
        if (LogStub.getInstance().isDebugEnabled) {
            LogStub.getInstance()
                .logDebug(LOG, LogStub.getInstance().tag + " - APDUs to transmit: " + apdus)
        }
        val apduResponse = apduChannel.transmitAPDUS(apdus)
        if (LogStub.getInstance().isDebugEnabled) {
            LogStub.getInstance().logDebug(
                LOG,
                LogStub.getInstance().tag + " - Transmit APDUs response: " + apduResponse
            )
        }

        if (apduResponse.length < 4) {
            throw RuntimeException("APDU response should at least contain a status code")
        }

        // Last 2 bytes are the status code (should be 0x9000)
        // TODO: Do this properly
        return apduResponse.substring(0, apduResponse.length - 4)
    }

    fun addApduTransmittedListener(apduTransmittedListener: ApduTransmittedListener?) {
        apduChannel.setApduTransmittedListener(apduTransmittedListener)
    }

    fun removeApduTransmittedListener(apduTransmittedListener: ApduTransmittedListener?) {
        apduChannel.removeApduTransmittedListener(apduTransmittedListener)
    }
}