# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
LOCAL_PATH := $(call my-dir)
ROOT_PATH := $(LOCAL_PATH)
//EXTERN_PATH := $(LOCAL_PATH)/../../../../external

########################################################
## libevent
########################################################

include $(CLEAR_VARS)

LIBEVENT_SOURCE := event.c

LOCAL_MODULE := libevent
LOCAL_CFLAGS := -O2 -I$(LOCAL_PATH)/libevent -I$(LOCAL_PATH)/libevent/include -I$(LOCAL_PATH)/libevent/include/event2
LOCAL_CFLAGS += -std=gnu99
LOCAL_CFLAGS += -DNDEBUG -DANDROID

LOCAL_C_INCLUDES := $(LOCAL_PATH)/libevent
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libevent/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libevent/include/event2
//LOCAL_C_INCLUDES += $(LOCAL_PATH)/libevent/WIN32-Code/nmake/
LOCAL_SRC_FILES := $(addprefix libevent/, $(LIBEVENT_SOURCE))

include $(BUILD_STATIC_LIBRARY)
