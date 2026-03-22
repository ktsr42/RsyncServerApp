#!/bin/bash

export MESA_LOADER_DRIVER_OVERRIDE=i965
export LIBGL_ALWAYS_SOFTWARE=0
export ANDROID_EMULATOR_USE_VULKAN=true
~/Android/Sdk/emulator/emulator -avd Medium_Phone_API_35
