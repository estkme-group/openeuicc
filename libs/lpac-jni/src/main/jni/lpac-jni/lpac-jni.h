#pragma once

#include <euicc/euicc.h>
#include <pthread.h>
#include <jni.h>

_Static_assert(sizeof(void *) <= sizeof(jlong),
               "jlong must be big enough to hold a platform raw pointer");

struct lpac_jni_ctx {
    jobject apdu_interface;
    jobject http_interface;
};

#define LPAC_JNI_CTX(ctx) ((struct lpac_jni_ctx *) ctx->userdata)
#define LPAC_JNI_SETUP_ENV \
    JNIEnv *env; \
    (*jvm)->AttachCurrentThread(jvm, &env, NULL)

extern JavaVM *jvm;
extern jclass string_class;

jstring toJString(JNIEnv *env, const char *pat);

#define LPAC_JNI_STRUCT_GETTER_LINKED_LIST_NEXT(st, st_jname) \
        JNIEXPORT jlong JNICALL Java_net_typeblog_lpac_1jni_LpacJni_##st_jname##Next(JNIEnv *env, jobject thiz, jlong raw) { \
            st *p = (st *) raw;                       \
            if (p == NULL) return 0;                  \
            return (jlong) p->next;                   \
        }

#define LPAC_JNI_STRUCT_GETTER_NULL_TERM_LIST_NEXT(st, st_jname) \
        JNIEXPORT jlong JNICALL Java_net_typeblog_lpac_1jni_LpacJni_##st_jname##Next(JNIEnv *env, jobject thiz, jlong raw) { \
            st *p = (st *) raw;                     \
            p++;                                      \
            if (*p == NULL) return 0;                 \
            return (jlong) p;                         \
        }

#define LPAC_JNI_STRUCT_FREE(st, st_jname, free_func) \
        JNIEXPORT void JNICALL Java_net_typeblog_lpac_1jni_LpacJni_##st_jname##Free(JNIEnv *env, jobject thiz, jlong raw) { \
            st *p = (st *) raw;                       \
            if (p == NULL) return;                    \
            free_func(p);                             \
        }

#define LPAC_JNI_STRUCT_GETTER_LONG(st, st_name, name, jname) \
        JNIEXPORT jlong JNICALL Java_net_typeblog_lpac_1jni_LpacJni_##st_name##Get##jname(JNIEnv *env, jobject thiz, jlong raw) { \
            st *p = (st *) raw;                       \
            if (p == NULL) return 0;                  \
            return (jlong) p->name;                   \
        }

#define LPAC_JNI_STRUCT_GETTER_STRING(st, st_name, name, jname) \
        JNIEXPORT jstring JNICALL Java_net_typeblog_lpac_1jni_LpacJni_##st_name##Get##jname(JNIEnv *env, jobject thiz, jlong raw) { \
            st *p = (st *) raw;                       \
            return toJString(env, p->name);           \
        }