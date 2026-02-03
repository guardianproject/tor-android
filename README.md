## Tor Android

This is native Android `TorService` built on the Tor shared library built for
Android.  The included `libtor.so` binaries can also be used directly as a tor
daemon.

Currently, Tor Android is built with the following versions of `tor`, `libevent`, `openssl`, `zlib` and `zstd`:

| Component | Version  |
|:--------- | --------:|
| tor       | [0.4.8.22](https://forum.torproject.org/t/stable-release-0-4-8-22/) |
| libevent  | [2.1.12](https://github.com/libevent/libevent/releases/tag/release-2.1.12-stable)   |
| OpenSSL   | [3.5.5](https://github.com/openssl/openssl/releases/tag/openssl-3.5.5)    |
| zlib   | [1.3.1](https://github.com/madler/zlib/releases/tag/v1.3.1)    |
| zstd | [1.5.7](https://github.com/facebook/zstd/releases/tag/v1.5.7)    |

Tor Android binaries are available on the [Guardian Project Maven Repo](https://github.com/guardianproject/gpmaven)

First add the repo to your top level `build.gradle` project:
```groovy
allprojects {
    repositories {
        // ...
        maven { url "https://raw.githubusercontent.com/guardianproject/gpmaven/master" }
    }
}
```

Then add the `tor-android` and `jtorctl` dependencies to your project:
```groovy
dependencies {
    implementation("info.guardianproject:tor-android:0.4.8.22")
    implementation("info.guardianproject:jtorctl:0.4.5.7")
}
```

Apps using tor-android need to declare the `INTERNET` permission in their Android Manifest file:

```xml
    <uses-permission android:name="android.permission.INTERNET"/>
```

Tor protects your privacy on the internet by hiding the connection 
between your Internet address and the services you use. We believe Tor
is reasonably secure, but please ensure you read the instructions and
configure it properly. Learn more at https://torproject.org/

## Minimum Requirements 

In order to use tor-android you need to target Android **API 24** or higher. 

It runs on the following hardware architectures:
- `arm64-v8a` 
- `armeabi-v7a`
- `x86`
- `x86_64`

## Tor Frequently Asked Questions:
        
- https://2019.www.torproject.org/docs/faq
- https://support.torproject.org/faq/


## Building `tor-android`

Please see: https://raw.githubusercontent.com/guardianproject/tor-android/master/BUILD.md

This can be built reproducibly using the included Vagrant VM setup.  That will
run with either `libvirt` or VirtualBox.  The provisioning is based on the
"release" job in `.gitlab-ci.yml`.

## Preparing for a release 

#### `build.gradle`

Update these fields in `build.gradle`. For `tor` 0.4.8.22 the first digits of `versionCode` and the `versionName` string are the version of `tor` used. `versionCode` ends in a `0`.

```groovy
    versionCode = 48220
    versionName = "0.4.8.22"
```

If you are making new releases of `tor-android` that don't include a new update of tor, change the last digit 
of `versionCode` and add a field onto `versionName`, ie:

```groovy
    versionCode = 48221
    versionName = "0.4.8.22.1"
```

#### `README.md`
Update the versions of the dependencies in the table, as well as the field which contains copy+paste instructions
on how to add `tor-android` to a gradle project.

#### `gradle.properties`
Update `VERSION_NAME` to the version of `tor` used in this build, ie `VERSION_NAME=0.4.8.22`

#### `sampletorapp/build.gradle`
Update the version of `tor-android` used in the sample app's gradle configuration. 

## Publishing `tor-android`

Once you build the binaries, you can use gradle tasks to publish this in various ways, if you have the right credentials

Publish to your local Maven repository:
`./gradlew publishToMavenLocal`

Publish to Github packages:
`./gradlew publishReleasePublicationToGitHubPackagesRepository`

Publish to Gradle Central:
`./gradlew publishAggregationToCentralPortal`





