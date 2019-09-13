# Please install the following prerequisites (instructions for each follows):
# 	Android OS SDK: http://source.android.com/download
#
# Install and prepare the Android OS SDK ( http://source.android.com/download )
# on Debian or Ubuntu

EXTERNAL_ROOT := $(shell pwd)

DEBUG ?= 0

# No-op command.
NOOP = true

# Android now has 64-bit and 32-bit versions of the NDK for GNU/Linux.  We
# assume that the build platform uses the appropriate version, otherwise the
# user building this will have to manually set NDK_PROCESSOR or NDK_TOOLCHAIN.
CPU := $(shell uname -m)
ifeq ($(CPU),x86_64)
 NDK_PROCESSOR=x86_64
else
 NDK_PROCESSOR=x86
endif

# matching NDK revisions are required for reproducible builds
ANDROID_NDK_HOME ?= /opt/android-ndk
NDK_REVISION := $(shell sed -n 's,^Pkg.Revision *= *\([^ ]*\),\1,p' $(ANDROID_NDK_HOME)/source.properties)
NDK_REQUIRED_REVISION := 20.0.5594570
MIN_NDK_VERSION := 19
ifeq ($(shell test $(MIN_NDK_VERSION) -gt $(firstword $(subst ., ,$(NDK_REVISION))); echo $$?),0)
 $(error NDK r$(MIN_NDK_VERSION) or newer required! r$(NDK_REVISION) is installed at $(ANDROID_NDK_HOME).)
endif
ifneq ($(NDK_REQUIRED_REVISION), $(NDK_REVISION))
 $(warning WARNING: NDK $(NDK_REQUIRED_REVISION) required for reproducible builds, $(NDK_REVISION) is installed.)
endif


ifneq ($(filter mips%, $(APP_ABI)),)
 HOST := $(NDK_ABI)el-linux-android
 ALTHOST := $(HOST)
 GREP_CHECK := MIPS
 NDK_BIT := 32
 NDK_PLATFORM_LEVEL := 16
 NDK_TOOLCHAIN := $(HOST)-$(NDK_TOOLCHAIN_VERSION)
endif
ifneq ($(filter arm64-v8a, $(APP_ABI)),)
 HOST := aarch64-linux-android
 ALTHOST := $(HOST)
 GREP_CHECK := aarch64
 NDK_ABI := arm64
 NDK_BIT := 64
 NDK_PLATFORM_LEVEL := 21
 NDK_TOOLCHAIN := $(HOST)-$(NDK_TOOLCHAIN_VERSION)
endif
ifneq ($(filter armeabi-v7a, $(APP_ABI)),)
 HOST := armv7a-linux-androideabi
 ALTHOST := arm-linux-androideabi
 GREP_CHECK := EABI5
 NDK_ABI := arm
 NDK_BIT := 32
 NDK_PLATFORM_LEVEL := 16
 NDK_TOOLCHAIN := $(HOST)-$(NDK_TOOLCHAIN_VERSION)
endif
ifneq ($(filter x86, $(APP_ABI)),)
 HOST := i686-linux-android
 ALTHOST := $(HOST)
 GREP_CHECK := 80386
 NDK_ABI := x86
 NDK_BIT := 32
 NDK_PLATFORM_LEVEL := 16
 NDK_TOOLCHAIN := $(NDK_ABI)-$(NDK_TOOLCHAIN_VERSION)
endif
ifneq ($(filter x86_64, $(APP_ABI)),)
 HOST := x86_64-linux-android
 ALTHOST := $(HOST)
 GREP_CHECK := x86-64
 NDK_ABI := x86_64
 NDK_BIT := 64
 NDK_PLATFORM_LEVEL := 21
 NDK_TOOLCHAIN := $(NDK_ABI)-$(NDK_TOOLCHAIN_VERSION)
endif

NDK_SYSROOT=$(ANDROID_NDK_HOME)/platforms/android-$(NDK_PLATFORM_LEVEL)/arch-$(NDK_ABI)
NDK_UNAME := $(shell uname -s | tr '[A-Z]' '[a-z]')
NDK_TOOLCHAIN_BASE=$(ANDROID_NDK_HOME)/toolchains/llvm/prebuilt/$(NDK_UNAME)-$(NDK_PROCESSOR)

CC := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)$(NDK_PLATFORM_LEVEL)-clang
CXX := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)$(NDK_PLATFORM_LEVEL)-clang++ --sysroot=$(NDK_SYSROOT)
CPP := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)$(NDK_PLATFORM_LEVEL)-clang++ --sysroot=$(NDK_SYSROOT)
LD := $(NDK_TOOLCHAIN_BASE)/bin/$(ALTHOST)-ld
AR := $(NDK_TOOLCHAIN_BASE)/bin/$(ALTHOST)-ar
RANLIB := $(NDK_TOOLCHAIN_BASE)/bin/$(ALTHOST)-ranlib

ifeq ($(DEBUG), 1)
	STRIP := $(NOOP)
else
	STRIP := $(NDK_TOOLCHAIN_BASE)/bin/$(ALTHOST)-strip --strip-unneeded -R .note -R .comment --strip-debug
endif

TZ := UTC
LC_ALL := C.UTF-8
SOURCE_DATE_EPOCH := 1234567890
export LC_ALL TZ SOURCE_DATE_EPOCH

REPRODUCIBLE_CFLAGS = \
  -fbuild-session-timestamp=$(SOURCE_DATE_EPOCH) \
  -fdebug-prefix-map=OLD=NEW

# for gcc but not clang:
#  -Wl,--no-insert-timestamp
#  -fmacro-prefix-map=OLD=NEW

ALL_CFLAGS = $(REPRODUCIBLE_CFLAGS) $(CFLAGS)

OUTPUT_FILE := $(EXTERNAL_ROOT)/tor/src/app/tor


.PHONY = clean showsetup \
	assets assets-clean \
	openssl-static openssl-clean \
	libevent libevent-clean \
	lzma lzma-clean \
	zstd zstd-clean \
	tor tor-clean

all: test-setup assets

test-setup:
	test -d $(NDK_SYSROOT)
	test -x $(AR)
	test -x $(CC)
	test -x $(LD)
	test -x $(RANLIB)

#------------------------------------------------------------------------------#
# openssl

PATH := $(ANDROID_NDK_HOME)/toolchains/llvm/prebuilt/linux-x86_64/bin:$(PATH)


openssl/Makefile: openssl/Configure $(wildcard openssl/Configurations/*.*)
	cd openssl && PATH=$(PATH) \
		./Configure android-$(NDK_ABI) -D__ANDROID_API__=$(NDK_PLATFORM_LEVEL) \
			--prefix=$(EXTERNAL_ROOT) --openssldir=$(EXTERNAL_ROOT)

openssl-build-stamp: openssl/Makefile lib/libcrypto.a lib/libssl.a
	PATH=$(PATH) make -j8 -C openssl depend
	PATH=$(PATH) make -j8 -C openssl build_libs
	touch openssl-build-stamp

openssl-static: openssl-build-stamp
	make -C openssl install_dev
	rm lib/*.so*

openssl-clean:
	-rm openssl-build-stamp
	-rm lib/libcrypto.a
	-rm lib/libssl.a
	-cd openssl && \
		git clean -fdx

#------------------------------------------------------------------------------#
# libevent

libevent/Makefile:
	cp libevent-patch-1 libevent
	-cd libevent && \
	 	patch -N -p1 --reject-file=- < libevent-patch-1
	cd libevent && ./autogen.sh
	cp config.sub libevent
	cp config.guess libevent
	cd libevent && \
		AR="$(AR)" \
		CC="$(CC)" \
		CFLAGS="$(ALL_CFLAGS) -I$(EXTERNAL_ROOT)/include" \
		RANLIB=$(RANLIB) \
			./configure \
				--host=$(ALTHOST) \
				--disable-libevent-regress \
				--disable-samples \
				--disable-shared

libevent-build-stamp: libevent/Makefile
	$(MAKE) -C libevent ./include/event2/event-config.h all-am
	touch libevent-build-stamp

libevent: openssl-static libevent-build-stamp
	test -d lib || mkdir lib
	test -d include || mkdir include
	cp libevent/.libs/libevent.a lib
	cp -R libevent/include/event2 include

libevent-clean:
	-rm -f include/event2
	-rm -f lib/libevent.a
	-rm -f libevent-build-stamp
	-cd libevent && \
		git clean -fdx
#------------------------------------------------------------------------------#
# lzma

xz/Makefile:
	cd xz && ./autogen.sh
	cp config.sub xz
	cp config.guess xz
	cd xz && \
		AR="$(AR)" \
		CC="$(CC)" \
		CFLAGS="$(ALL_CFLAGS) -std=c99" \
		CXX="$(CXX)" \
		PKG_CONFIG_PATH="$(EXTERNAL_ROOT)/lib/pkgconfig" \
		RANLIB="$(RANLIB)" \
			./configure \
				--host=$(HOST) \
				--enable-static \
				--disable-doc \
				--disable-lzma-links \
				--disable-lzmadec \
				--disable-lzmainfo \
				--disable-scripts \
				--disable-shared \
				--disable-xz \
				--disable-xzdec \
				--prefix=$(EXTERNAL_ROOT)

lzma: lzma-build-stamp
	$(MAKE) -C xz install

lzma-build-stamp: xz/Makefile
	touch lzma-build-stamp

lzma-clean:
	-rm -rf include/lzma
	-rm -f include/lzma.h
	-rm -f lib/liblzma.a
	-rm -f lib/liblzma.la
	-rm -f lzma-build-stamp
	-cd xz && \
		git clean -fdx
#------------------------------------------------------------------------------#
# zstd

zstd: zstd-build-stamp
	$(MAKE) -C zstd/lib \
		AR="$(AR)" \
		CC="$(CC)" \
		CFLAGS="$(ALL_CFLAGS)" \
		CXX="$(CXX)" \
		PREFIX="$(EXTERNAL_ROOT)" \
		RANLIB="$(RANLIB)" \
		libzstd.a-mt
	$(MAKE) -C zstd/lib \
		AR="$(AR)" \
		CC="$(CC)" \
		CFLAGS="$(ALL_CFLAGS)" \
		CXX="$(CXX)" \
		PREFIX="$(EXTERNAL_ROOT)" \
		RANLIB="$(RANLIB)" \
		libzstd.pc
	test -d lib || mkdir lib
	test -d lib/pkgconfig || mkdir lib/pkgconfig
	test -d include || mkdir include
	cp zstd/lib/libzstd.a lib
	cp zstd/lib/libzstd.pc lib/pkgconfig
	cp zstd/lib/zstd.h include
	cp zstd/lib/common/zstd_errors.h include
	cp zstd/lib/deprecated/zbuff.h include
	cp zstd/lib/dictBuilder/zdict.h include

zstd-build-stamp:
	touch zstd-build-stamp

zstd-clean:
	-rm -f include/zstd.h include/zstd_errors.h include/zbuff.h include/zdict.h
	-rm -f lib/libzstd.a
	-rm -f lib/pkgconfig/libzstd.pc
	-rm -f zstd-build-stamp
	-cd zstd && \
		git clean -fdx
#------------------------------------------------------------------------------#
# tor

tor/configure: 
	cd tor && \
	 ./autogen.sh

tor/Makefile: tor/configure
	cp config.sub tor
	cp config.guess tor
	git -C tor checkout configure.ac
	cd tor && patch -N -p1 --reject-file=- < ../enable-android.patch
	cd tor && ./autogen.sh
	cd tor && \
		AR="$(AR)" \
		CC="$(CC)" \
		CFLAGS="$(ALL_CFLAGS) -I$(EXTERNAL_ROOT)/include -I$(EXTERNAL_ROOT)/include/event2" \
		LIBS="-L$(EXTERNAL_ROOT)/lib" \
		PKG_CONFIG_PATH="$(EXTERNAL_ROOT)/lib/pkgconfig" \
		RANLIB="$(RANLIB)" \
			./configure \
				--host=$(ALTHOST) \
				--enable-android \
				--enable-lzma \
				--enable-static-libevent --with-libevent-dir=$(EXTERNAL_ROOT) \
				--enable-static-openssl --with-openssl-dir=$(EXTERNAL_ROOT) \
				--enable-zstd

tor-build-stamp: tor/Makefile
	$(MAKE) -C tor all-am
	touch tor-build-stamp

tor: lzma zstd libevent openssl-static tor-build-stamp

tor-clean:
	-rm -f $(OUTPUT_FILE)
	-rm -f tor-build-stamp
	-cd tor && \
		git clean -fdx
	-cd tor && \
		git reset HEAD --hard

#------------------------------------------------------------------------------#
#create and clean assets: FYI - tor is stored as a ZIP file with an so extension
#in the libs directly, so it is handled like a shared library for local installation

assets: tor
	file $(OUTPUT_FILE) | grep $(GREP_CHECK)
	install -d ../tor-android-binary/src/main/libs/$(APP_ABI)
	cp $(OUTPUT_FILE) ../tor-android-binary/src/main/libs/$(APP_ABI)/tor.so
	-$(STRIP) ../tor-android-binary/src/main/libs/$(APP_ABI)/tor.so

assets-clean:
	-rm ../tor-android-binary/src/main/libs/$(APP_ABI)/tor.so

#------------------------------------------------------------------------------#
# cleanup, cleanup, put the toys away

##clean: openssl-clean libevent-clean tor-clean polipo-clean assets-clean
clean: openssl-clean libevent-clean lzma-clean zstd-clean tor-clean

#------------------------------------------------------------------------------#
# debugging stuff

showsetup:
	@echo "ALL_CFLAGS: $(ALL_CFLAGS)"
	@echo "ARFLAGS: $(ARFLAGS)"
	@echo "APP_ABI: $(APP_ABI)"
	@echo "CC: $(CC)"
	@echo "CFLAGS: $(CFLAGS)"
	@echo "HOST: $(HOST)"
	@echo "LD: $(LD)"
	@echo "LDFLAGS: $(LDFLAGS)"
	@echo "NDK_ABI: $(NDK_ABI)"
	@echo "NDK_BIT: $(NDK_BIT)"
	@echo "NDK_PLATFORM_LEVEL: $(NDK_PLATFORM_LEVEL)"
	@echo "NDK_SYSROOT: $(NDK_SYSROOT)"
	@echo "NDK_TOOLCHAIN: $(NDK_TOOLCHAIN)"
	@echo "PATH: $(PATH)"
	@echo "RANLIB: $(RANLIB)"
	@echo "REPRODUCIBLE_CFLAGS: $(REPRODUCIBLE_CFLAGS)"
	@echo "LC_ALL: $$LC_ALL"
	@echo "SOURCE_DATE_EPOCH: $$SOURCE_DATE_EPOCH"
	@echo "TZ: $$TZ"
