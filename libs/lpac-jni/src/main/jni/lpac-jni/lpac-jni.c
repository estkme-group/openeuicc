#include <euicc/es10x.h>
#include <euicc/interface.h>
#include <malloc.h>
#include <string.h>
#include <syslog.h>
#include "lpac-jni.h"
#include "lpac-download.h"
#include "interface-wrapper.h"

JavaVM  *jvm = NULL;

jclass local_profile_info_class;
jmethodID local_profile_info_constructor;

jobject local_profile_state_enabled;
jobject local_profile_state_disabled;

jobject local_profile_class_testing;
jobject local_profile_class_provisioning;
jobject local_profile_class_operational;

jstring empty_string;

jclass string_class;
jmethodID string_constructor;

jclass euicc_info2_class;
jmethodID euicc_info2_constructor;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;
    interface_wrapper_init();
    lpac_download_init();

    LPAC_JNI_SETUP_ENV;
    string_class = (*env)->FindClass(env, "java/lang/String");
    string_class = (*env)->NewGlobalRef(env, string_class);
    string_constructor = (*env)->GetMethodID(env, string_class, "<init>", "([BLjava/lang/String;)V");

    local_profile_info_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/LocalProfileInfo");
    local_profile_info_class = (*env)->NewGlobalRef(env, local_profile_info_class);
    local_profile_info_constructor = (*env)->GetMethodID(env, local_profile_info_class, "<init>",
                                                         "(Ljava/lang/String;Lnet/typeblog/lpac_jni/LocalProfileInfo$State;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/typeblog/lpac_jni/LocalProfileInfo$Clazz;)V");

    jclass local_profile_state_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/LocalProfileInfo$State");
    jfieldID field_enabled = (*env)->GetStaticFieldID(env, local_profile_state_class, "Enabled", "Lnet/typeblog/lpac_jni/LocalProfileInfo$State;");
    local_profile_state_enabled = (*env)->GetStaticObjectField(env, local_profile_state_class, field_enabled);
    local_profile_state_enabled = (*env)->NewGlobalRef(env, local_profile_state_enabled);
    jfieldID field_disabled = (*env)->GetStaticFieldID(env, local_profile_state_class, "Disabled", "Lnet/typeblog/lpac_jni/LocalProfileInfo$State;");
    local_profile_state_disabled = (*env)->GetStaticObjectField(env, local_profile_state_class, field_disabled);
    local_profile_state_disabled = (*env)->NewGlobalRef(env, local_profile_state_disabled);

    jclass local_profile_class_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/LocalProfileInfo$Clazz");
    jfieldID field_testing = (*env)->GetStaticFieldID(env, local_profile_class_class, "Testing", "Lnet/typeblog/lpac_jni/LocalProfileInfo$Clazz;");
    local_profile_class_testing = (*env)->GetStaticObjectField(env, local_profile_class_class, field_testing);
    local_profile_class_testing = (*env)->NewGlobalRef(env, local_profile_class_testing);
    jfieldID field_provisioning = (*env)->GetStaticFieldID(env, local_profile_class_class, "Provisioning", "Lnet/typeblog/lpac_jni/LocalProfileInfo$Clazz;");
    local_profile_class_provisioning = (*env)->GetStaticObjectField(env, local_profile_class_class, field_provisioning);
    local_profile_class_provisioning = (*env)->NewGlobalRef(env, local_profile_class_provisioning);
    jfieldID field_operational = (*env)->GetStaticFieldID(env, local_profile_class_class, "Operational", "Lnet/typeblog/lpac_jni/LocalProfileInfo$Clazz;");
    local_profile_class_operational = (*env)->GetStaticObjectField(env, local_profile_class_class, field_operational);
    local_profile_class_operational = (*env)->NewGlobalRef(env, local_profile_class_operational);

    euicc_info2_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/EuiccInfo2");
    euicc_info2_class = (*env)->NewGlobalRef(env, euicc_info2_class);
    euicc_info2_constructor = (*env)->GetMethodID(env, euicc_info2_class, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;III)V");

    const char _unused[1];
    empty_string = (*env)->NewString(env, _unused, 0);
    empty_string = (*env)->NewGlobalRef(env, empty_string);

    return JNI_VERSION_1_6;
}

JNIEXPORT jlong JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_createContext(JNIEnv *env, jobject thiz,
                                                  jobject apdu_interface,
                                                  jobject http_interface) {
    struct euicc_ctx *ctx = malloc(sizeof(struct euicc_ctx));
    struct lpac_jni_ctx *_ctx = malloc(sizeof(struct lpac_jni_ctx));
    memset(ctx, 0, sizeof(struct lpac_jni_ctx));
    memset(_ctx, 0, sizeof(struct lpac_jni_ctx));
    ctx->interface.apdu = &lpac_jni_apdu_interface;
    ctx->interface.http = &lpac_jni_http_interface;
    _ctx->apdu_interface = (*env)->NewGlobalRef(env, apdu_interface);
    _ctx->http_interface = (*env)->NewGlobalRef(env, http_interface);
    ctx->userdata = (void *) _ctx;
    return (jlong) ctx;
}

JNIEXPORT void JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_destroyContext(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    struct lpac_jni_ctx *_ctx = LPAC_JNI_CTX(ctx);
    (*env)->DeleteGlobalRef(env, _ctx->apdu_interface);
    (*env)->DeleteGlobalRef(env, _ctx->http_interface);
    free(_ctx);
    free(ctx);
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10xInit(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    return es10x_init(ctx);
}

JNIEXPORT void JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10xFini(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    es10x_fini(ctx);
}

jstring toJString(JNIEnv *env, const char *pat) {
    int len = strlen(pat);
    jbyteArray bytes = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte *) pat);
    jstring encoding = (*env)->NewStringUTF(env, "utf-8");
    jstring jstr = (jstring) (*env)->NewObject(env, string_class,
                                               string_constructor, bytes, encoding);
    (*env)->DeleteLocalRef(env, encoding);
    (*env)->DeleteLocalRef(env, bytes);
    return jstr;
}

JNIEXPORT jstring JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cGetEid(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    char *buf;
    if (es10c_get_eid(ctx, &buf) < 0) {
        return NULL;
    }
    jstring ret = toJString(env, buf);
    free(buf);
    return ret;
}

JNIEXPORT jobjectArray JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cGetProfilesInfo(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    struct es10c_profile_info *info;
    int count;
    if (es10c_get_profiles_info(ctx, &info, &count) < 0) {
        return NULL;
    }

    jobjectArray ret = (*env)->NewObjectArray(env, count, local_profile_info_class, NULL);

    // Convert the native info array to Java
    for (int i = 0; i < count; i++) {
        jstring iccid = toJString(env, info[i].iccid);
        jstring isdpAid = toJString(env, info[i].isdpAid);
        jstring name = toJString(env, info[i].profileName);
        jstring nickName = info[i].profileNickname ? toJString(env, info[i].profileNickname) : (*env)->NewLocalRef(env, empty_string);
        jstring serviceProvider = info[i].serviceProviderName ? toJString(env, info[i].serviceProviderName) : (*env)->NewLocalRef(env, empty_string);

        jobject state;
        switch (info[i].profileState) {
            case ES10C_PROFILE_INFO_STATE_ENABLED:
                state = local_profile_state_enabled;
                break;
            case ES10C_PROFILE_INFO_STATE_DISABLED:
                state = local_profile_state_disabled;
                break;
        }
        state = (*env)->NewLocalRef(env, state);

        jobject class;
        switch (info[i].profileClass) {
            case ES10C_PROFILE_INFO_CLASS_TEST:
                class = local_profile_class_testing;
                break;
            case ES10C_PROFILE_INFO_CLASS_PROVISIONING:
                class = local_profile_class_provisioning;
                break;
            case ES10C_PROFILE_INFO_CLASS_OPERATIONAL:
                class = local_profile_class_operational;
                break;
        }
        class = (*env)->NewLocalRef(env, class);

        jobject jinfo = (*env)->NewObject(env, local_profile_info_class, local_profile_info_constructor,
                                          iccid, state, name, nickName, serviceProvider, isdpAid, class);
        (*env)->SetObjectArrayElement(env, ret, i, jinfo);

        (*env)->DeleteLocalRef(env, jinfo);
        (*env)->DeleteLocalRef(env, class);
        (*env)->DeleteLocalRef(env, state);
        (*env)->DeleteLocalRef(env, serviceProvider);
        (*env)->DeleteLocalRef(env, nickName);
        (*env)->DeleteLocalRef(env, name);
        (*env)->DeleteLocalRef(env, isdpAid);
        (*env)->DeleteLocalRef(env, iccid);
    }

    es10c_profile_info_free_all(info, count);
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cEnableProfile(JNIEnv *env, jobject thiz, jlong handle,
                                                       jstring iccid) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    const char *_iccid = (*env)->GetStringUTFChars(env, iccid, NULL);
    int ret = es10c_enable_profile_iccid(ctx, _iccid, 1);
    (*env)->ReleaseStringUTFChars(env, iccid, _iccid);
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cDisableProfile(JNIEnv *env, jobject thiz, jlong handle,
                                                        jstring iccid) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    const char *_iccid = (*env)->GetStringUTFChars(env, iccid, NULL);
    int ret = es10c_disable_profile_iccid(ctx, _iccid, 1);
    (*env)->ReleaseStringUTFChars(env, iccid, _iccid);
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cSetNickname(JNIEnv *env, jobject thiz, jlong handle,
                                                     jstring iccid, jstring nick) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    const char *_iccid = (*env)->GetStringUTFChars(env, iccid, NULL);
    const char *_nick = (*env)->GetStringUTFChars(env, nick, NULL);
    int ret = es10c_set_nickname(ctx, _iccid, _nick);
    (*env)->ReleaseStringUTFChars(env, nick, _nick);
    (*env)->ReleaseStringUTFChars(env, iccid, _iccid);
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cDeleteProfile(JNIEnv *env, jobject thiz, jlong handle,
                                                       jstring iccid) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    const char *_iccid = (*env)->GetStringUTFChars(env, iccid, NULL);
    int ret = es10c_delete_profile_iccid(ctx, _iccid);
    (*env)->ReleaseStringUTFChars(env, iccid, _iccid);
    return ret;
}

JNIEXPORT jobject JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cexGetEuiccInfo2(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    struct es10cex_euiccinfo2 info;
    jstring sas_accreditation_number = NULL;
    jstring global_platform_version = NULL;
    jstring euicc_firmware_version = NULL;
    jstring uicc_firmware_version = NULL;
    jstring profile_version = NULL;
    jstring sgp22_version = NULL;
    jstring pp_version = NULL;
    jobject ret = NULL;

    if (es10cex_get_euiccinfo2(ctx, &info) < 0)
        goto out;

    profile_version = toJString(env, info.profile_version);
    sgp22_version = toJString(env, info.sgp22_version);
    euicc_firmware_version = toJString(env, info.euicc_firmware_version);
    uicc_firmware_version = toJString(env, info.uicc_firmware_version);
    global_platform_version = toJString(env, info.global_platform_version);
    sas_accreditation_number = toJString(env, info.sas_accreditation_number);
    pp_version = toJString(env, info.pp_version);

    ret = (*env)->NewObject(env, euicc_info2_class, euicc_info2_constructor,
                            profile_version, sgp22_version, euicc_firmware_version,
                            uicc_firmware_version, global_platform_version,
                            sas_accreditation_number, pp_version,
                            info.installed_app, info.free_nvram, info.free_ram);

    out:
    (*env)->DeleteLocalRef(env, profile_version);
    (*env)->DeleteLocalRef(env, sgp22_version);
    (*env)->DeleteLocalRef(env, euicc_firmware_version);
    (*env)->DeleteLocalRef(env, uicc_firmware_version);
    (*env)->DeleteLocalRef(env, global_platform_version);
    (*env)->DeleteLocalRef(env, sas_accreditation_number);
    (*env)->DeleteLocalRef(env, pp_version);
    return ret;
}