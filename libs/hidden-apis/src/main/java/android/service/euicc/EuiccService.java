/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.service.euicc;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccInfo;
import java.io.PrintWriter;

/**
 * Service interface linking the system with an eUICC local profile assistant (LPA) application.
 *
 * <p>An LPA consists of two separate components (which may both be implemented in the same APK):
 * the LPA backend, and the LPA UI or LUI.
 *
 * @hide
 */
public abstract class EuiccService extends Service {
    /** Action which must be included in this service's intent filter. */
    public static final String EUICC_SERVICE_INTERFACE = "android.service.euicc.EuiccService";
    /** Category which must be defined to all UI actions, for efficient lookup. */
    public static final String CATEGORY_EUICC_UI = "android.service.euicc.category.EUICC_UI";
    // LUI actions. These are passthroughs of the corresponding EuiccManager actions.
    /**
     * Action used to bind the carrier app and get the activation code from the carrier app. This
     * activation code will be used to download the eSIM profile during eSIM activation flow.
     */
    public static final String ACTION_BIND_CARRIER_PROVISIONING_SERVICE =
            "android.service.euicc.action.BIND_CARRIER_PROVISIONING_SERVICE";
    /**
     * Intent action sent by the LPA to launch a carrier app Activity for eSIM activation, e.g. a
     * carrier login screen. Carrier apps wishing to support this activation method must implement
     * an Activity that responds to this intent action. Upon completion, the Activity must return
     * one of the following results to the LPA:
     *
     * <p>{@code Activity.RESULT_CANCELED}: The LPA should treat this as an back button and abort
     * the activation flow.
     * <p>{@code Activity.RESULT_OK}: The LPA should try to get an activation code from the carrier
     * app by binding to the carrier app service implementing
     * {@link #ACTION_BIND_CARRIER_PROVISIONING_SERVICE}.
     * <p>{@code Activity.RESULT_OK} with
     * {@link android.telephony.euicc.EuiccManager#EXTRA_USE_QR_SCANNER} set to true: The LPA should
     * start a QR scanner for the user to scan an eSIM profile QR code.
     * <p>For other results: The LPA should treat this as an error.
     **/
    public static final String ACTION_START_CARRIER_ACTIVATION =
            "android.service.euicc.action.START_CARRIER_ACTIVATION";
    /**
     * @see android.telephony.euicc.EuiccManager#ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS
     * The difference is this one is used by system to bring up the LUI.
     */
    public static final String ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS =
            "android.service.euicc.action.MANAGE_EMBEDDED_SUBSCRIPTIONS";
    public static final String ACTION_PROVISION_EMBEDDED_SUBSCRIPTION =
            "android.service.euicc.action.PROVISION_EMBEDDED_SUBSCRIPTION";
    public static final String ACTION_TOGGLE_SUBSCRIPTION_PRIVILEGED =
            "android.service.euicc.action.TOGGLE_SUBSCRIPTION_PRIVILEGED";
    public static final String ACTION_DELETE_SUBSCRIPTION_PRIVILEGED =
            "android.service.euicc.action.DELETE_SUBSCRIPTION_PRIVILEGED";
    public static final String ACTION_RENAME_SUBSCRIPTION_PRIVILEGED =
            "android.service.euicc.action.RENAME_SUBSCRIPTION_PRIVILEGED";
    public static final String ACTION_START_EUICC_ACTIVATION =
            "android.service.euicc.action.START_EUICC_ACTIVATION";
    // LUI resolution actions. These are called by the platform to resolve errors in situations that
    // require user interaction.
    // TODO(b/33075886): Define extras for any input parameters to these dialogs once they are
    // more scoped out.
    /**
     * Alert the user that this action will result in an active SIM being deactivated.
     * To implement the LUI triggered by the system, you need to define this in AndroidManifest.xml.
     */
    public static final String ACTION_RESOLVE_DEACTIVATE_SIM =
            "android.service.euicc.action.RESOLVE_DEACTIVATE_SIM";
    /**
     * Alert the user about a download/switch being done for an app that doesn't currently have
     * carrier privileges.
     */
    public static final String ACTION_RESOLVE_NO_PRIVILEGES =
            "android.service.euicc.action.RESOLVE_NO_PRIVILEGES";
    /**
     * Ask the user to input carrier confirmation code.
     *
     * @deprecated From Q, the resolvable errors happened in the download step are presented as
     * bit map in {@link #EXTRA_RESOLVABLE_ERRORS}. The corresponding action would be
     * {@link #ACTION_RESOLVE_RESOLVABLE_ERRORS}.
     */
    @Deprecated
    public static final String ACTION_RESOLVE_CONFIRMATION_CODE =
            "android.service.euicc.action.RESOLVE_CONFIRMATION_CODE";
    /** Ask the user to resolve all the resolvable errors. */
    public static final String ACTION_RESOLVE_RESOLVABLE_ERRORS =
            "android.service.euicc.action.RESOLVE_RESOLVABLE_ERRORS";
    /**
     * Possible value for the bit map of resolvable errors indicating the download process needs
     * the user to input confirmation code.
     */
    public static final int RESOLVABLE_ERROR_CONFIRMATION_CODE = 1 << 0;
    /**
     * Possible value for the bit map of resolvable errors indicating the download process needs
     * the user's consent to allow profile policy rules.
     */
    public static final int RESOLVABLE_ERROR_POLICY_RULES = 1 << 1;
    /**
     * Intent extra set for resolution requests containing the package name of the calling app.
     * This is used by the above actions including ACTION_RESOLVE_DEACTIVATE_SIM,
     * ACTION_RESOLVE_NO_PRIVILEGES and ACTION_RESOLVE_RESOLVABLE_ERRORS.
     */
    public static final String EXTRA_RESOLUTION_CALLING_PACKAGE =
            "android.service.euicc.extra.RESOLUTION_CALLING_PACKAGE";
    /**
     * Intent extra set for resolution requests containing the list of resolvable errors to be
     * resolved. Each resolvable error is an integer. Its possible values include:
     * <UL>
     * <LI>{@link #RESOLVABLE_ERROR_CONFIRMATION_CODE}
     * <LI>{@link #RESOLVABLE_ERROR_POLICY_RULES}
     * </UL>
     */
    public static final String EXTRA_RESOLVABLE_ERRORS =
            "android.service.euicc.extra.RESOLVABLE_ERRORS";
    /**
     * Intent extra set for resolution requests containing a boolean indicating whether to ask the
     * user to retry another confirmation code.
     */
    public static final String EXTRA_RESOLUTION_CONFIRMATION_CODE_RETRIED =
            "android.service.euicc.extra.RESOLUTION_CONFIRMATION_CODE_RETRIED";
    /**
     * Intent extra set for resolution requests containing an int indicating the current card Id.
     */
    public static final String EXTRA_RESOLUTION_CARD_ID =
            "android.service.euicc.extra.RESOLUTION_CARD_ID";
    /**
     * Intent extra set for resolution requests containing an int indicating the current port index.
     */
    public static final String EXTRA_RESOLUTION_PORT_INDEX =
            "android.service.euicc.extra.RESOLUTION_PORT_INDEX";
    /**
     * Intent extra set for resolution requests containing a bool indicating whether to use the
     * given port index.
     */
    public static final String EXTRA_RESOLUTION_USE_PORT_INDEX =
            "android.service.euicc.extra.RESOLUTION_USE_PORT_INDEX";
    /** Result code for a successful operation. */
    public static final int RESULT_OK = 0;
    /** Result code indicating that an active SIM must be deactivated to perform the operation. */
    public static final int RESULT_MUST_DEACTIVATE_SIM = -1;
    /** Result code indicating that the user must resolve resolvable errors. */
    public static final int RESULT_RESOLVABLE_ERRORS = -2;
    /**
     * Result code indicating that the user must input a carrier confirmation code.
     *
     * @deprecated From Q, the resolvable errors happened in the download step are presented as
     * bit map in {@link #EXTRA_RESOLVABLE_ERRORS}. The corresponding result would be
     * {@link #RESULT_RESOLVABLE_ERRORS}.
     */
    @Deprecated
    public static final int RESULT_NEED_CONFIRMATION_CODE = -2;
    // New predefined codes should have negative values.
    /** Start of implementation-specific error results. */
    public static final int RESULT_FIRST_USER = 1;
    /**
     * Boolean extra for resolution actions indicating whether the user granted consent.
     * This is used and set by the implementation and used in {@code EuiccOperation}.
     */
    public static final String EXTRA_RESOLUTION_CONSENT =
            "android.service.euicc.extra.RESOLUTION_CONSENT";
    /**
     * String extra for resolution actions indicating the carrier confirmation code.
     * This is used and set by the implementation and used in {@code EuiccOperation}.
     */
    public static final String EXTRA_RESOLUTION_CONFIRMATION_CODE =
            "android.service.euicc.extra.RESOLUTION_CONFIRMATION_CODE";
    /**
     * String extra for resolution actions indicating whether the user allows policy rules.
     * This is used and set by the implementation and used in {@code EuiccOperation}.
     */
    public static final String EXTRA_RESOLUTION_ALLOW_POLICY_RULES =
            "android.service.euicc.extra.RESOLUTION_ALLOW_POLICY_RULES";

    public EuiccService() {
    }
    /**
     * Given a SubjectCode[5.2.6.1] and ReasonCode[5.2.6.2] from GSMA (SGP.22 v2.2), encode it to
     * the format described in
     * {@link android.telephony.euicc.EuiccManager#OPERATION_SMDX_SUBJECT_REASON_CODE}
     *
     * @param subjectCode SubjectCode[5.2.6.1] from GSMA (SGP.22 v2.2)
     * @param reasonCode  ReasonCode[5.2.6.2] from GSMA (SGP.22 v2.2)
     * @return encoded error code described in
     * {@link android.telephony.euicc.EuiccManager#OPERATION_SMDX_SUBJECT_REASON_CODE}
     * @throws NumberFormatException         when the Subject/Reason code contains non digits
     * @throws IllegalArgumentException      when Subject/Reason code is null/empty
     * @throws UnsupportedOperationException when sections has more than four layers (e.g 5.8.1.2)
     *                                       or when an number is bigger than 15
     */
    public int encodeSmdxSubjectAndReasonCode(String subjectCode,
                                              String reasonCode) {
        return 0;
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    /**
     * If overriding this method, call through to the super method for any unknown actions.
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    /**
     * Callback class for {@link #onStartOtaIfNecessary(int, OtaStatusChangedCallback)}
     *
     * The status of OTA which can be {@code android.telephony.euicc.EuiccManager#EUICC_OTA_}
     */
    public abstract static class OtaStatusChangedCallback {
        /** Called when OTA status is changed. */
        public abstract void onOtaStatusChanged(int status);
    }
    /**
     * Return the EID of the eUICC.
     *
     * @param slotId ID of the SIM slot being queried.
     * @return the EID.
     * @see android.telephony.euicc.EuiccManager#getEid
     */
    // TODO(b/36260308): Update doc when we have multi-SIM support.
    public abstract String onGetEid(int slotId);
    /**
     * Return the status of OTA update.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @return The status of Euicc OTA update.
     */
    public abstract int onGetOtaStatus(int slotId);
    /**
     * Perform OTA if current OS is not the latest one.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @param statusChangedCallback Function called when OTA status changed.
     */
    public abstract void onStartOtaIfNecessary(
            int slotId, OtaStatusChangedCallback statusChangedCallback);
    /**
     * Populate {@link DownloadableSubscription} metadata for the given downloadable subscription.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @param subscription A subscription whose metadata needs to be populated.
     * @param forceDeactivateSim If true, and if an active SIM must be deactivated to access the
     *     eUICC, perform this action automatically. Otherwise, {@link #RESULT_MUST_DEACTIVATE_SIM)}
     *     should be returned to allow the user to consent to this operation first.
     * @return The result of the operation.
     */
    public abstract GetDownloadableSubscriptionMetadataResult onGetDownloadableSubscriptionMetadata(
            int slotId, DownloadableSubscription subscription, boolean forceDeactivateSim);
    /**
     * Return metadata for subscriptions which are available for download for this device.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @param forceDeactivateSim If true, and if an active SIM must be deactivated to access the
     *     eUICC, perform this action automatically. Otherwise, {@link #RESULT_MUST_DEACTIVATE_SIM)}
     *     should be returned to allow the user to consent to this operation first.
     * @return The result of the list operation.
     */
    public abstract GetDefaultDownloadableSubscriptionListResult
    onGetDefaultDownloadableSubscriptionList(int slotId, boolean forceDeactivateSim);
    /**
     * Download the given subscription.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @param subscription The subscription to download.
     * @param switchAfterDownload If true, the subscription should be enabled upon successful
     *     download.
     * @param forceDeactivateSim If true, and if an active SIM must be deactivated to access the
     *     eUICC, perform this action automatically. Otherwise, {@link #RESULT_MUST_DEACTIVATE_SIM}
     *     should be returned to allow the user to consent to this operation first.
     * @param resolvedBundle The bundle containing information on resolved errors. It can contain
     *     a string of confirmation code for the key {@link #EXTRA_RESOLUTION_CONFIRMATION_CODE},
     *     and a boolean for key {@link #EXTRA_RESOLUTION_ALLOW_POLICY_RULES} indicating whether
     *     the user allows profile policy rules or not.
     * @return a DownloadSubscriptionResult instance including a result code, a resolvable errors
     *     bit map, and original the card Id. The result code may be one of the predefined
     *     {@code RESULT_} constants or any implementation-specific code starting with
     *     {@link #RESULT_FIRST_USER}. The resolvable error bit map can be either 0 or values
     *     defined in {@code RESOLVABLE_ERROR_}. A subclass should override this method. Otherwise,
     *     this method does nothing and returns null by default.
     * @see android.telephony.euicc.EuiccManager#downloadSubscription
     */
    public DownloadSubscriptionResult onDownloadSubscription(int slotId,
                                                             DownloadableSubscription subscription, boolean switchAfterDownload,
                                                             boolean forceDeactivateSim, Bundle resolvedBundle) {
        return null;
    }
    /**
     * Download the given subscription.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @param subscription The subscription to download.
     * @param switchAfterDownload If true, the subscription should be enabled upon successful
     *     download.
     * @param forceDeactivateSim If true, and if an active SIM must be deactivated to access the
     *     eUICC, perform this action automatically. Otherwise, {@link #RESULT_MUST_DEACTIVATE_SIM}
     *     should be returned to allow the user to consent to this operation first.
     * @return the result of the download operation. May be one of the predefined {@code RESULT_}
     *     constants or any implementation-specific code starting with {@link #RESULT_FIRST_USER}.
     * @see android.telephony.euicc.EuiccManager#downloadSubscription
     *
     * @deprecated From Q, a subclass should use and override the above
     * {@link #onDownloadSubscription(int, DownloadableSubscription, boolean, boolean, Bundle)}. The
     * default return value for this one is Integer.MIN_VALUE.
     */
    @Deprecated public int onDownloadSubscription(int slotId,
                                                          DownloadableSubscription subscription, boolean switchAfterDownload,
                                                          boolean forceDeactivateSim) {
        return Integer.MIN_VALUE;
    }
    /**
     * Return a list of all @link EuiccProfileInfo}s.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @return The result of the operation.
     * @see android.telephony.SubscriptionManager#getAccessibleSubscriptionInfoList
     */
    public abstract GetEuiccProfileInfoListResult onGetEuiccProfileInfoList(int slotId);
    /**
     * Return info about the eUICC chip/device.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @return the {@link EuiccInfo} for the eUICC chip/device.
     * @see android.telephony.euicc.EuiccManager#getEuiccInfo
     */
    public abstract EuiccInfo onGetEuiccInfo(int slotId);
    /**
     * Delete the given subscription.
     *
     * <p>If the subscription is currently active, it should be deactivated first (equivalent to a
     * physical SIM being ejected).
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @param iccid the ICCID of the subscription to delete.
     * @return the result of the delete operation. May be one of the predefined {@code RESULT_}
     *     constants or any implementation-specific code starting with {@link #RESULT_FIRST_USER}.
     * @see android.telephony.euicc.EuiccManager#deleteSubscription
     */
    public abstract  int onDeleteSubscription(int slotId, String iccid);
    /**
     * Switch to the given subscription.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @param iccid the ICCID of the subscription to enable. May be null, in which case the current
     *     profile should be deactivated and no profile should be activated to replace it - this is
     *     equivalent to a physical SIM being ejected.
     * @param forceDeactivateSim If true, and if an active SIM must be deactivated to access the
     *     eUICC, perform this action automatically. Otherwise, {@link #RESULT_MUST_DEACTIVATE_SIM}
     *     should be returned to allow the user to consent to this operation first.
     * @return the result of the switch operation. May be one of the predefined {@code RESULT_}
     *     constants or any implementation-specific code starting with {@link #RESULT_FIRST_USER}.
     * @see android.telephony.euicc.EuiccManager#switchToSubscription
     *
     * @deprecated prefer {@link #onSwitchToSubscriptionWithPort(int, int, String, boolean)}
     */
    @Deprecated public abstract int onSwitchToSubscription(int slotId,
                                                                   String iccid, boolean forceDeactivateSim);
    /**
     * Switch to the given subscription.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @param portIndex which port on the eUICC to use
     * @param iccid the ICCID of the subscription to enable. May be null, in which case the current
     *     profile should be deactivated and no profile should be activated to replace it - this is
     *     equivalent to a physical SIM being ejected.
     * @param forceDeactivateSim If true, and if an active SIM must be deactivated to access the
     *     eUICC, perform this action automatically. Otherwise, {@link #RESULT_MUST_DEACTIVATE_SIM}
     *     should be returned to allow the user to consent to this operation first.
     * @return the result of the switch operation. May be one of the predefined {@code RESULT_}
     *     constants or any implementation-specific code starting with {@link #RESULT_FIRST_USER}.
     * @see android.telephony.euicc.EuiccManager#switchToSubscription
     */
    public int onSwitchToSubscriptionWithPort(int slotId, int portIndex,
                                                      String iccid, boolean forceDeactivateSim) {
        // stub implementation, LPA needs to implement this
        throw new UnsupportedOperationException("LPA must override onSwitchToSubscriptionWithPort");
    }
    /**
     * Update the nickname of the given subscription.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @param iccid the ICCID of the subscription to update.
     * @param nickname the new nickname to apply.
     * @return the result of the update operation. May be one of the predefined {@code RESULT_}
     *     constants or any implementation-specific code starting with {@link #RESULT_FIRST_USER}.
     * @see android.telephony.euicc.EuiccManager#updateSubscriptionNickname
     */
    public abstract int onUpdateSubscriptionNickname(int slotId, String iccid,
                                                     String nickname);
    /**
     * Erase all operational subscriptions on the device.
     *
     * <p>This is intended to be used for device resets. As such, the reset should be performed even
     * if an active SIM must be deactivated in order to access the eUICC.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @return the result of the erase operation. May be one of the predefined {@code RESULT_}
     *     constants or any implementation-specific code starting with {@link #RESULT_FIRST_USER}.
     *
     * @deprecated From R, callers should specify a flag for specific set of subscriptions to erase
     * and use {@link #onEraseSubscriptions(int, int)} instead
     */
    @Deprecated
    public abstract int onEraseSubscriptions(int slotId);
    /**
     * Erase specific subscriptions on the device.
     *
     * <p>This is intended to be used for device resets. As such, the reset should be performed even
     * if an active SIM must be deactivated in order to access the eUICC.
     *
     * @param slotIndex index of the SIM slot to use for the operation.
     * @param options flag for specific group of subscriptions to erase
     * @return the result of the erase operation. May be one of the predefined {@code RESULT_}
     *     constants or any implementation-specific code starting with {@link #RESULT_FIRST_USER}.
     */
    public int onEraseSubscriptions(int slotIndex, int options) {
        throw new UnsupportedOperationException(
                "This method must be overridden to enable the ResetOption parameter");
    }
    /**
     * Ensure that subscriptions will be retained on the next factory reset.
     *
     * <p>Called directly before a factory reset. Assumes that a normal factory reset will lead to
     * profiles being erased on first boot (to cover fastboot/recovery wipes), so the implementation
     * should persist some bit that will remain accessible after the factory reset to bypass this
     * flow when this method is called.
     *
     * @param slotId ID of the SIM slot to use for the operation.
     * @return the result of the operation. May be one of the predefined {@code RESULT_} constants
     *     or any implementation-specific code starting with {@link #RESULT_FIRST_USER}.
     */
    public abstract int onRetainSubscriptionsForFactoryReset(int slotId);
    /**
     * Dump to a provided printWriter.
     */
    public void dump(PrintWriter printWriter) {
        printWriter.println("The connected LPA does not implement EuiccService#dump()");
    }
}