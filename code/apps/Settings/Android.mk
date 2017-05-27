LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := bouncycastle conscrypt telephony-common ims-common
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 android-support-v13 jsr305 statistic_jar

LOCAL_MODULE_TAGS := optional

src_dirs := src
res_dirs := res

ifneq ($(strip $(wildcard $(LOCAL_PATH)/../AudioProfile)),)
audioprofile_dir := ../AudioProfile

src_dirs += \
    $(audioprofile_dir)/src \

res_dirs += \
    $(audioprofile_dir)/res \

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.sprd.audioprofile \

endif

LOCAL_SRC_FILES := \
        $(call all-java-files-under, $(src_dirs)) \
        src/com/android/settings/EventLogTags.logtags

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_PACKAGE_NAME := Settings
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

# Force building PowerGuru UI
# For normalize, put HeartbeatSyncSettings into PRODUCT_PACKAGES board config
# if current product needed
LOCAL_REQUIRED_MODULES += HeartbeatSyncSettings


LOCAL_PROGUARD_FLAG_FILES := proguard.flags

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
endif

include frameworks/opt/setupwizard/navigationbar/common.mk
include frameworks/opt/setupwizard/library/common.mk
include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)

####
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := statistic_jar:libs/statistic_settings.jar
include $(BUILD_MULTI_PREBUILT)
###

# Use the following include to make our test apk.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
