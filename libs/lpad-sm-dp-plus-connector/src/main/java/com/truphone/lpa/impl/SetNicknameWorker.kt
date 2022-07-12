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

package com.truphone.lpa.impl

import com.truphone.lpa.ApduChannel
import com.truphone.util.LogStub
import com.truphone.lpa.apdu.ApduUtils
import com.truphone.rsp.dto.asn1.rspdefinitions.SetNicknameResponse
import com.truphone.util.TextUtil
import com.truphone.util.Util
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.NumberFormatException
import java.lang.RuntimeException
import java.util.logging.Level
import java.util.logging.Logger

class SetNicknameWorker internal constructor(
    private val iccid: String,
    private val nickname: String,
    private val apduChannel: ApduChannel
) {
    companion object {
        private val LOG = Logger.getLogger(
            ListProfilesWorker::class.java.name
        )
    }

    fun run(): Boolean {
        if (LogStub.getInstance().isDebugEnabled) {
            LogStub.getInstance()
                .logDebug(LOG, LogStub.getInstance().tag + " - Renaming profile: " + iccid)
        }
        val apdu = ApduUtils.setNicknameApdu(
            iccid, Util.byteArrayToHexString(
                nickname.toByteArray(), ""
            )
        )
        val eResponse = apduChannel.transmitAPDU(apdu)
        return try {
            val `is`: InputStream = ByteArrayInputStream(TextUtil.decodeHex(eResponse))
            val response = SetNicknameResponse()
            response.decode(`is`)
            if ("0" == response.setNicknameResult.toString()) {
                if (LogStub.getInstance().isDebugEnabled) {
                    LogStub.getInstance()
                        .logDebug(LOG, LogStub.getInstance().tag + " - Profile renamed: " + iccid)
                }
                true
            } else {
                if (LogStub.getInstance().isDebugEnabled) {
                    LogStub.getInstance().logDebug(
                        LOG,
                        LogStub.getInstance().tag + " - Profile not renamed: " + iccid
                    )
                }
                false
            }
        } catch (ioe: IOException) {
            LOG.log(
                Level.SEVERE,
                LogStub.getInstance().tag + " - iccid: " + iccid + " profile failed to be renamed"
            )
            throw RuntimeException("Unable to rename profile: $iccid, response: $eResponse")
        } catch (e: NumberFormatException) {
            LOG.log(Level.SEVERE, LogStub.getInstance().tag + " - " + e.message, e)
            LOG.log(
                Level.SEVERE,
                LogStub.getInstance().tag + " - iccid: " + iccid + " profile failed to be renamed. Exception in Decoder:" + e.message
            )
            throw RuntimeException("Unable to rename profile: $iccid, response: $eResponse")
        }
    }
}