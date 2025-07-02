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
# user building this will have to manually set NDK_PROCESSOR.
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
NDK_REVISION := $(shell sed -n 's,^Pkg.Revision *= *\([^ ]*\),\1,p' $(ANDROID_NDK_HOME)/source.properties)
NDK_REQUIRED_REVISION := 25.2.9519653
MIN_NDK_VERSION := 24
NDK_PLATFORM_LEVEL := 24
ifeq ($(shell test $(MIN_NDK_VERSION) -gt $(firstword $(subst ., ,$(NDK_REVISION))); echo $$?),0)
 $(error NDK r$(MIN_NDK_VERSION) or newer required! r$(NDK_REVISION) is installed at $(ANDROID_NDK_HOME).)
endif
ifneq ($(NDK_REQUIRED_REVISION), $(NDK_REVISION))
 $(warning WARNING: NDK $(NDK_REQUIRED_REVISION) required for reproducible builds, $(NDK_REVISION) is installed.)
endif

ifneq ($(filter arm64-v8a, $(APP_ABI)),)
 HOST := aarch64-linux-android
 ALTHOST := $(HOST)
 GREP_CHECK := aarch64
 NDK_ABI := arm64
 NDK_BIT := 64
endif
ifneq ($(filter armeabi-v7a, $(APP_ABI)),)
 HOST := armv7a-linux-androideabi
 ALTHOST := arm-linux-androideabi
 GREP_CHECK := EABI5
 NDK_ABI := arm
 NDK_BIT := 32
endif
ifneq ($(filter x86, $(APP_ABI)),)
 HOST := i686-linux-android
 ALTHOST := $(HOST)
 GREP_CHECK := 80386
 NDK_ABI := x86
 NDK_BIT := 32
endif
ifneq ($(filter x86_64, $(APP_ABI)),)
 HOST := x86_64-linux-android
 ALTHOST := $(HOST)
 GREP_CHECK := x86-64
 NDK_ABI := x86_64
 NDK_BIT := 64
endif

NDK_UNAME := $(shell uname -s | tr '[A-Z]' '[a-z]')
NDK_TOOLCHAIN_BASE=$(ANDROID_NDK_HOME)/toolchains/llvm/prebuilt/$(NDK_UNAME)-$(NDK_PROCESSOR)

export CC := $(NDK_TOOLCHAIN_BASE)/bin/$(HOST)$(NDK_PLATFORM_LEVEL)-clang


export TZ := UTC
export LC_ALL := C.UTF-8
export SOURCE_DATE_EPOCH := 1234567890

REPRODUCIBLE_CFLAGS := \
  -fbuild-session-timestamp=$(SOURCE_DATE_EPOCH) \
  -fdebug-compilation-dir . \
  -no-canonical-prefixes \


ALL_CFLAGS := $(REPRODUCIBLE_CFLAGS) $(CFLAGS) -Os
export CFLAGS := $(ALL_CFLAGS)


OUTPUT_FILE := $(EXTERNAL_ROOT)/tor/src/app/tor
INSTALL_DIR := $(EXTERNAL_ROOT)/lib/$(APP_ABI)

.PHONY = clean showsetup \
	PREBUILD_SHARED_LIBRARY PREBUILD_SHARED_LIBRARY-clean \
	zlib-clean \
	openssl-clean \
	libevent-clean \
	zstd-clean \
	tor tor-clean

all: test-setup PREBUILD_SHARED_LIBRARY

test-setup:
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

# openssl3.0.x looks for NDK with the word root instead of home in the var name
export ANDROID_NDK_ROOT=$(ANDROID_NDK_HOME)

openssl/Makefile: openssl/Configure $(wildcard openssl/Configurations/*.*)
	cd openssl && PATH=$(PATH) && CFLAGS="$(CFLAGS) -Wno-macro-redefined" \
		./Configure \
			no-comp no-dtls no-ec2m no-psk no-srp no-ssl3 \
			no-camellia no-idea no-md2 no-md4 no-mdc2 no-rc2 no-rc4 no-rc5 no-rmd160 no-whirlpool \
			no-dso no-padlockeng no-ui-console \
			no-shared no-tests \
			android-$(NDK_ABI) \
			-D__ANDROID_API__=$(NDK_PLATFORM_LEVEL) \
			--prefix=/ \
			--openssldir=/

openssl-build-stamp: openssl/Makefile
ifeq ($(V), 0)
	PATH=$(PATH) make -j`nproc` --silent -C openssl install_dev DESTDIR=$(EXTERNAL_ROOT) ECHO=':'
else
	PATH=$(PATH) make -j`nproc` -C openssl install_dev DESTDIR=$(EXTERNAL_ROOT)
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
				LDFLAGS="-L../lib" \
				CFLAGS="-I../include" \
				--host=$(ALTHOST) \
				--disable-libevent-regress \
				--disable-samples \
				--disable-openssl \
				--disable-shared \
			        --prefix=/

libevent-build-stamp: openssl-build-stamp libevent/Makefile
	$(MAKE) -C libevent install DESTDIR=$(EXTERNAL_ROOT)
	touch $@

libevent-clean:
	-rm -f lib/libevent.a
	-rm -f libevent-build-stamp
	-$(MAKE) -C libevent uninstall DESTDIR=$(EXTERNAL_ROOT)
	-$(MAKE) -C libevent clean
	-cd libevent && \
		git clean -fdx > /dev/null


#------------------------------------------------------------------------------#
# zlib

zlib-build-stamp:
	cd zlib && ./configure \
		--static \
		--prefix=$(EXTERNAL_ROOT)
	$(MAKE) -C zlib install
	touch $@

zlib-clean:
	-$(MAKE) -C zlib uninstall
	-$(MAKE) -C zlib clean
	-rm -f lib/libz.a
	-rm -f lib/pkgconfig/zlib.pc
	-rm -f include/zconf.h
	-rm -f include/zlib.h
	-rm -f zlib-build-stamp
	-rm -f share/man/man3/zlib.3
	-cd zlib && \
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
	cp zstd/lib/deprecated/zbuff.h include
	cp zstd/lib/zstd_errors.h include
	cp zstd/lib/zdict.h include
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
				--enable-gpl \
				--enable-pic \
				--enable-static-libevent --with-libevent-dir=$(EXTERNAL_ROOT) \
				--enable-static-openssl --with-openssl-dir=$(EXTERNAL_ROOT) \
				--enable-static-zlib --with-zlib-dir=$(EXTERNAL_ROOT) \
				--enable-zstd \
				--disable-lzma \
				--disable-module-relay \
				--disable-module-dirauth \
				--disable-asciidoc \
				--disable-tool-name-check \
				--disable-manpage \
				--disable-html-manual \
				--disable-system-torrc \
				--prefix=$(EXTERNAL_ROOT)
	grep -E '^# *define +HAVE_ZSTD +1$$' tor/orconfig.h
	grep -E '^# *define +ENABLE_OPENSSL +1$$' tor/orconfig.h
	grep -E '^# *define +HAVE_TLS_METHOD +1$$' tor/orconfig.h

tor-build-stamp: tor/Makefile
	$(MAKE) -C tor
	touch $@

tor: zlib-build-stamp zstd-build-stamp libevent-build-stamp openssl-build-stamp tor-build-stamp

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
	$(NDK_TOOLCHAIN_BASE)/bin/llvm-strip --strip-unneeded -R .note -R .comment --strip-debug \
		$(INSTALL_DIR)/libtor.so
	$(EXTERNAL_ROOT)/bin/termux-elf-cleaner --api-level $(NDK_PLATFORM_LEVEL) $(INSTALL_DIR)/libtor.so
	ls -l  $(INSTALL_DIR)/libtor.so
endif
	install -d $(EXTERNAL_ROOT)/test/$(APP_ABI)
	cp tor/src/test/test tor/src/test/test-memwipe tor/src/test/test-slow \
		$(EXTERNAL_ROOT)/test/$(APP_ABI)

PREBUILD_SHARED_LIBRARY-clean:
	-rm $(INSTALL_DIR)/libtor.so


#------------------------------------------------------------------------------#
# cleanup, cleanup, put the toys away

clean: zlib-clean openssl-clean libevent-clean zstd-clean tor-clean


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
	@echo "PATH: $$PATH"
	@echo "REPRODUCIBLE_CFLAGS: $(REPRODUCIBLE_CFLAGS)"
	@echo "SOURCE_DATE_EPOCH: $$SOURCE_DATE_EPOCH"
	@echo "TZ: $$TZ"
