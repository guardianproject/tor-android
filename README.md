## Tor Android

This is native Android `TorService` built on the Tor shared library built for
Android. The included `libtor.so` binaries can also be used directly as a tor
daemon.

Currently, Tor Android is built with the following versions of `tor`, `libevent`, `openssl`, `zlib` and `zstd`:

| Component |                                                                           Version |
|:----------|----------------------------------------------------------------------------------:|
| tor       |          [0.4.9.11](https://forum.torproject.org/t/security-release-0-4-9-11/21786) |
| libevent  | [2.1.12](https://github.com/libevent/libevent/releases/tag/release-2.1.12-stable) |
| OpenSSL   |            [3.5.7](https://github.com/openssl/openssl/releases/tag/openssl-3.5.7) |
| zlib      |                       [1.3.2](https://github.com/madler/zlib/releases/tag/v1.3.2) |
| zstd      |                     [1.5.7](https://github.com/facebook/zstd/releases/tag/v1.5.7) |

Tor Android binaries are available on the [Guardian Project Maven Repo](https://github.com/guardianproject/gpmaven)

First add the repo to your top level Gradle file:

```kts
allprojects {
    repositories {
        // ...
        maven { uri("https://raw.githubusercontent.com/guardianproject/gpmaven/master") }
    }
}
```

Then add the `tor-android` and `jtorctl` dependencies to your project:
```kts
dependencies {
    implementation("info.guardianproject:tor-android:0.4.9.11")
    implementation("info.guardianproject:jtorctl:0.4.5.7")
}
```

Apps using tor-android need to declare the `INTERNET` permission in their Android Manifest file.
*Additionally, if your app targets Android 17 (API 37), you may want to also declare the `ACCESS_LOCAL_NETWORK` permission if you're configuring your app to interact with devices on your LAN.*
```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />
```

Most developers building with `tor-android` will not need this permission. However, if you're wanting to do things like expose `tor`'s SOCKS port to devices on your network. IE, starting `tor` with this `torrc`, you must use it (but again, only if `targetSdk` > 36):
```
SOCKSPort 0.0.0.0:9050
SocksPolicy accept *:*
```

You'll need to do additional logic at runtime to grant the permission. More information on this new Android 17 restriction can be found [here](https://developer.android.com/privacy-and-security/local-network-permission).

Tor protects your privacy on the internet by hiding the connection 
between your Internet address and the services you use. We believe Tor
is reasonably secure, but please ensure you read the instructions and
configure it properly. Learn more at https://torproject.org/

## Minimum Requirements 

- In order to use tor-android you need to target Android **API 24** or higher.
- It runs on all standard Android architectures: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`

## [Tor Frequently Asked Questions](https://support.torproject.org/faq/)

## Building `tor-android`

Please see: [BUILD.md](https://raw.githubusercontent.com/guardianproject/tor-android/master/BUILD.md)

`tor-android` is generally built on Linux, but it can also be built on [macOS](https://github.com/guardianproject/tor-android/pull/186)

This can be built reproducibly using the included Vagrant VM setup.  That will
run with either `libvirt` or VirtualBox.  The provisioning is based on the
"release" job in `.gitlab-ci.yml`.

### Building on Debian
First install the prerequisite packages:

```bash
sudo apt update
sudo apt install autoconf \
    autogen \
    automake \
    autopoint \
    autotools-dev \
    gettext-base \
    gettext \
    git \
    libtool \
    make \
    patch \
    pkg-config \
    g++ \
    uidmap \
    libseccomp-dev \
    libscrypt-dev \
    build-essential \
    ca-certificates \
    po4a \
    libzstd-dev
sudo apt install linux-headers-$(uname -r)
```

*(NOTE: see instructions for [building Tor on Debian](https://gitlab.com/torproject/tor/-/blob/main/.gitlab-ci.yml?ref_type=heads) for a starting point on the latest Debian dependencies used to build `tor`...)*

You'll need a valid JDK setup on your system. An easy way to obtain a correctly configured one is to [install SDKMAN](https://sdkman.io/). With SDKMAN installed, you can obtain and use Java 25 like so:

```bash
sdk init 
sdk install java 25.0.2-tem
sdk use java 25.0.2-tem
``` 

Then obtain the Android SDK and NDK. The Android SDK is installed by default with Android Studio, and the NDK can be downloaded from within Android Studio's SDK manager.

for now, tor-android is built with NDK toolchain 29.0.14206865

Then set these environment variables for the SDK and NDK:

```bash
export ANDROID_HOME=~/Android/Sdk
export ANDROID_NDK_HOME=~/Android/Sdk/ndk/29.0.14206865
```

Be sure that you have every git submodule up-to-date:
```bash
./tor-droid-make.sh fetch -c
```

To build, run:
```bash
# make a universal tor-android library for every supported architecture
./tor-droid-make.sh build 
# make a tor-android library for particular architectures from: arm64-v8a armeabi-v7a x86 x86_64

# 64 bit ARM APK, for running on devices
./tor-droid-make.sh build -a arm64-v8a

# 64 bit Intel APK, for running on emulators with Intel hosts
./tor-droid-make.sh build -a x86_64 
```

This will produce an unsigned tor-android AAR.

*(NOTE: that `./tor-droid-make.sh ...` does not currently work in the `fish` shell, use `zsh` or `bash`...)*

## Preparing for a release 

#### Top-level `build.gradle.kts`

Update these fields at the top of `build.gradle.kts`. For example, for a release of `tor` 0.4.9.5 the first digits of `versionCode` and the `versionName` string are the version of `tor` used.

Note that `versionCode` ends in a 0.

```kts
    versionCode = 49050
    versionName = "0.4.9.5"
```

If you are making new releases of `tor-android` that don't include a new update of tor, change the last digit 
of `versionCode` and add a field onto `versionName`, ie:

```kts
    versionCode = 49051
    versionName = "0.4.9.5.1"
```

#### `README.md`
Update the versions of the dependencies in the table, as well as the field which contains copy+paste instructions
on how to add `tor-android` to a Gradle project.

#### `gradle.properties`
Update `VERSION_NAME` to the version of `tor` used in this build, ie `VERSION_NAME=0.4.9.5`

#### `sampletorapp/build.gradle.kts`
Update the version of `tor-android` used in the sample app's Gradle configuration. 

## Publishing `tor-android`

Once you build the binaries, you can use Gradle tasks to publish this in various ways, if you have the right credentials:
```bash
# Publish to your local Maven repository:
./gradlew publishToMavenLocal

# Publish to GitHub packages:
./gradlew publishReleasePublicationToGitHubPackagesRepository

# Publish to Gradle Central:
./gradlew publishAggregationToCentralPortal
```
