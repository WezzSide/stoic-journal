## Release artefacts — integration/mentor-into-main (commit 489cd78b84c9f7fc4699bfb98f0b35b968ad2bee)

- app-release.apk: 3877349 bytes, SHA-256 1bd263944c72ce83a2780a3c5027935d97061eb585c77d15eccb2985afa154e9
- app-release.aab: 7383513 bytes, SHA-256 3199cb0bef2a05dbcb8cf9765ab205b9ee1ce4ae46dacc1473fe84fca486fb75

### aapt2 badging (APK)
    package: name='com.stoicjournal' versionCode='1' versionName='1.0' platformBuildVersionName='14' platformBuildVersionCode='34' compileSdkVersion='34' compileSdkVersionCodename='14'
    targetSdkVersion:'34'
    uses-permission: name='android.permission.INTERNET'
    uses-permission: name='android.permission.POST_NOTIFICATIONS'
    uses-permission: name='android.permission.RECEIVE_BOOT_COMPLETED'
    uses-permission: name='android.permission.USE_BIOMETRIC'
    uses-permission: name='android.permission.USE_FINGERPRINT'
    uses-permission: name='android.permission.WAKE_LOCK'
    uses-permission: name='android.permission.ACCESS_NETWORK_STATE'
    uses-permission: name='android.permission.FOREGROUND_SERVICE'
    uses-permission: name='com.stoicjournal.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
    launchable-activity: name='com.stoicjournal.MainActivity'  label='' icon=''

### apksigner verify (APK)
    Verifies
    Verified using v1 scheme (JAR signing): false
    Verified using v2 scheme (APK Signature Scheme v2): true
    Verified using v3 scheme (APK Signature Scheme v3): false
    Verified using v3.1 scheme (APK Signature Scheme v3.1): false
    Verified using v4 scheme (APK Signature Scheme v4): false
    Verified for SourceStamp: false
    Number of signers: 1
    Signer #1 certificate DN: CN=StoicJournal, O=Stoic, C=NL
    Signer #1 certificate SHA-256 digest: 7cf08be259f0b2bb1d81e529b7c23ae7a9287e3d3f49db6f494bb313e08d6cb1
    Signer #1 certificate SHA-1 digest: 01e701636c2be45d39a318830cc1bf0195bd41f1
    Signer #1 certificate MD5 digest: 14001f7c365ba243d684c90d3b76766a

### AAB contents check
    BundleConfig.pb
    base/manifest/AndroidManifest.xml
    META-INF/STOICJOU.SF
    META-INF/STOICJOU.RSA

### sensitive files bundled?
    none found

### debuggable / test strings in classes
    matches (HiltTestActivity|fake_provider): 1
