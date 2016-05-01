LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := \
	org.cyanogenmod.platform.sdk \
    org.cyanogenmod.platform.internal

LOCAL_MODULE := org.cmremix.utils

include $(BUILD_JAVA_LIBRARY)


