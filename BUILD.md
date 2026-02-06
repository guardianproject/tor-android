These instructions are for building tor-android on a Debian based system.

First install the prerequisite packages:

```bash
sudo apt install autotools-dev
sudo apt install automake
sudo apt install autogen autoconf libtool gettext-base autopoint
sudo apt install git make g++ pkg-config openjdk-17-jdk openjdk-17-jre
```

Then obtain the Android SDK and NDK. The Android SDK is installed by default with Android Studio, and the NDK can be downloaded from within Android Studio's SDK manager.

for now, tor-android is built with NDK toolchain 28.2.13676358

Then set these environment variables for the SDK and NDK:

```bash
export ANDROID_HOME=~/Android/Sdk
export ANDROID_NDK_HOME=~/Android/Sdk/ndk/25.2.9519653
```

Be sure that you have all of the git submodules up-to-date:
```bash
./tor-droid-make.sh fetch
```

To build, run:
```bash
# make a universal tor-android library for every supported architecture
./tor-droid-make.sh build 
# make a tor-android library for particular architectures from:
# arm64-v8a armeabi-v7a x86 x86_64, e.g.:
./tor-droid-make.sh build -a arm64-v8a
```

This will produce an unsigned tor-android AAR
