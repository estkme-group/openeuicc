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

    LPAC_JNI_SETUP_ENV;
    string_class = (*env)->FindClass(env, "java/lang/String");
    string_class = (*env)->NewGlobalRef(env, string_class);
    string_constructor = (*env)->GetMethodID(env, string_class, "<init>", "([BLjava/lang/String;)V");

    local_profile_info_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/LocalProfileInfo");
    local_profile_info_class = (*env)->NewGlobalRef(env, local_profile_info_class);
    local_profile_info_constructor = (*env)->GetMethodID(env, local_profile_info_class, "<init>",
                                                         "(Ljava/lang/String;Lnet/typeblog/lpac_jni/LocalProfileInfo$State;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/typeblog/lpac_jni/LocalProfileInfo$Clazz;)V");

    local_profile_state_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/LocalProfileInfo$State");
    local_profile_state_class = (*env)->NewGlobalRef(env, local_profile_state_class);
    local_profile_state_from_string = (*env)->GetStaticMethodID(env, local_profile_state_class, "fromString", "(Ljava/lang/String;)Lnet/typeblog/lpac_jni/LocalProfileInfo$State;");

    local_profile_class_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/LocalProfileInfo$Clazz");
    local_profile_class_class = (*env)->NewGlobalRef(env, local_profile_class_class);
    local_profile_class_from_string = (*env)->GetStaticMethodID(env, local_profile_class_class, "fromString", "(Ljava/lang/String;)Lnet/typeblog/lpac_jni/LocalProfileInfo$Clazz;");

    euicc_info2_class = (*env)->FindClass(env, "net/typeblog/lpac_jni/EuiccInfo2");
    euicc_info2_class = (*env)->NewGlobalRef(env, euicc_info2_class);
    euicc_info2_constructor = (*env)->GetMethodID(env, euicc_info2_class, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V");

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

    ctx = malloc(sizeof(struct euicc_ctx));
    jni_ctx = malloc(sizeof(struct lpac_jni_ctx));
    memset(ctx, 0, sizeof(struct lpac_jni_ctx));
    memset(jni_ctx, 0, sizeof(struct lpac_jni_ctx));
    ctx->interface.apdu = &lpac_jni_apdu_interface;
    ctx->interface.http = &lpac_jni_http_interface;
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
    jbyteArray bytes = NULL;
    jstring encoding = NULL;
    jstring jstr = NULL;
    int len;

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

jobject profile_info_native_to_java(JNIEnv *env, struct es10c_profile_info *info) {
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
    nickName = info->profileNickname ? toJString(env, info->profileNickname) : (*env)->NewLocalRef(env, empty_string);
    serviceProvider = info->serviceProviderName ? toJString(env, info->serviceProviderName) : (*env)->NewLocalRef(env, empty_string);

    state = (*env)->CallStaticObjectMethod(env, local_profile_state_class,
                                           local_profile_state_from_string,
                                           toJString(env, info->profileState));

    class = (*env)->CallStaticObjectMethod(env, local_profile_class_class,
                                           local_profile_class_from_string,
                                           toJString(env, info->profileClass));

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
    struct es10c_profile_info *info = NULL;
    jobjectArray ret = NULL;
    jobject jinfo = NULL;
    int count;

    if (es10c_get_profiles_info(ctx, &info, &count) < 0) {
        return NULL;
    }

    ret = (*env)->NewObjectArray(env, count, local_profile_info_class, NULL);

    // Convert the native info array to Java
    for (int i = 0; i < count; i++) {
        jinfo = profile_info_native_to_java(env, &info[i]);
        (*env)->SetObjectArrayElement(env, ret, i, jinfo);
        (*env)->DeleteLocalRef(env, jinfo);
    }

    es10c_profile_info_free_all(info, count);
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10cEnableProfile(JNIEnv *env, jobject thiz, jlong handle,
                                                       jstring iccid) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    const char *_iccid = NULL;
    int ret;

    _iccid = (*env)->GetStringUTFChars(env, iccid, NULL);
    ret = es10c_enable_profile_iccid(ctx, _iccid, 1);
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
    ret = es10c_disable_profile_iccid(ctx, _iccid, 1);
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
    ret = es10c_delete_profile_iccid(ctx, _iccid);
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
                            info.free_nvram, info.free_ram);

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