#include "lpac-notifications.h"
#include <euicc/es9p.h>
#include <euicc/es10b.h>
#include <malloc.h>
#include <syslog.h>

JNIEXPORT jlong JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10bListNotification(JNIEnv *env, jobject thiz, jlong handle) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    struct es10b_notification_metadata_list *info = NULL;

    if (es10b_list_notification(ctx, &info) < 0)
        return 0;

    return (jlong) info;
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_handleNotification(JNIEnv *env, jobject thiz, jlong handle,
                                                       jlong seq_number) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    struct es10b_pending_notification notification;
    int res;

    res = es10b_retrieve_notifications_list(ctx, &notification, (unsigned long) seq_number);
    syslog(LOG_DEBUG, "es10b_retrieve_notification = %d %s", res, notification.b64_PendingNotification);
    if (res < 0)
        goto out;

    ctx->http.server_address = notification.notificationAddress;

    res = es9p_handle_notification(ctx, notification.b64_PendingNotification);
    syslog(LOG_DEBUG, "es9p_handle_notification = %d", res);
    if (res < 0)
        goto out;

    out:
    euicc_http_cleanup(ctx);
    return res;
}

JNIEXPORT jint JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_es10bDeleteNotification(JNIEnv *env, jobject thiz, jlong handle,
                                                            jlong seq_number) {
    struct euicc_ctx *ctx = (struct euicc_ctx *) handle;
    return es10b_remove_notification_from_list(ctx, (unsigned long) seq_number);
}

JNIEXPORT jstring JNICALL
Java_net_typeblog_lpac_1jni_LpacJni_notificationGetOperationString(JNIEnv *env, jobject thiz,
                                                                   jlong curr) {
    struct es10b_notification_metadata_list *info = (struct es10b_notification_metadata_list *) curr;
    const char *profileManagementOperationStr = NULL;
    switch (info->profileManagementOperation) {
        case ES10B_PROFILE_MANAGEMENT_OPERATION_INSTALL:
            profileManagementOperationStr = "install";
            break;
        case ES10B_PROFILE_MANAGEMENT_OPERATION_DELETE:
            profileManagementOperationStr = "delete";
            break;
        case ES10B_PROFILE_MANAGEMENT_OPERATION_ENABLE:
            profileManagementOperationStr = "enable";
            break;
        case ES10B_PROFILE_MANAGEMENT_OPERATION_DISABLE:
            profileManagementOperationStr = "disable";
            break;
        default:
            profileManagementOperationStr = "unknown";
            break;
    }

    return toJString(env, profileManagementOperationStr);
}

LPAC_JNI_STRUCT_GETTER_LINKED_LIST_NEXT(struct es10b_notification_metadata_list, notifications)
LPAC_JNI_STRUCT_FREE(struct es10b_notification_metadata_list, notifications, es10b_notification_metadata_list_free_all)
LPAC_JNI_STRUCT_GETTER_LONG(struct es10b_notification_metadata_list, notification, seqNumber, Seq)
LPAC_JNI_STRUCT_GETTER_STRING(struct es10b_notification_metadata_list, notification, notificationAddress, Address)
LPAC_JNI_STRUCT_GETTER_STRING(struct es10b_notification_metadata_list, notification, iccid, Iccid)
