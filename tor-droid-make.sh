#!/usr/bin/env bash

set -e

fetch_submodules()
{
    if [ -n "$1" ]; then
        echo "Cleaning repository"
        git reset --hard
        git clean -fdx
        git submodule foreach git reset --hard
        git submodule foreach git clean -fdx
    fi
    echo "Fetching git submodules"
    git submodule sync
    git submodule foreach git submodule sync
    git submodule update --init --recursive
}

# predictable build paths make reproducible builds easier, so this
# tries to find things at likely standard paths
check_android_dependencies()
{
    if [ -d /opt/android-sdk ]; then
	export ANDROID_HOME=/opt/android-sdk
    elif [ ! -e "$ANDROID_HOME" ]; then
        echo "ANDROID_HOME must be set!"
        exit 1
    fi
    export ANDROID_SDK_ROOT="$ANDROID_HOME"

    # openssl wants a var called ANDROID_NDK_HOME
    if [ ! -e "$ANDROID_NDK_HOME" ]; then
	ndkVersion=$(sed -En 's,NDK_REQUIRED_REVISION *:?= *([0-9.]+).*,\1,p' external/Makefile)
	echo $ANDROID_HOME/ndks/$ndkVersion/source.properties
	if [ -n "$ANDROID_NDK_ROOT" ]; then
	    export ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
	elif [ -e "$ANDROID_HOME/ndks/$ndkVersion/source.properties" ]; then
	    export ANDROID_NDK_HOME="$ANDROID_HOME/ndks/$ndkVersion"
	elif [ -e "$ANDROID_HOME/ndk-bundle/source.properties" ]; then
	    export ANDROID_NDK_HOME="$ANDROID_HOME/ndk-bundle"
	else
            echo "ANDROID_NDK_HOME must be set!"
            exit 1
	fi
	export ANDROID_NDK_ROOT=$ANDROID_NDK_HOME
    fi
    echo "Using Android SDK: $ANDROID_HOME"
    echo "Using Android NDK: $ANDROID_NDK_HOME"
}

build_external_dependencies()
{
    check_android_dependencies
    for abi in $abis; do
	default_abis=`echo $default_abis | sed -E "s,(\s?)$abi(\s?),\1\2,"`
	APP_ABI=$abi make -C external clean
	APP_ABI=$abi make -C external
	binary=external/lib/$abi/libtor.so
	test -e $binary || (echo ERROR $abi missing $binary; exit 1)
    done
    for abi in $default_abis; do
	echo remove dangling symlink: $abi
	rm -f tor-android-binary/src/main/jniLibs/$abi
    done
}

build_app()
{
    echo "Building tor-android"
    build_external_dependencies
    if [ -z $1 ] || [ $1 = 'debug' ]; then
        ./gradlew assembleDebug
    else
        ./gradlew assembleRelease javadocJar sourcesJar
    fi
}

buildinfo()
{
    artifact=$1
    v=$2
    aar=$3
    jv=$(java -XshowSettings:properties -version 2>&1 | sed -En 's,.*java\.version\s+=\s+(.*),\1,p')
    vendor=$(java -XshowSettings:properties -version 2>&1 | sed -En 's,.*java\.vendor\s+=\s+(.*),\1,p')
    buildinfo=$(printf $aar | sed 's,\.aar$,.buildinfo,')
    cat > $buildinfo <<EOF
# https://reproducible-builds.org/docs/jvm/
buildinfo.version=1.0-SNAPSHOT

name=Tor Android
group-id=info.guardianproject
artifact-id=$artifact
version=$v

# source information
source.scm.uri=scm:git:https://github.com/guardianproject/tor-android.git
source.scm.tag=$v
source.used=scm

# build instructions
build-tool=$0 release

# effective build environment information
$(java -XshowSettings:properties -version 2>&1 | sed -En 's,.*(java\.runtime\.version)\s+=\s+(.*),\1=\2,p')
$(java -XshowSettings:properties -version 2>&1 | sed -En 's,.*(java\.version)\s+=\s+(.*),\1=\2,p')
$(java -XshowSettings:properties -version 2>&1 | sed -En 's,.*(java\.specification\.version)\s+=\s+(.*),\1=\2,p')
$(java -XshowSettings:properties -version 2>&1 | sed -En 's,.*(java\.vendor)\s+=\s+(.*),\1=\2,p')
os.name=$(uname)

ndk.version=$(sed -n 's,^Pkg\.Revision\s*=\s*\([^ ]*\),\1,p' $ANDROID_NDK_HOME/source.properties)

outputs.0.filename=$aar
outputs.0.length=$(wc -c <$aar)
outputs.0.checksums.sha526=$(sha256sum $aar | awk '{print $1}')
outputs.0.checksums.sha512=$(sha512sum $aar | awk '{print $1}')
EOF
}

pom()
{
    artifact=$1
    version=$2
    cat > ${artifact}-${version}.pom <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <packaging>aar</packaging>
  <groupId>info.guardianproject</groupId>
  <artifactId>${artifact}</artifactId>
  <version>${version}</version>
  <name>tor-android</name>
  <description>Tor as a native Android Service</description>
  <url>https://gitweb.torproject.org/tor-android</url>
  <inceptionYear>2018</inceptionYear>
  <licenses>
    <license>
      <name>BSD-3-clause</name>
      <url>https://github.com/guardianproject/tor-android/blob/master/LICENSE</url>
      <distribution>repo</distribution>
    </license>
    <license>
      <name>BSD-3-clause</name>
      <url>https://gitweb.torproject.org/tor.git/tree/LICENSE</url>
      <distribution>repo</distribution>
    </license>
    <license>
      <name>BSD-3-clause</name>
      <url>https://github.com/facebook/zstd/blob/dev/LICENSE</url>
      <distribution>repo</distribution>
    </license>
    <license>
      <name>BSD-3-clause</name>
      <url>https://libevent.org/LICENSE.txt</url>
      <distribution>repo</distribution>
    </license>
    <license>
      <name>OpenSSL</name>
      <url>http://www.openssl.org/source/license.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <developers>
    <developer>
      <id>torproject</id>
      <name>Tor Project</name>
      <email>torbrowser@torproject.org</email>
    </developer>
    <developer>
      <id>guardianproject</id>
      <name>Guardian Project</name>
      <email>support@guardianproject.info</email>
    </developer>
  </developers>
  <scm>
    <connection>scm:git:https://github.com/guardianproject/tor-android.git</connection>
    <url>https://github.com/guardianproject/tor-android</url>
  </scm>
  <issueManagement>
    <url>https://github.com/guardianproject/tor-android/issues</url>
    <system>GitHub</system>
  </issueManagement>
</project>
EOF
}

release()
{
    if [ -z "$force" ] && [ -n "$(git status --porcelain)" ]; then
	printf '\nERROR: the git repo must be clean before building:\n\n'
	git status
	exit 1
    fi

    check_android_dependencies

    # tame the build log to fit into GitLab CI's 4MB limit
    export V=0

    fetch_submodules clean
    build_app release
    artifact="tor-android"
    # version must match getVersionName() in tor-android-binary/build.gradle
    version=$(git describe --tags --always)
    aar=${artifact}-${version}.aar
    cd tor-android-binary/build/outputs/aar/
    mv ../../libs/${artifact}-${version}-*.jar ./
    mv *-release.aar $aar
    buildinfo $artifact $version $aar
    pom $artifact $version
    if gpg --list-secret-keys | grep -Eo '[0-9A-F]{40}'; then
	for f in ${artifact}-*.*; do
	    gpg --armor --detach-sign $f
	done
    fi
    jar -cvf bundle.jar ${artifact}-*.*
}

show_options()
{
    echo "usage: ./tor-droid-make.sh command arguments"
    echo ""
    echo "Commands:"
    echo "          fetch   Fetch git submodules"
    echo "          build   Build the project"
    echo ""
    echo "Options:"
    echo "          -a      ABI(s) to build (default: \"$default_abis\")"
    echo "          -b      Build type, it can be release or debug (default: debug)"
    echo "          -c      Clean the repository (Used together with the fetch command)"
    echo "          -f      Force clean all (Used together with the release command)"
    echo ""
    exit
}

option=$1
default_abis="arm64-v8a armeabi-v7a x86 x86_64"
abis=$default_abis
build_type="debug"

if [ -z $option ]; then
    show_options
fi
shift

while getopts 'a:b:cf' opts; do
    case $opts in
        a) abis=${OPTARG:-$abis} ;;
        b) build_type=${OPTARG:-$build_type} ;;
        c) clean=clean ;;
        f) force=force ;;
    esac
done

case "$option" in
    "fetch") fetch_submodules $clean ;;
    "build") build_app $build_type ;;
    "release") release ;;
    *) show_options ;;
esac
