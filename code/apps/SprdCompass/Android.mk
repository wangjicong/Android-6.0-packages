LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

# TODO Avoid build error on build server, but if we want to make our apk, open this
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES :=

LOCAL_PACKAGE_NAME := SprdCompass
# LOCAL_CERTIFICATE := platform
LOCAL_MODULE_PATH := $(TARGET_OUT)/app

#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

# /sprdroid6.0_trunk/vendor/sprd/open-source/common_packages.mk

