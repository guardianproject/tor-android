
## Tor Android

This is native Android `TorService` built on the Tor shared library built for
Android.  The included _libtor.so_ binaries can also be used directly as a tor
daemon.  Binaries are available on Maven Central:

```gradle
dependencies {
    implementation 'info.guardianproject:tor-android:0.4.5.9'
}
```

Tor protects your privacy on the internet by hiding the connection 
between your Internet address and the services you use. We believe Tor
is reasonably secure, but please ensure you read the instructions and
configure it properly. Learn more at https://torproject.org/

## Tor Frequently Asked Questions:
        
- https://2019.www.torproject.org/docs/faq
- https://support.torproject.org/faq/


## How to Build

Please see: https://raw.githubusercontent.com/guardianproject/tor-android/master/BUILD

This can be built reproducibly using the included Vagrant VM setup.  That will
run with either _libvirt_ or VirtualBox.  The provisioning is based on the
"release" job in _.gitlab-ci.yml_.
