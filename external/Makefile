# Please install the following prerequisites (instructions for each follows):
# 	Android OS SDK: http://source.android.com/download
#
# Install and prepare the Android OS SDK ( http://source.android.com/download )
# on Debian or Ubuntu

EXTERNAL_ROOT := $(shell pwd)

DEBUG ?= 0

MAKE ?= make -j`nproc`

# Android now has 64-bit and 32-bit versions of the NDK for GNU/Linux.  We
# assume that the build platform uses the appropriate version, otherwise the
# user building this will have to manually set NDK_PROCESSOR or NDK_TOOLCHAIN.
CPU := $(shell uname -m)
ifeq ($(CPU),x86_64)
 NDK_PROCESSOR=x86_64
else
 NDK_PROCESSOR=x86
endif

ifeq ($(APP_ABI), )
 $(error APP_ABI must be set to an NDK-supported ABI/arch for this to run!)
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
 HOST := mipsel-linux-android
 ALTHOST := $(HOST)
 GREP_CHECK := MIPS
 NDK_ABI := mips
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

export CC := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)$(NDK_PLATFORM_LEVEL)-clang


export TZ := UTC
export LC_ALL := C.UTF-8
export SOURCE_DATE_EPOCH := 1234567890

REPRODUCIBLE_CFLAGS := \
  -fbuild-session-timestamp=$(SOURCE_DATE_EPOCH) \
  -fdebug-prefix-map=OLD=NEW

# for gcc but not clang:
#  -Wl,--no-insert-timestamp
#  -fmacro-prefix-map=OLD=NEW

ALL_CFLAGS := $(REPRODUCIBLE_CFLAGS) $(CFLAGS) -Os
export CFLAGS := $(ALL_CFLAGS) -I$(EXTERNAL_ROOT)/include


OUTPUT_FILE := $(EXTERNAL_ROOT)/tor/src/app/tor
INSTALL_DIR := $(EXTERNAL_ROOT)/lib/$(APP_ABI)

.PHONY = clean showsetup \
	PREBUILD_SHARED_LIBRARY PREBUILD_SHARED_LIBRARY-clean \
	openssl-clean \
	libevent-clean \
	lzma-clean \
	zstd-clean \
	tor tor-clean

all: test-setup PREBUILD_SHARED_LIBRARY

test-setup:
	test -d $(NDK_SYSROOT)
	test -x $(CC)
	printf 'int main() {return 0;}\n' > .test.c
	$(CC) $(CFLAGS) .test.c
	rm -f .test.c a.out


#------------------------------------------------------------------------------#
# openssl
#
# not all of openssl is needed, only the parts that Tor needs
# https://trac.torproject.org/projects/tor/ticket/32200
# https://gitweb.torproject.org/tor.git/tree/src/lib/tls/ciphers.inc
# https://wiki.openssl.org/index.php/Compilation_and_Installation

PATH := $(ANDROID_NDK_HOME)/toolchains/llvm/prebuilt/linux-x86_64/bin:$(PATH)


openssl/Makefile: openssl/Configure $(wildcard openssl/Configurations/*.*)
	cd openssl && PATH=$(PATH) \
		./Configure \
			no-comp no-dtls no-ec2m no-psk no-srp no-ssl2 no-ssl3 \
			no-camellia no-idea no-md2 no-md4 no-mdc2 no-rc2 no-rc4 no-rc5 no-rmd160 no-whirlpool \
			no-dso no-engine no-hw no-ui-console \
			no-shared no-unit-test \
			android-$(NDK_ABI) \
			-D__ANDROID_API__=$(NDK_PLATFORM_LEVEL) \
			--prefix=$(EXTERNAL_ROOT) \
			--openssldir=$(EXTERNAL_ROOT)

openssl-build-stamp: openssl/Makefile
ifeq ($(V), 0)
	PATH=$(PATH) make -j`nproc` --silent -C openssl install_dev ECHO=':'
else
	PATH=$(PATH) make -j`nproc` -C openssl install_dev
endif
	touch $@

openssl-clean:
	-rm openssl-build-stamp
	-rm lib/libcrypto.a
	-rm lib/libssl.a
	-make -C openssl uninstall_dev > /dev/null
	-make -C openssl clean
	-cd openssl && \
		git clean -fdx > /dev/null


#------------------------------------------------------------------------------#
# libevent

libevent/Makefile: libevent/Makefile.am libevent/configure.ac
	cd libevent && ./autogen.sh
	cd libevent && ./configure \
				--host=$(ALTHOST) \
				--disable-libevent-regress \
				--disable-samples \
				--disable-shared \
			        --prefix=$(EXTERNAL_ROOT)

libevent-build-stamp: openssl-build-stamp libevent/Makefile
	$(MAKE) -C libevent install
	touch $@

libevent-clean:
	-rm -f lib/libevent.a
	-rm -f libevent-build-stamp
	-$(MAKE) -C libevent uninstall
	-$(MAKE) -C libevent clean
	-cd libevent && \
		git clean -fdx > /dev/null


#------------------------------------------------------------------------------#
# lzma

xz/Makefile: xz/configure.ac xz/Makefile.am
	cd xz && ./autogen.sh
	cd xz && ./configure \
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

lzma-build-stamp: xz/Makefile
	$(MAKE) -C xz install
	touch $@

lzma-clean:
	-$(MAKE) -C xz uninstall
	-$(MAKE) -C xz clean
	-rm -rf include/lzma
	-rm -f include/lzma.h
	-rm -f lib/liblzma.a
	-rm -f lib/liblzma.la
	-rm -f lzma-build-stamp
	-cd xz && \
		git clean -fdx > /dev/null


#------------------------------------------------------------------------------#
# zstd

zstd-build-stamp:
	$(MAKE) -C zstd/lib \
		PREFIX=$(EXTERNAL_ROOT) \
		libzstd.a-mt
	$(MAKE) -C zstd/lib \
		PREFIX=$(EXTERNAL_ROOT) \
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
	touch $@

zstd-clean:
	-rm -f include/zstd.h include/zstd_errors.h include/zbuff.h include/zdict.h
	-rm -f lib/libzstd.a
	-rm -f lib/pkgconfig/libzstd.pc
	-rm -f zstd-build-stamp
	-$(MAKE) -C zstd uninstall
	-$(MAKE) -C zstd clean
	-cd zstd && \
		git clean -fdx > /dev/null


#------------------------------------------------------------------------------#
# tor

tor/Makefile: tor/configure.ac tor/Makefile.am
	@which pkg-config || (echo "ERROR: pkg-config is required! apt-get install pkg-config"; exit 1)
	cd tor && ./autogen.sh
	cd tor && \
			./configure \
				--host=$(ALTHOST) \
				--enable-android \
				--enable-lzma \
				--enable-pic \
				--enable-restart-debugging \
				--enable-static-libevent --with-libevent-dir=$(EXTERNAL_ROOT) \
				--enable-static-openssl --with-openssl-dir=$(EXTERNAL_ROOT) \
				--enable-zstd \
				--disable-module-dirauth \
				--prefix=$(EXTERNAL_ROOT)
	grep -E '^# *define +HAVE_LZMA +1$$' tor/orconfig.h
	grep -E '^# *define +HAVE_ZSTD +1$$' tor/orconfig.h
	grep -E '^# *define +ENABLE_OPENSSL +1$$' tor/orconfig.h
	grep -E '^# *define +HAVE_TLS_METHOD +1$$' tor/orconfig.h

tor-build-stamp: tor/Makefile
	$(MAKE) -C tor
	touch $@

tor: lzma-build-stamp zstd-build-stamp libevent-build-stamp openssl-build-stamp tor-build-stamp

tor-clean:
	-rm -f $(OUTPUT_FILE)
	-rm -f tor-build-stamp
	-cd tor && \
		git clean -fdx > /dev/null


#------------------------------------------------------------------------------#
# create and clean libtor.so

PREBUILD_SHARED_LIBRARY: tor
	file $(OUTPUT_FILE) | grep $(GREP_CHECK)
	install -d $(EXTERNAL_ROOT)/lib/$(APP_ABI)
	cp $(OUTPUT_FILE) $(INSTALL_DIR)/libtor.so
	cp openssl/configdata.pm tor/orconfig.h $(INSTALL_DIR)/
ifneq ($(DEBUG), 1)
	ls -l  $(INSTALL_DIR)/libtor.so
	$(NDK_TOOLCHAIN_BASE)/bin/$(ALTHOST)-strip --strip-unneeded -R .note -R .comment --strip-debug \
		$(INSTALL_DIR)/libtor.so
	ls -l  $(INSTALL_DIR)/libtor.so
endif
	@echo "check if all the libs got included:"
	grep "tor_lzma_state_size_precalc" $(INSTALL_DIR)/libtor.so
	install -d $(EXTERNAL_ROOT)/test/$(APP_ABI)
	cp tor/src/test/test tor/src/test/test-memwipe tor/src/test/test-slow \
		$(EXTERNAL_ROOT)/test/$(APP_ABI)

PREBUILD_SHARED_LIBRARY-clean:
	-rm $(INSTALL_DIR)/libtor.so


#------------------------------------------------------------------------------#
# cleanup, cleanup, put the toys away

clean: openssl-clean libevent-clean lzma-clean zstd-clean tor-clean


#------------------------------------------------------------------------------#
# debugging stuff

showsetup:
	@echo "ALL_CFLAGS: $(ALL_CFLAGS)"
	@echo "APP_ABI: $(APP_ABI)"
	@echo "CC: $$CC"
	@echo "CFLAGS: $$CFLAGS"
	@echo "HOST: $(HOST)"
	@echo "NDK_ABI: $(NDK_ABI)"
	@echo "NDK_BIT: $(NDK_BIT)"
	@echo "NDK_PLATFORM_LEVEL: $(NDK_PLATFORM_LEVEL)"
	@echo "NDK_SYSROOT: $(NDK_SYSROOT)"
	@echo "NDK_TOOLCHAIN: $(NDK_TOOLCHAIN)"
	@echo "PATH: $$PATH"
	@echo "REPRODUCIBLE_CFLAGS: $(REPRODUCIBLE_CFLAGS)"
	@echo "SOURCE_DATE_EPOCH: $$SOURCE_DATE_EPOCH"
	@echo "TZ: $$TZ"
