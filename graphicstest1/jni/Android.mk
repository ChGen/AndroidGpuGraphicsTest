LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := graphicstest1
LOCAL_SRC_FILES := graphicstest1.cpp

include $(BUILD_SHARED_LIBRARY)
