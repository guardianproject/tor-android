
## Tor Android
This is a project forked from Orbot for building the tor binary for Android

## How to Build

Please see: https://raw.githubusercontent.com/guardianproject/tor-android/master/BUILD

## How to Use via Gradle

Add the repository your list as shown:

```gradle
repositories {
        maven { url "https://raw.githubusercontent.com/guardianproject/gpmaven/master" }
    }
    
```

and then add the dependency, setting it to the latest version (or any version) we have made available, as a release:

```gradle
dependencies {
    implementation 'org.torproject:tor-android-binary:0.4.4.6'
}
```


Tor protects your privacy on the internet by hiding the connection 
between your Internet address and the services you use. We believe Tor
is reasonably secure, but please ensure you read the instructions and
configure it properly. Learn more at https://torproject.org/

## Tor Frequently Asked Questions:
        
- https://2019.www.torproject.org/docs/faq
- https://support.torproject.org/faq/

