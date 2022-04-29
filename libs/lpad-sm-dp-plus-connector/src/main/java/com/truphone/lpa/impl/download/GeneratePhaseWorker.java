package com.truphone.lpa.impl.download;


import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpa.apdu.ProfileUtil;
import com.truphone.lpa.progress.DownloadProgress;
import com.truphone.lpa.progress.DownloadProgressPhase;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.rsp.dto.asn1.rspdefinitions.BoundProfilePackage;
import com.truphone.util.LogStub;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeneratePhaseWorker {
    private static final Logger LOG = Logger.getLogger(GeneratePhaseWorker.class.getName());

    private final DownloadProgress progress;

    public GeneratePhaseWorker(DownloadProgress progress) {

        this.progress = progress;
    }

    public Map<SbppApdu, List<String>> generateSbpp(String bpp) throws IOException {

        progress.setCurrentPhase(DownloadProgressPhase.GENERATING);
        List<String[]> sbpp = generateSbpps(bpp);

        return generateSbppMap(sbpp);

    }

    private Map<SbppApdu, List<String>> generateSbppMap(List<String[]> sbpp) {
        Map<SbppApdu, List<String>> sbppApduMap = new HashMap<>();

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_GENERATING_SBPP_APDUS,
                "generateSbpp generating...");

        sbppApduMap.put(SbppApdu.BOUND_PROFILE_PACKAGE, ApduUtils.loadBoundProfilePackageApdu(sbpp));
        if(sbpp.size()==5){
            //secondSequenceOf87
            sbppApduMap.put(SbppApdu.REPLACE_SESSIONS_KEYS, ApduUtils.loadProfileProtectionKeys(sbpp));
        }
        sbppApduMap.put(SbppApdu.STORE_METADATA, ApduUtils.loadStoreMetadataApdu(sbpp));
        sbppApduMap.put(SbppApdu.CONFIGURE_ISDPA, ApduUtils.loadConfigureISDPApdu(sbpp));
        sbppApduMap.put(SbppApdu.INITIALIZE_SECURE_CHANNEL, ApduUtils.loadInitialiseSecureChannelApdu(sbpp));

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_GENERATED_SBPP_APDUS,
                "generateSbpp generated...");

        return sbppApduMap;
    }

    private List<String[]> generateSbpps(String bpp) throws IOException {

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_GENERATING_SBPP,
                "generateSbpp generating...");

        ProfileUtil util = new ProfileUtil();

        logPrintBpp(bpp);

        // TODO: This should be refactored to use the jASN1 objects
        List<String[]> sbpp = util.generateSBPP(bpp);

        logPrintSbpp(sbpp);

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_GENERATED_SBPP,
                "generateSbpp generated...");

        return sbpp;
    }

    private void logPrintSbpp(List<String[]> sbpp) {

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - SBPP count: " + sbpp.size());

            for (String[] s : sbpp) {
                for (String s1 : s) {
                    LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - generateSbpp - SBPP is: " + s1);
                }
            }
        }
    }

    private void logPrintBpp(String bpp) throws IOException {

        if (LogStub.getInstance().isDebugEnabled()) {
            InputStream bppIs = null;
            BoundProfilePackage bppObj = new BoundProfilePackage();

            try {
                bppIs = new ByteArrayInputStream(Hex.decodeHex(bpp.toCharArray()));

                bppObj.decode(bppIs);

                LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - BPP Object is: " + bppObj);
            } catch (DecoderException e) {
                LOG.log(Level.INFO, LogStub.getInstance().getTag() + " - " + e.getMessage(), e);
            } finally {
                CloseResources.closeResources(bppIs);
            }
        }
    }
}
