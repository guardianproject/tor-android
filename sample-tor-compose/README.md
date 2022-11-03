# ABOUT:
I spend lot of time trying to implement Tor Network inside an android app and finally I got it working. I thought this might help someone else out there.

So this project uses kotlin instead of java and also using the emerging compose jetpack.

## What is this app?
This app is a proof of concept for Tor Network in android app in a simple web browser app.

## How does it work?
By clicking the Connect button, the app will start and bind TorService.

After the Tor Service started, we can start browsing with Tor Network with entering the URL inside the textfield. Hitting the reload button will then trigger state change and update the webView which uses GenericWebViewClient to intercept the connection and use Tor instead.

# IDE: 
- Android Studio

# LANGUAGE:
- Kotlin

# STEP:
- create new project with compose activity template

- add dependency to gradle

    implementation 'info.guardianproject:tor-android:0.4.7.8'
    implementation 'info.guardianproject:jtorctl:0.4.5.7'
    implementation 'androidx.localbroadcastmanager:localbroadcastmanager:1.1.0'

- add permission to manifest

    <uses-permission android:name="android.permission.INTERNET" />

- copy GenericWebViewClient from [link](https://github.com/guardianproject/tor-android/blob/74a98cc7bc0d12d6a38509020486df667c556e9f/sampletorapp/src/main/java/org/torproject/android/sample/GenericWebViewClient.java) to app/src/main/java/torcompose/com/ then modify to use kotlin and update package name

- edit MainActivity.kt

# SCREENSHOTS:
- ![image](https://user-images.githubusercontent.com/20583849/199776823-05769bce-1006-4c46-8b27-0b731c278e74.png)

- ![image](https://user-images.githubusercontent.com/20583849/199776636-cca1a5f0-c3aa-4043-90d4-1fd1e912747c.png)
