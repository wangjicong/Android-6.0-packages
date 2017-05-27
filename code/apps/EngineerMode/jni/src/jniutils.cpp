/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "engjni"
#include "utils/Log.h"

#include <stdint.h>
#include <jni.h>

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
/// SUN:jiazhenl 20160822 modify for call auto record start @{
#include <malloc.h>
#include <dlfcn.h>
#include <stdio.h>
#include <cutils/properties.h>

#define LIBRARY_PATH "/system/lib/hw/"
/// SUN:jiazhenl 20160822 modify for call auto record end @}
#define ROOT_MAGIC 0x524F4F54 //"ROOT"
#define ROOT_OFFSET 512


extern "C" {
#include "sprd_efuse_hw.h"
}


typedef struct {
  uint32_t magic;
  uint32_t root_flag;
} root_stat_t;

static jint get_rootflag(JNIEnv* env, jobject thiz) {

    char block_device[100];
    //strcpy(block_device, "/dev/block/platform/sdio_emmc/by-name/miscdata");
    strcpy(block_device, "/sys/root_recorder/rootrecorder");

    root_stat_t stat;
    FILE *device;
    int retval = 0;

    device = fopen(block_device, "r");
    if (!device) {
//        ALOGE("[root_recorder] Could not open block device %s (%s).\n", block_device, strerror(errno));
        goto out;
    }

    //if (fseek(device, ROOT_OFFSET, SEEK_SET) < 0) {
        //ALOGE("[root_recorder] Could not seek to start of ROOT FLAG metadata block.\n");
        //goto out;
    //}
    if (!fread(&stat, sizeof(root_stat_t), 1, device)) {
        ALOGE("[root_recorder] Couldn't read magic number!\n");
        goto out;
    }

    ALOGD("[root_recorder] magic=%d\n",stat.magic);
    ALOGD("[root_recorder] rootflag=%d\n",stat.root_flag);

    if(stat.magic == ROOT_MAGIC) {
        ALOGE("[root_recorder] sprd magic verify pass.\n");
        retval = stat.root_flag;
    } else {
        ALOGE("[root_recorder] sprd magic verify failed.\n");
    }

out:
    if (device)
        fclose(device);
    return retval;

}


static jboolean Hardware_hashValueWrited(JNIEnv* env, jobject thiz) {      
if (efuse_is_hash_write() == 1) {
ALOGD("hash value has writed");
return true;
}
ALOGD("hash value has not writed");
return false;
}

static const char *hardWareClassPathName =
        "com.sprd.engineermode.hardware/HardWareFragment";
        
static JNINativeMethod getMethods[] = {
		{"hashValueWrited", "()Z", (void*) Hardware_hashValueWrited },
		{"get_rootflag", "()I", (void*)get_rootflag}
};

/// SUN:jiazhenl 20160822 modify for call auto record start @{
static const char *sensorsIDClassPathName =
        "com.sprd.engineermode.debuglog/SensorsIDActivity";
        
static jstring Hardware_getBackCameraID(JNIEnv* env, jobject thiz) {
    ALOGE("Native Hardware_getBackCameraID");
	void *handle = NULL;
	char * (*read_sensor) (int);
	char lib_full_name[60] = { 0 };
	char prop[PROPERTY_VALUE_MAX];
	char *pFront = NULL;
	char *pBack = NULL;
	property_get("ro.board.platform", prop, NULL);//jiazhenl modify ro.hardware
	sprintf(lib_full_name, "%scamera.%s.so", LIBRARY_PATH, prop);
	ALOGE("Native Hardware_getFrontCameraID 0 lib_full_name=%s", lib_full_name);
	handle = dlopen(lib_full_name, RTLD_LAZY);
	if (!handle) {
	    ALOGE("Native Hardware_getBackCameraID 1 dlerror()=%s", dlerror());
	    return (jstring)"Fail to open camera.sc8830.so";
	}
	else
	{
		read_sensor = (char * (*) (int)) dlsym(handle, "sensor_rid_read_sensor");
		ALOGE("Native Hardware_getBackCameraID 2 dlerror()=%s", dlerror());
	}

	int len = strlen((*read_sensor) (0));
	pBack = (char*)malloc(len + 1);
	memset(pBack,0,len + 1);
	memcpy(pBack,(*read_sensor) (0),len);
	
	dlclose(handle);
	return env->NewStringUTF(pBack);
}

static jstring Hardware_getFrontCameraID(JNIEnv* env, jobject thiz) {
    ALOGE("Native Hardware_getFrontCameraID");
	void *handle = NULL;
	char * (*read_sensor) (int);
	char lib_full_name[60] = { 0 };
	char prop[PROPERTY_VALUE_MAX];
	char *pFront = NULL;
	char *pBack = NULL;
	property_get("ro.board.platform", prop, NULL);//jiazhenl modify ro.hardware
	sprintf(lib_full_name, "%scamera.%s.so", LIBRARY_PATH, prop);
	ALOGE("Native Hardware_getFrontCameraID 0 lib_full_name=%s", lib_full_name);
	handle = dlopen(lib_full_name, RTLD_LAZY);
	if (!handle) {
	    ALOGE("Native Hardware_getBackCameraID 1 dlerror()=%s", dlerror());
	    return (jstring)"Fail to open camera.sc8830.so";
	}
	else
	{
		read_sensor = (char * (*) (int)) dlsym(handle, "sensor_rid_read_sensor");
		ALOGE("Native Hardware_getBackCameraID 2 dlerror()=%s", dlerror());
	}
	
	int len = strlen((*read_sensor) (1));
	pFront = (char*)malloc(len + 1);
	memset(pFront,0,len + 1);
	memcpy(pFront,(*read_sensor) (1),len);
	
	dlclose(handle);
	return env->NewStringUTF(pFront);
}

static JNINativeMethod getCameraIDMethods[] = {
        {"getBackCameraID", "()Ljava/lang/String;", (void*) Hardware_getBackCameraID},
        {"getFrontCameraID", "()Ljava/lang/String;", (void*) Hardware_getFrontCameraID},
};
/// SUN:jiazhenl 20160822 modify for call auto record end @}
        
static int registerNativeMethods(JNIEnv* env, const char* className,
        JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        ALOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        ALOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}
        
jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    //use JNI1.6
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        ALOGE("Error: GetEnv failed in JNI_OnLoad");
        return -1;
    }
    if (!registerNativeMethods(env, hardWareClassPathName, getMethods,
            sizeof(getMethods) / sizeof(getMethods[0]))) {
        ALOGE("Error: could not register native methods for HardwareFragment");
        return -1;
    }
	/// SUN:jiazhenl 20160822 modify for call auto record start @{
    if (!registerNativeMethods(env, sensorsIDClassPathName, getCameraIDMethods,
            sizeof(getCameraIDMethods) / sizeof(getCameraIDMethods[0]))) {
        ALOGE("Error: could not register native methods for SensorsIDActivity");
        return -1;
    }
	/// SUN:jiazhenl 20160822 modify for call auto record end @}
      return JNI_VERSION_1_6;
}
