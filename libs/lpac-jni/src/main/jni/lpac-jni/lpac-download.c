#include <euicc/es9p.h>
#include <euicc/es10b.h>
#include <stdlib.h>
#include <string.h>
#include <syslog.h>
#include "lpac-download.h"

jobject download_state_preparing;
jobject download_state_connecting;
jobject download_state_authenticating;
jobject download_state_downloading;
jobject download_state_finalizing;

jmethodID on_state_update;

void lpac_download_init() {
    LPAC_JNI_SETUP_ENV;

    jclass download_state_class = (*env)->FindClass(env,
                                                    "net/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState");
    jfieldID download_state_preparing_field = (*env)->GetStaticFieldID(env, download_state_class,
                                                                       "Preparing",
                                                                       "Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;");
    download_state_preparing = (*env)->GetStaticObjectField(env, download_state_class,
                                                            download_state_preparing_field);
    download_state_preparing = (*env)->NewGlobalRef(env, download_state_preparing);
    jfieldID download_state_connecting_field = (*env)->GetStaticFieldID(env, download_state_class,
                                                                        "Connecting",
                                                                        "Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;");
    download_state_connecting = (*env)->GetStaticObjectField(env, download_state_class,
                                                             download_state_connecting_field);
    download_state_connecting = (*env)->NewGlobalRef(env, download_state_connecting);
    jfieldID download_state_authenticating_field = (*env)->GetStaticFieldID(env,
                                                                            download_state_class,
                                                                            "Authenticating",
                                                                            "Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;");
    download_state_authenticating = (*env)->GetStaticObjectField(env, download_state_class,
                                                                 download_state_authenticating_field);
    download_state_authenticating = (*env)->NewGlobalRef(env, download_state_authenticating);
    jfieldID download_state_downloading_field = (*env)->GetStaticFieldID(env, download_state_class,
                                                                         "Downloading",
                                                                         "Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;");
    download_state_downloading = (*env)->GetStaticObjectField(env, download_state_class,
                                                              download_state_downloading_field);
    download_state_downloading = (*env)->NewGlobalRef(env, download_state_downloading);
    jfieldID download_state_finalizng_field = (*env)->GetStaticFieldID(env, download_state_class,
                                                                       "Finalizing",
                                                                       "Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;");
    download_state_finalizing = (*env)->GetStaticObjectField(env, download_state_class,
                                                             download_state_finalizng_field);
    download_state_finalizing = (*env)->NewGlobalRef(env, download_state_finalizing);

    jclass download_callback_class = (*env)->FindClass(env,
                                                       "net/typeblog/lpac_jni/ProfileDownloadCallback");
    on_state_update = (*env)->GetMethodID(env, download_callback_class, "onStateUpdate",
                                          "(Lnet/typeblog/lpac_jni/ProfileDownloadCallback$DownloadState;)V");
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_downloadProfile(JNIEnv *env, jobject thiz, jlong handle,
                                                    jstring smdp, jstring matching_id,
                                                    jstring imei, jstring confirmation_code,
                                                    jobject callback) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    struct es10b_load_bound_profile_package_result es10b_load_bound_profile_package_result;
    const char *_confirmation_code = NULL;
    const char *_matching_id = NULL;
    const char *_smdp = NULL;
    const char *_imei = NULL;
    int ret;

    if (confirmation_code != NULL)
        _confirmation_code = (*env)->GetStringUTFChars(env, confirmation_code, NULL);
    if (matching_id != NULL)
        _matching_id = (*env)->GetStringUTFChars(env, matching_id, NULL);
    _smdp = (*env)->GetStringUTFChars(env, smdp, NULL);
    if (imei != NULL)
        _imei = (*env)->GetStringUTFChars(env, imei, NULL);

    ctx->http.server_address = _smdp;

    (*env)->CallVoidMethod(env, callback, on_state_update, download_state_preparing);
    ret = es10b_get_euicc_challenge_and_info(ctx);
    syslog(LOG_INFO, "es10b_get_euicc_challenge_and_info %d", ret);
    if (ret < 0) {
        ret = -ES10B_ERROR_REASON_UNDEFINED;
        goto out;
    }

    (*env)->CallVoidMethod(env, callback, on_state_update, download_state_connecting);
    ret = es9p_initiate_authentication(ctx);
    syslog(LOG_INFO, "es9p_initiate_authentication %d", ret);
    if (ret < 0) {
        ret = -ES10B_ERROR_REASON_UNDEFINED;
        goto out;
    }

    (*env)->CallVoidMethod(env, callback, on_state_update, download_state_authenticating);
    ret = es10b_authenticate_server(ctx, _matching_id, _imei);
    syslog(LOG_INFO, "es10b_authenticate_server %d", ret);
    if (ret < 0) {
        ret = -ES10B_ERROR_REASON_UNDEFINED;
        goto out;
    }

    ret = es9p_authenticate_client(ctx);
    if (ret < 0) {
        ret = -ES10B_ERROR_REASON_UNDEFINED;
        goto out;
    }

    (*env)->CallVoidMethod(env, callback, on_state_update, download_state_downloading);
    ret = es10b_prepare_download(ctx, _confirmation_code);
    syslog(LOG_INFO, "es10b_prepare_download %d", ret);
    if (ret < 0) {
        ret = -ES10B_ERROR_REASON_UNDEFINED;
        goto out;
    }

    ret = es9p_get_bound_profile_package(ctx);
    if (ret < 0)
        goto out;

    (*env)->CallVoidMethod(env, callback, on_state_update, download_state_finalizing);
    ret = es10b_load_bound_profile_package(ctx, &es10b_load_bound_profile_package_result);
    syslog(LOG_INFO, "es10b_load_bound_profile_package %d, reason %d", ret, es10b_load_bound_profile_package_result.errorReason);
    if (ret < 0) {
        ret = - (int) es10b_load_bound_profile_package_result.errorReason;
        goto out;
    }

    euicc_http_cleanup(ctx);

    out:
    // We expect Java side to call cancelSessions after any error -- thus, `euicc_http_cleanup` is done there
    // This is so that Java side can access the last HTTP and/or APDU errors when we return.
    if (_confirmation_code != NULL)
        (*env)->ReleaseStringUTFChars(env, confirmation_code, _confirmation_code);
    if (_matching_id != NULL)
        (*env)->ReleaseStringUTFChars(env, matching_id, _matching_id);
    (*env)->ReleaseStringUTFChars(env, smdp, _smdp);
    if (_imei != NULL)
        (*env)->ReleaseStringUTFChars(env, imei, _imei);
    return ret;
}


JNIEXPORT void JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_cancelSessions(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    es9p_cancel_session(ctx);
    es10b_cancel_session(ctx, ES10B_CANCEL_SESSION_REASON_UNDEFINED);
    euicc_http_cleanup(ctx);
}

#define QUOTE(S) #S
#define ERRCODE_ENUM_TO_STRING(VARIANT) case VARIANT: return toJString(env, QUOTE(VARIANT))

JNIEXPORT jstring JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_downloadErrCodeToString(JNIEnv *env, jobject thiz, jint code) {
    switch (code) {
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_INCORRECT_INPUT_VALUES);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_INVALID_SIGNATURE);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_INVALID_TRANSACTION_ID);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_UNSUPPORTED_CRT_VALUES);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_UNSUPPORTED_REMOTE_OPERATION_TYPE);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_UNSUPPORTED_PROFILE_CLASS);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_SCP03T_STRUCTURE_ERROR);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_SCP03T_SECURITY_ERROR);
        ERRCODE_ENUM_TO_STRING(
                ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_ICCID_ALREADY_EXISTS_ON_EUICC);
        ERRCODE_ENUM_TO_STRING(
                ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_INSUFFICIENT_MEMORY_FOR_PROFILE);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_INTERRUPTION);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_PE_PROCESSING_ERROR);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_ICCID_MISMATCH);
        ERRCODE_ENUM_TO_STRING(
                ES10B_ERROR_REASON_TEST_PROFILE_INSTALL_FAILED_DUE_TO_INVALID_NAA_KEY);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_PPR_NOT_ALLOWED);
        ERRCODE_ENUM_TO_STRING(ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_UNKNOWN_ERROR);
        default:
            return toJString(env, "ES10B_ERROR_REASON_UNDEFINED");
    }
}