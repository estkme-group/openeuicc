#pragma once
#include <euicc/euicc.h>
#include <pthread.h>
#include <jni.h>

struct lpac_jni_ctx {
    struct euicc_ctx ctx;
    jobject apdu_interface;
    jobject http_interface;
};

extern JavaVM *jvm;
extern pthread_mutex_t global_lock;
extern struct lpac_jni_ctx *jni_ctx;

#define LPAC_JNI_BEGIN pthread_mutex_lock(&global_lock)
#define LPAC_JNI_END0 pthread_mutex_unlock(&global_lock)
#define LPAC_JNI_END(ret) LPAC_JNI_END0; return ret
