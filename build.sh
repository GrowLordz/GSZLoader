#!/bin/bash
export ANDROID_HOME=~/android-sdk
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

cd ~/GSZLoader
gradle assembleDebug

if [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
    echo "BUILD SUCCESS!"
    echo "APK: app/build/outputs/apk/debug/app-debug.apk"
    cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/GSZLoader.apk
    echo "Copied to /sdcard/Download/GSZLoader.apk"
else
    echo "BUILD FAILED!"
fi
