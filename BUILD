
This document explains how to properly build an Android package of Orbot from
source.

Orbot includes, in the external directory, git repo submodules of:
	- Tor
	- OpenSSL (statically built and patched for Android)
	- LibEvent
	- JTorControl: The Tor Control Library for Java

Please install the following prerequisites (instructions for each follows):
	Android Native Dev Kit or NDK (for C/C++ code):
        http://developer.android.com/sdk/ndk/index.html
	Android Software Dev Kit or SDK (for Java code):
        http://developer.android.com/sdk/index.html
	AutoMake and AutoConf tool
	sudo apt-get install autotools-dev
	sudo apt-get install automake
	sudo apt-get install autogen autoconf libtool gettext-base autopoint

You will need to run the 'android' command in the SDK to install the necessary
Android platform supports (ICS 4.x or android-15)

Be sure that you have all of the git submodules up-to-date:

	./tor-droid-make.sh fetch

To begin building, from the Orbot root directory, it builds all submodules and
the project.

        ./tor-droid-make.sh build

Now build the Android app

(gradle / android studio instructions here)

This will produce an unsigned Tor package APK.

To produce a usable package, you'll need to sign the .apk. The basics on
signing can be found on the Android developer site:

	http://developer.android.com/guide/publishing/app-signing.html


