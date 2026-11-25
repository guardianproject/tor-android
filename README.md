# Tor Android

This is native Android `TorService` built on the Tor shared library built for
Android. The included `libtor.so` binaries can also be used directly as a tor
daemon.

Currently, Tor Android is built with the following versions of `tor`, `libevent`, `openssl`, `zlib` and `zstd`:

| Component |                                                                           Version |
|:----------|----------------------------------------------------------------------------------:|
| tor       |        [0.4.9.17](https://forum.torproject.org/t/security-release-0-4-9-17/22454) |
| libevent  | [2.1.13](https://github.com/libevent/libevent/releases/tag/release-2.1.13-stable) |
| OpenSSL   |            [3.5.9](https://github.com/openssl/openssl/releases/tag/openssl-3.5.9) |
| zlib      |                           [1.4](https://github.com/madler/zlib/releases/tag/v1.4) |
| zstd      |                         [1.6.0](https://github.com/meta/zstd/releases/tag/v1.6.0) |

Tor Android binaries are available on the [Guardian Project Maven Repository](https://github.com/guardianproject/gpmaven)

As of late 2026, a Changelog for new releases is available in [`CHANGELOG.md`](https://github.com/guardianproject/tor-android/blob/master/CHANGELOG.md)

Tor protects your privacy on the internet by hiding the connection between your Internet address and the services you use. We believe Tor  is reasonably secure, but please ensure you read the instructions and  configure it properly. Learn more at https://torproject.org/

## Using `tor-android` in your project

### Gradle Configuration

First, add the Guardian Project's maven repo to your top level Gradle file:

```kts
allprojects {
    repositories {
        // ...
        maven { uri("https://raw.githubusercontent.com/guardianproject/gpmaven/master") }
    }
}
```

Then add the `tor-android` dependency to your project:
```kts
dependencies {
    implementation("info.guardianproject:tor-android:0.4.9.17")
}

```
#### *NOTE:* previously you needed to declare a Gradle dependency for jtorctl, this is no longer needed in all newer builds of tor-android! 

### Declare Necessary Permissions in your app: `INTERNET` and (optionally also) `ACCESS_LOCAL_NETWORK`

Apps using tor-android _must_ declare the `INTERNET` permission in their `AndroidManifest.xml` file!
*Additionally, if your app targets Android 17 (API 37) or higher, you may want to also declare the `ACCESS_LOCAL_NETWORK` permission if you're configuring your app to interact with other devices on your LAN.*

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />
```

Most developers building with `tor-android` will probably not need this extra permission. However, if you're wanting to do things like expose `tor`'s SOCKS Port to other devices on your local network. IE, starting `tor` with this `torrc`, you must use it (but again, only if `targetSdk` > 36):

```
# torrc for exposing SOCKSPort to other devices on your LAN
SOCKSPort 0.0.0.0:9050
SocksPolicy accept *:*
```

Additionally, on AAndroid 17 and up, declaring `ACCESS_LOCAL_NETWORK` on its own is not enough to access devices on your LAN. You _also_ have to request the user's consent for the `ACCESS_LOCAL_NETWORKS` at runtime. More information on this new Android 17 requirement can be found [here...](https://developer.android.com/privacy-and-security/local-network-permission)

## Minimum Requirements for apps using `tor-android`

- In order to use tor-android you need to target Android **API 24** or higher.
- It runs on all standard Android architectures: `arm64-v8a`, `armeabi-v7a`, `x86_64`(`x86` support is possible but is off by default)

## Building `tor-android`

`tor-android` is generally built on Linux, but it can also be built on [macOS](https://github.com/guardianproject/tor-android/pull/186)

It can be built reproducibly using the included Vagrant VM setup.  That will
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

*(NOTE: see instructions for [building Tor on Debian](https://gitlab.com/torproject/tor/-/blob/main/.gitlab-ci.yml?ref_type=heads) for a starting point on the latest Debian dependencies needed to build `tor`...)*

You'll need a valid JDK setup on your system. An easy way to obtain a correctly configured one is to [install SDKMAN](https://sdkman.io/). With SDKMAN installed, you can obtain and use Java 25 like so:

```bash
sdk init 
sdk install java 25.0.2-tem
sdk use java 25.0.2-tem
``` 

Then obtain the Android SDK and NDK. The Android SDK is installed by default with Android Studio, and the NDK can be downloaded from within Android Studio's SDK manager.

For now, `tor-android` is built with NDK toolchain version `30.0.16248370`. It should already be installed if you've built Orbot. 

Then set these environment variables for the SDK and NDK:

```bash
export ANDROID_HOME=~/Android/Sdk
export ANDROID_NDK_HOME=~/Android/Sdk/ndk/30.0.16248370
```

Be sure that you have every git submodule up-to-date:
```bash
./tor-droid-make.sh fetch -c
```

To build, run:
```bash
# make a universal tor-android library (arm64-v8a, armeabi-v7a, x86_64)
./tor-droid-make.sh build 

# other builds 
./tor-droid-make.sh build -a arm64-v8a # for running on 64 bit ARM devices, emulating on arm64 
./tor-droid-make.sh build -a armeabi-v7a # for running on 32 bit ARM devices
./tor-droid-make.sh build -a armeabi-v7a arm64-v8a # for any real device in the wild  
./tor-droid-make.sh build -a x86_64 # for emulating on Intel hosts
./tor-droid-make.sh build -a x86 # generally not used, for emulating API 24 devices on amd64 hosts
```

This will produce an unsigned tor-android AAR.

## Preparing for a release 

### `tor-android/binary/build.gradle.kts`

Update these fields at the top of `tor-android-binary`'s `build.gradle.kts`. 

For a release of `tor` 0.4.9.5 the first digits of `versionCode` and the `versionName` string are the version of `tor` used.

Note that `versionCode` ends in a 0.

```kts
    versionCode = 49050
    versionName = "0.4.9.5"
```

When making new releases of `tor-android` that don't use a new version of tor, change the last digit
of `versionCode` and add a field onto `versionName`, ie:

```kts
    versionCode = 49051
    versionName = "0.4.9.5.1"
```

### `README.md`
Update the versions of the dependencies in the table, as well as the field which contains copy+paste instructions
on how to add `tor-android` to a Gradle project.

### `sampletorapp/build.gradle.kts`
After a release is published, update the version of `tor-android` used in the sample app's Gradle configuration (defined in `gradle/libs.versions.toml`).

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
