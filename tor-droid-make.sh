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

check_android_dependencies()
{
    if [ -z $ANDROID_HOME ]; then
        echo "ANDROID_HOME must be set!"
        exit
    fi

    if [ -z $ANDROID_NDK_HOME ]; then
        echo "ANDROID_NDK_HOME not set and 'ndk-build' not in PATH"
        exit
    fi
}

build_external_dependencies()
{
    check_android_dependencies
    for abi in arm64-v8a armeabi-v7a x86 x86_64; do
	APP_ABI=$abi make -C external clean
	APP_ABI=$abi make -C external
	binary=external/lib/$abi/libtor.so
	test -e $binary || (echo ERROR $abi missing $binary; exit 1)
    done
}

build_app()
{
    echo "Building tor-android"
    build_external_dependencies
    if [ -z $1 ] || [ $1 = 'debug' ]; then
        ./gradlew assembleDebug
    else
        ./gradlew assembleRelease
    fi
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
    echo "          -b      Build type, it can be release or debug (default: debug)"
    echo "          -c      Clean the repository (Used together with the fetch command)"
    echo "          -n      Project name (default: tor-android-binary)"
    echo "          -t      Project target (default: android-23)"
    echo ""
    exit
}

option=$1
build_type="debug"

if [ -z $option ]; then
    show_options
fi
shift

while getopts 'c:b:n:t' opts; do
    case $opts in
        c) clean=clean ;;
        b) build_type=${OPTARG:-$build_type} ;;
    esac
done

case "$option" in
    "fetch") fetch_submodules $clean ;;
    "build") build_app $build_type ;;
    *) show_options ;;
esac
