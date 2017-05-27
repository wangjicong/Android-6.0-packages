LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
MY_FILTER_SCENERY := false
MY_EDIT_PUZZLE:=true
MY_TIME_STAMP:=true
MY_GIF_CAM:=true

ifeq ($(strip $(TARGET_TS_UCAM_EDIT_PUZZLE_NENABLE)),false)
    MY_EDIT_PUZZLE := false
endif

ifeq ($(strip $(TARGET_TS_UCAM_MAKEUP_TIMESTAMP_NENABLE)),false)
    MY_TIME_STAMP:=false
endif

ifeq ($(strip $(TARGET_TS_UCAM_MAKEUP_GIF_NENABLE)),false)
    MY_GIF_CAM := false
endif

#SUN:jicong.wang add for panoram goriginal start 
ifeq ($(strip $(SUNVOV_PANORAMA_ORIGINAL)),true)
LOCAL_CFLAGS += -DSUNVOV_PANORAMA_ORIGINAL
$(shell cp $(LOCAL_PATH)/res/layout/sunvov_pano_module_capture.xml $(LOCAL_PATH)/res/layout/pano_module_capture.xml)
endif
#SUN:jicong.wang add for panoram goriginal end
 
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += android-ex-camera2-portability
LOCAL_STATIC_JAVA_LIBRARIES += xmp_toolkit
LOCAL_STATIC_JAVA_LIBRARIES += glide
LOCAL_STATIC_JAVA_LIBRARIES += guava
LOCAL_STATIC_JAVA_LIBRARIES += jsr305

ifeq ($(strip $(MY_EDIT_PUZZLE)),true)
LOCAL_STATIC_JAVA_LIBRARIES += libemail-uphoto
LOCAL_JAVA_LIBRARIES := org.apache.http.legacy
endif

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, src_pd)
LOCAL_SRC_FILES += $(call all-java-files-under, src_pd_gcam)
LOCAL_SRC_FILES += src/com/sprd/gallery3d/aidl/IFloatWindowController.aidl

LOCAL_SRC_FILES += $(call all-java-files-under, src_ucam/UCamera/src/com/ucamera/ucam/jni)

ifeq ($(MY_EDIT_PUZZLE),true)
LOCAL_SRC_FILES += $(call all-java-files-under, src_ucam/UCommon/puzzle)
LOCAL_SRC_FILES += $(call all-java-files-under, src_ucam/UCommon/downloadcenter)
LOCAL_SRC_FILES += $(call all-java-files-under, src_ucam/UGallery)
LOCAL_SRC_FILES += $(call all-java-files-under, src_ucam/UPhoto)
else
LOCAL_SRC_FILES += $(call all-java-files-under, src_fake/com/ucamera/ugallery)
endif

LOCAL_RESOURCE_DIR += \
    $(LOCAL_PATH)/res \
    $(LOCAL_PATH)/res_p \
    $(LOCAL_PATH)/res_ucam

ifeq ($(strip $(MY_EDIT_PUZZLE)),true)
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/src_ucam/UCommon/puzzle/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/src_ucam/UCommon/downloadcenter/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/src_ucam/UGallery/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/src_ucam/UPhoto/res
endif

ifeq ($(strip $(MY_GIF_CAM)),true)
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/src_ucam/UCamera/res_gif
endif

ifeq ($(strip $(MY_GIF_CAM)),true)
LOCAL_SRC_FILES += $(call all-java-files-under, src_ucam/UCamera/src/com/ucamera/ucam/modules)
LOCAL_SRC_FILES += $(call all-java-files-under, src_ucam/UCamera/src/com/ucamera/ucam/sound)
LOCAL_SRC_FILES += $(call all-java-files-under, src_ucam/UCamera/src/com/ucamera/ucam/thumbnail)
else
LOCAL_SRC_FILES += $(call all-java-files-under, src_fake/com/ucamera/ucam/modules)
endif

include $(LOCAL_PATH)/version.mk
LOCAL_AAPT_FLAGS := \
        --auto-add-overlay \
        --extra-packages com.ucamera.uphoto \
        --extra-packages com.ucamera.ugallery:com.ucamera.ucomm.puzzle:com.ucamera.ucomm.sns:com.ucamera.ucomm.downloadcenter \
        --version-name "$(version_name_package)" \
        --version-code $(version_code_package) \

LOCAL_PACKAGE_NAME := Camera2
LOCAL_CERTIFICATE := platform

#LOCAL_SDK_VERSION := current

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

ifeq ($(strip $(MY_EDIT_PUZZLE)),true)
LOCAL_PROGUARD_FLAGS += -include $(LOCAL_PATH)/src_ucam/UCamera/proguard.flags
LOCAL_PROGUARD_FLAGS += -include $(LOCAL_PATH)/src_ucam/UPhoto/proguard.flags
LOCAL_PROGUARD_FLAGS += -include $(LOCAL_PATH)/src_ucam/UGallery/proguard.flags
LOCAL_PROGUARD_FLAGS += -include $(LOCAL_PATH)/src_ucam/UCommon/puzzle/proguard.flags
endif

ifeq ($(strip $(MY_EDIT_PUZZLE)),true)
LOCAL_ASSET_DIR := $(call intermediates-dir-for, APPS, $(LOCAL_PACKAGE_NAME),,COMMON)/assets
ifeq ($(UPHOTO_ASSET_COMMON_DIR),)
UPHOTO_ASSET_COMMON_DIR := $(LOCAL_ASSET_DIR)
$(info Coping UPhoto common assets to: $(UPHOTO_ASSET_COMMON_DIR))
$(shell rm -rf $(UPHOTO_ASSET_COMMON_DIR))
define my-copy-assets
$(foreach f, $(call find-subdir-assets, $(1)), \
  $(shell mkdir -p $(dir $(2)/$(f)); $(LOCAL_PATH)/acp -fp $(1)/$(f) $(2)/$(f)) \
)
endef
$(shell rm -rf $(LOCAL_ASSET_DIR))
$(call my-copy-assets, $(LOCAL_PATH)/assets, $(LOCAL_ASSET_DIR))
$(call my-copy-assets, $(LOCAL_PATH)/src_ucam/UPhoto/assets, $(LOCAL_ASSET_DIR))
$(call my-copy-assets, $(LOCAL_PATH)/src_ucam/UCommon/puzzle/assets, $(LOCAL_ASSET_DIR))
endif
endif

LOCAL_JNI_SHARED_LIBRARIES := libjni_tinyplanet libjni_jpegutil
LOCAL_REQUIRED_MODULES := libmakeupengine libImageProcessJni libGpuProcessJni libjni_mosaic
include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

ifeq ($(strip $(MY_EDIT_PUZZLE)),true)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
                    libemail-uphoto:src_ucam/UGallery/libs/email.jar

ifeq ($(strip $(TARGET_ARCH)),arm64)
LOCAL_PREBUILT_LIBS += libGpuProcessJni:src_ucam/UPhoto/libs/arm64-v8a/libGpuProcessJni.so
LOCAL_PREBUILT_LIBS += libmakeupengine:src_ucam/UPhoto/libs/arm64-v8a/libmakeupengine.so
LOCAL_PREBUILT_LIBS += libImageProcessJni:src_ucam/UPhoto/libs/arm64-v8a/libImageProcessJni.so
else
LOCAL_PREBUILT_LIBS += libGpuProcessJni:src_ucam/UPhoto/libs/armeabi-v7a/libGpuProcessJni.so
LOCAL_PREBUILT_LIBS += libmakeupengine:src_ucam/UPhoto/libs/armeabi-v7a/libmakeupengine.so
LOCAL_PREBUILT_LIBS += libImageProcessJni:src_ucam/UPhoto/libs/armeabi-v7a/libImageProcessJni.so
endif
else
ifeq ($(strip $(MY_TIME_STAMP)),true)

ifeq ($(strip $(TARGET_ARCH)),arm64)
LOCAL_PREBUILT_LIBS += libImageProcessJni:src_ucam/UPhoto/libs/arm64-v8a/libImageProcessJni.so
else
LOCAL_PREBUILT_LIBS += libImageProcessJni:src_ucam/UPhoto/libs/armeabi-v7a/libImageProcessJni.so
endif
endif

endif

include $(BUILD_MULTI_PREBUILT)
                    
include $(call all-makefiles-under, $(LOCAL_PATH))
