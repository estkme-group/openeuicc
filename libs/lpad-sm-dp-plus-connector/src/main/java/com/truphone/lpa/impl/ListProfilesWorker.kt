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
import com.truphone.lpa.LocalProfileInfo
import com.truphone.rsp.dto.asn1.rspdefinitions.ProfileInfoListResponse
import com.truphone.util.LogStub
import com.truphone.lpa.apdu.ApduUtils
import com.truphone.util.TextUtil
import com.truphone.util.TextUtil.iccidBigToLittle
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.NumberFormatException
import java.lang.RuntimeException
import java.util.logging.Level
import java.util.logging.Logger

internal class ListProfilesWorker(private val apduChannel: ApduChannel) {
    fun run(): List<LocalProfileInfo> {
        val profilesInfo = profileInfoListResponse
        val profiles = ProfileInfoListResponse()
        val profileList: MutableList<LocalProfileInfo> = mutableListOf()
        return try {
            decodeProfiles(profilesInfo, profiles)
            for (info in profiles.profileInfoListOk.profileInfo) {
                profileList.add(
                    LocalProfileInfo(
                        iccid = iccidBigToLittle(info.iccid.toString()),
                        state = LocalProfileInfo.stateFromString(info.profileState?.toString()),
                        name = info.profileName?.toString() ?: "",
                        nickName = info.profileNickname?.toString() ?: "",
                        providerName = info.serviceProviderName?.toString() ?: "",
                        isdpAID = info.isdpAid?.toString() ?: "",
                        profileClass = LocalProfileInfo.classFromString(info.profileClass?.toString())
                    )
                )
            }
            if (LogStub.getInstance().isDebugEnabled) {
                LogStub.getInstance().logDebug(
                    LOG,
                    LogStub.getInstance().tag + " - getProfiles - returning: " + profileList.toString()
                )
            }
            profileList
        } catch (e: NumberFormatException) {
            LOG.log(Level.SEVERE, LogStub.getInstance().tag + " - " + e.message, e)
            LOG.log(
                Level.SEVERE,
                LogStub.getInstance().tag + " -  Unable to retrieve profiles. Exception in Decoder:" + e.message
            )
            throw RuntimeException("Unable to retrieve profiles")
        } catch (ioe: IOException) {
            LOG.log(Level.SEVERE, LogStub.getInstance().tag + " - " + ioe.message, ioe)
            throw RuntimeException("Unable to retrieve profiles")
        }
    }

    @Throws(NumberFormatException::class, IOException::class)
    private fun decodeProfiles(profilesInfo: String, profiles: ProfileInfoListResponse) {
        val `is`: InputStream = ByteArrayInputStream(TextUtil.decodeHex(profilesInfo))
        profiles.decode(`is`)
        if (LogStub.getInstance().isDebugEnabled) {
            LogStub.getInstance().logDebug(LOG, "Profile list object: $profiles")
        }
    }

    private val profileInfoListResponse: String
        get() {
            if (LogStub.getInstance().isDebugEnabled) {
                LogStub.getInstance()
                    .logDebug(LOG, LogStub.getInstance().tag + " - Getting Profiles")
            }
            val apdu = ApduUtils.getProfilesInfoApdu(null)
            if (LogStub.getInstance().isDebugEnabled) {
                LogStub.getInstance().logDebug(LOG, "List profiles APDU: $apdu")
            }
            return apduChannel.transmitAPDU(apdu)
        }

    companion object {
        private val LOG = Logger.getLogger(
            ListProfilesWorker::class.java.name
        )
    }
}