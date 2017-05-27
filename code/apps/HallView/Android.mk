LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-common \
                               android-support-v4 \
                               android-support-v7-palette

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
        src/com/android/music/IMediaPlaybackService.aidl \
        src/com/android/incallui/IHallCallBackService.aidl



LOCAL_PACKAGE_NAME := HallView
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
