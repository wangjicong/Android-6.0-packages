#****************************************************
ifeq ($(strip $(REDSTONE_FOTA_SUPPORT)), yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := Rsota
LOCAL_MODULE_TAGS := optional
ifeq ($(strip $(REDSTONE_FOTA_APK_ICON)), yes)
LOCAL_SRC_FILES := aotaui_v5.2_20170428.apk
else

LOCAL_SRC_FILES := aotaui_v5.2_20170428_noicon.apk

endif
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGES_SUFFIX)
LOCAL_CERTIFICATE:=platform
#LOCAL_MODULE_PATH:=$(LOCAL_PATH)
include $(BUILD_PREBUILT)
endif
#****************************************************

