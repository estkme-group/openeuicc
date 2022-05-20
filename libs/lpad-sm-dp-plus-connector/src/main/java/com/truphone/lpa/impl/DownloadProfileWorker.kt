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

import com.truphone.lpa.progress.DownloadProgress
import com.truphone.lpa.ApduChannel
import com.truphone.es9plus.Es9PlusImpl
import com.truphone.lpa.impl.download.ApduTransmitter
import com.truphone.lpa.impl.download.AuthenticatingPhaseWorker
import com.truphone.lpa.impl.download.DownloadPhaseWorker
import com.truphone.util.LogStub
import com.truphone.lpa.impl.download.ConnectingPhaseWorker
import com.truphone.lpa.impl.download.GeneratePhaseWorker
import com.truphone.lpa.impl.download.InstallationPhaseWorker
import java.lang.RuntimeException
import java.util.logging.Logger

internal class DownloadProfileWorker(
    private var matchingId: String,
    private val imei: String,
    private val progress: DownloadProgress,
    apduChannel: ApduChannel,
    private val es9Module: Es9PlusImpl
) {
    private val apduTransmitter = ApduTransmitter(apduChannel)

    companion object {
        private val LOG = Logger.getLogger(
            DownloadProfileWorker::class.java.name
        )
    }

    fun run() {
        val authenticatingPhaseWorker = AuthenticatingPhaseWorker(
            progress, apduTransmitter, es9Module
        )
        val downloadPhaseWorker = DownloadPhaseWorker(progress, apduTransmitter, es9Module)
        LOG.info(LogStub.getInstance().tag + " - Downloading profile with matching Id: " + matchingId)


        // AP Added this to support Activation Codes
        // If matchingId is an Activation Code, parses AC to retrieve DP Address and Matching ID.
        // Otherwise LPA shall use the default SMDP configured on the cards
        val serverAddress: String
        if (matchingId.contains("$")) {
            // Its activation code
            val acParts = matchingId.split("$").toTypedArray()
            if (acParts.size < 3) throw RuntimeException("Invalid ActivationCode format")
            serverAddress = acParts[1]
            matchingId = acParts[2]
        } else {
            serverAddress =
                ConnectingPhaseWorker(progress, apduTransmitter).getEuiccConfiguredAddress(
                    matchingId
                )
        }
        val initialAuthenticationKeys = InitialAuthenticationKeys(
            matchingId,
            serverAddress,
            authenticatingPhaseWorker.euiccInfo,
            authenticatingPhaseWorker.getEuiccChallenge(matchingId)
        )
        authenticatingPhaseWorker.initiateAuthentication(
            initialAuthenticationKeys,
            imei
        )
        downloadAndInstallProfilePackage(
            initialAuthenticationKeys,
            downloadPhaseWorker.prepareDownload(
                authenticatingPhaseWorker.authenticateClient(
                    initialAuthenticationKeys,
                    authenticatingPhaseWorker.authenticateWithEuicc(initialAuthenticationKeys)
                )
            ), downloadPhaseWorker
        )
    }

    private fun downloadAndInstallProfilePackage(
        initialAuthenticationKeys: InitialAuthenticationKeys,
        encodedPrepareDownloadResponse: String,
        downloadPhaseWorker: DownloadPhaseWorker
    ) {
        val bpp = downloadPhaseWorker.getBoundProfilePackage(
            initialAuthenticationKeys,
            encodedPrepareDownloadResponse
        )
        val sbpp = GeneratePhaseWorker(
            progress
        ).generateSbpp(bpp)
        InstallationPhaseWorker(progress, apduTransmitter).loadingSbppApdu(sbpp)
    }
}