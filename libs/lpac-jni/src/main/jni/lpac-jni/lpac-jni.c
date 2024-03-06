#include <euicc/euicc.h>
#include <euicc/es10c.h>
#include <euicc/es10c_ex.h>
#include <euicc/interface.h>
#include <malloc.h>
#include <string.h>
#include <syslog.h>
#include "lpac-jni.h"
#include "lpac-download.h"
#include "lpac-notifications.h"
#include "interface-wrapper.h"

JavaVM *jvm = NULL;

jclass local_profile_info_class;
jmethodID local_profile_info_constructor;

jclass local_profile_state_class;
jmethodID local_profile_state_from_string;

jclass local_profile_class_class;
jmethodID local_profile_class_from_string;

jstring empty_string;

jclass string_class;
jmethodID string_constructor;

jclass euicc_info2_class;
jmethodID euicc_info2_constructor;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;
    interface_wrapper_init();
    lpac_download_init();
    lpac_notifications_init();

    LPAC_JNI_SETUP_ENV;
    string_class = (*env)->FindClass(env, "java/lang/String");
    string_class = (*env)->NewGlobalRef(env, string_class);
    string_constructor = (*env)->GetMethodID(env, string_class, "<init>",
                                             "([BLjava/lang/String;)V");

    local_profile_info_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/LocalProfileInfo");
    local_profile_info_class = (*env)->NewGlobalRef(env, local_profile_info_class);
    local_profile_info_constructor = (*env)->GetMethodID(env, local_profile_info_class, "<init>",
                                                         "(Ljava/lang/String;Lnet/typeblog/lpac_jni/LocalProfileInfo$State;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/typeblog/lpac_jni/LocalProfileInfo$Clazz;)V");

    local_profile_state_class = (*env)->FindClass(env,
                                                  "net/typeblog/lpac_jni/LocalProfileInfo$State");
    local_profile_state_class = (*env)->NewGlobalRef(env, local_profile_state_class);
    local_profile_state_from_string = (*env)->GetStaticMethodID(env, local_profile_state_class,
                                                                "fromString",
                                                                "(Ljava/lang/String;)Lnet/typeblog/lpac_jni/LocalProfileInfo$State;");

    local_profile_class_class = (*env)->FindClass(env,
                                                  "net/typeblog/lpac_jni/LocalProfileInfo$Clazz");
    local_profile_class_class = (*env)->NewGlobalRef(env, local_profile_class_class);
    local_profile_class_from_string = (*env)->GetStaticMethodID(env, local_profile_class_class,
                                                                "fromString",
                                                                "(Ljava/lang/String;)Lnet/typeblog/lpac_jni/LocalProfileInfo$Clazz;");

    euicc_info2_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/EuiccInfo2");
    euicc_info2_class = (*env)->NewGlobalRef(env, euicc_info2_class);
    euicc_info2_constructor = (*env)->GetMethodID(env, euicc_info2_class, "<init>",
                                                  "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II[Ljava/lang/String;[Ljava/lang/String;)V");

    const char _unused[1];
    empty_string = (*env)->NewString(env, _unused, 0);
    empty_string = (*env)->NewGlobalRef(env, empty_string);

    return JNI_VERSION_1_6;
}

JNIEXPORT jlong JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_createContext(JNIEnv *env, jobject thiz,
                                                  jobject apdu_interface,
                                                  jobject http_interface) {
    struct lpac_jni_ctx *jni_ctx = NULL;
    struct euicc_ctx *ctx = NULL;

    ctx = calloc(1, sizeof(struct euicc_ctx));
    jni_ctx = calloc(1, sizeof(struct lpac_jni_ctx));
    ctx->apdu.interface = &lpac_jni_apdu_interface;
    ctx->http.interface = &lpac_jni_http_interface;
    jni_ctx->apdu_interface = (*env)->NewGlobalRef(env, apdu_interface);
    jni_ctx->http_interface = (*env)->NewGlobalRef(env, http_interface);
    ctx->userdata = (void *) jni_ctx;
    return (jlong) ctx;
}

JNIEXPORT void JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_destroyContext(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    struct lpac_jni_ctx *jni_ctx = LPAC_JNI_CTX(ctx);

    (*env)->DeleteGlobalRef(env, jni_ctx->apdu_interface);
    (*env)->DeleteGlobalRef(env, jni_ctx->http_interface);
    free(jni_ctx);
    free(ctx);
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_euiccInit(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    return euicc_init(ctx);
}

JNIEXPORT void JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_euiccFini(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    euicc_fini(ctx);
}

jstring toJString(JNIEnv *env, const char *pat) {
    jbyteArray bytes = NULL;
    jstring encoding = NULL;
    jstring jstr = NULL;
    int len;

    if (pat == NULL)
        return (*env)->NewLocalRef(env, empty_string);

    len = strlen(pat);
    bytes = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte *) pat);
    encoding = (*env)->NewStringUTF(env, "utf-8");
    jstr = (jstring) (*env)->NewObject(env, string_class,
                                       string_constructor, bytes, encoding);
    (*env)->DeleteLocalRef(env, encoding);
    (*env)->DeleteLocalRef(env, bytes);
    return jstr;
}

JNIEXPORT jstring JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cGetEid(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    char *buf = NULL;

    if (es10c_get_eid(ctx, &buf) < 0) {
        return NULL;
    }
    jstring ret = toJString(env, buf);
    free(buf);
    return ret;
}

jobject profile_info_native_to_java(JNIEnv *env, struct es10c_profile_info_list *info) {
    const char *profileStateStr = NULL;
    const char *profileClassStr = NULL;
    jstring serviceProvider = NULL;
    jstring nickName = NULL;
    jstring isdpAid = NULL;
    jstring iccid = NULL;
    jstring name = NULL;
    jobject state = NULL;
    jobject class = NULL;
    jobject jinfo = NULL;

    iccid = toJString(env, info->iccid);
    isdpAid = toJString(env, info->isdpAid);
    name = toJString(env, info->profileName);
    nickName = toJString(env, info->profileNickname);
    serviceProvider = toJString(env, info->serviceProviderName);

    // TODO: Maybe we should pass a Java object directly here?
    switch (info->profileState) {
        case ES10C_PROFILE_STATE_ENABLED:
            profileStateStr = "enabled";
            break;
        case ES10C_PROFILE_STATE_DISABLED:
            profileStateStr = "disabled";
            break;
        default:
            profileStateStr = "unknown";
    }

    state = (*env)->CallStaticObjectMethod(env, local_profile_state_class,
                                           local_profile_state_from_string,
                                           toJString(env, profileStateStr));

    switch (info->profileClass) {
        case ES10C_PROFILE_CLASS_TEST:
            profileClassStr = "test";
            break;
        case ES10C_PROFILE_CLASS_PROVISIONING:
            profileClassStr = "provisioning";
            break;
        case ES10C_PROFILE_CLASS_OPERATIONAL:
            profileClassStr = "operational";
            break;
        default:
            profileClassStr = "unknown";
            break;
    }

    class = (*env)->CallStaticObjectMethod(env, local_profile_class_class,
                                           local_profile_class_from_string,
                                           toJString(env, profileClassStr));

    jinfo = (*env)->NewObject(env, local_profile_info_class, local_profile_info_constructor,
                              iccid, state, name, nickName, serviceProvider, isdpAid, class);

    (*env)->DeleteLocalRef(env, class);
    (*env)->DeleteLocalRef(env, state);
    (*env)->DeleteLocalRef(env, serviceProvider);
    (*env)->DeleteLocalRef(env, nickName);
    (*env)->DeleteLocalRef(env, name);
    (*env)->DeleteLocalRef(env, isdpAid);
    (*env)->DeleteLocalRef(env, iccid);

    return jinfo;
}

JNIEXPORT jobjectArray JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cGetProfilesInfo(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    struct es10c_profile_info_list *info = NULL;
    struct es10c_profile_info_list *curr = NULL;
    jobjectArray ret = NULL;
    jobject jinfo = NULL;
    int count = 0;

    if (es10c_get_profiles_info(ctx, &info) < 0) {
        return NULL;
    }

    count = LPAC_JNI_LINKED_LIST_COUNT(info, curr);

    ret = (*env)->NewObjectArray(env, count, local_profile_info_class, NULL);

    // Convert the native info array to Java
    LPAC_JNI_LINKED_LIST_FOREACH(info, curr, {
        jinfo = profile_info_native_to_java(env, curr);
        (*env)->SetObjectArrayElement(env, ret, i, jinfo);
        (*env)->DeleteLocalRef(env, jinfo);
    });

    es10c_profile_info_list_free_all(info);
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cEnableProfile(JNIEnv *env, jobject thiz, jlong handle,
                                                       jstring iccid) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    const char *_iccid = NULL;
    int ret;

    _iccid = (*env)->GetStringUTFChars(env, iccid, NULL);
    ret = es10c_enable_profile(ctx, _iccid, 1);
    (*env)->ReleaseStringUTFChars(env, iccid, _iccid);
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cDisableProfile(JNIEnv *env, jobject thiz, jlong handle,
                                                        jstring iccid) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    const char *_iccid = NULL;
    int ret;

    _iccid = (*env)->GetStringUTFChars(env, iccid, NULL);
    ret = es10c_disable_profile(ctx, _iccid, 1);
    (*env)->ReleaseStringUTFChars(env, iccid, _iccid);
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cSetNickname(JNIEnv *env, jobject thiz, jlong handle,
                                                     jstring iccid, jstring nick) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    const char *_iccid = NULL;
    const char *_nick = NULL;
    int ret;

    _iccid = (*env)->GetStringUTFChars(env, iccid, NULL);
    _nick = (*env)->GetStringUTFChars(env, nick, NULL);
    ret = es10c_set_nickname(ctx, _iccid, _nick);
    (*env)->ReleaseStringUTFChars(env, nick, _nick);
    (*env)->ReleaseStringUTFChars(env, iccid, _iccid);
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cDeleteProfile(JNIEnv *env, jobject thiz, jlong handle,
                                                       jstring iccid) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    const char *_iccid = NULL;
    int ret;

    _iccid = (*env)->GetStringUTFChars(env, iccid, NULL);
    ret = es10c_delete_profile(ctx, _iccid);
    (*env)->ReleaseStringUTFChars(env, iccid, _iccid);
    return ret;
}

JNIEXPORT jobject JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cexGetEuiccInfo2(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    struct es10c_ex_euiccinfo2 info = {0};
    jobjectArray euiccCiPKIdListForVerification = NULL;
    jobjectArray euiccCiPKIdListForSigning = NULL;
    jstring sas_accreditation_number = NULL;
    jstring global_platform_version = NULL;
    jstring euicc_firmware_version = NULL;
    jstring profile_version = NULL;
    jstring pp_version = NULL;
    jobject ret = NULL;
    char **curr = NULL;
    int count = 0;

    if (es10c_ex_get_euiccinfo2(ctx, &info) < 0)
        goto out;

    profile_version = toJString(env, info.profileVersion);
    euicc_firmware_version = toJString(env, info.euiccFirmwareVer);
    global_platform_version = toJString(env, info.globalplatformVersion);
    sas_accreditation_number = toJString(env, info.sasAcreditationNumber);
    pp_version = toJString(env, info.ppVersion);

    count = LPAC_JNI_NULL_TERM_LIST_COUNT(info.euiccCiPKIdListForSigning, curr);
    euiccCiPKIdListForSigning = (*env)->NewObjectArray(env, count, string_class, NULL);
    LPAC_JNI_NULL_TERM_LIST_FOREACH(info.euiccCiPKIdListForSigning, curr, {
        (*env)->SetObjectArrayElement(env, euiccCiPKIdListForSigning, i, toJString(env, *curr));
    });

    count = LPAC_JNI_NULL_TERM_LIST_COUNT(info.euiccCiPKIdListForVerification, curr);
    euiccCiPKIdListForVerification = (*env)->NewObjectArray(env, count, string_class, NULL);
    LPAC_JNI_NULL_TERM_LIST_FOREACH(info.euiccCiPKIdListForVerification, curr, {
        (*env)->SetObjectArrayElement(env, euiccCiPKIdListForVerification, i,
                                      toJString(env, *curr));
    });

    ret = (*env)->NewObject(env, euicc_info2_class, euicc_info2_constructor,
                            profile_version, euicc_firmware_version,
                            global_platform_version,
                            sas_accreditation_number, pp_version,
                            info.extCardResource.freeNonVolatileMemory,
                            info.extCardResource.freeVolatileMemory,
                            euiccCiPKIdListForSigning,
                            euiccCiPKIdListForVerification);

    out:
    (*env)->DeleteLocalRef(env, profile_version);
    (*env)->DeleteLocalRef(env, euicc_firmware_version);
    (*env)->DeleteLocalRef(env, global_platform_version);
    (*env)->DeleteLocalRef(env, sas_accreditation_number);
    (*env)->DeleteLocalRef(env, pp_version);
    es10c_ex_euiccinfo2_free(&info);
    return ret;
}