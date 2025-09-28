# SMTShell-API

SMTShell-API is an Android library that provides methods for executing shell commands and loading shared objects **as root user** on rooted Android devices. This library has been updated to use direct root access instead of relying on external services.

## Important Changes

**⚠️ Breaking Change:** This version now uses direct root access instead of Samsung's SMTShell service. Your device must be rooted for this library to work.

## Including this in your project

**project build.gradle**
```gradle
allprojects {
    repositories {
        // other repos here
        maven { url "https://jitpack.io" }  // <--- add this
    }
}
```

**app/module build.gradle**
```gradle
dependencies {
    implementation 'com.github.Eduardob3677:SMTShell-API:2.0'
}
```

## Get permissions

Declare these permissions in your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.ACCESS_SUPERUSER" />
<uses-permission android:name="android.permission.REBOOT" />

<!-- Legacy permissions for backward compatibility -->
<uses-permission android:name="smtshell.permission.SYSTEM_COMMAND" />
<uses-permission android:name="smtshell.permission.LOAD_LIBRARY" />
```

## API Methods

### Check Root Access

Before using any other methods, check if root access is available:

```java
if (SMTShellAPI.isRootAvailable()) {
    // Device is rooted and root access is available
} else {
    // Device is not rooted or root access is denied
}
```

### Request Root Access

Request root permissions from the user (this will show a SuperSU/Magisk prompt):

```java
SMTShellAPI.requestRootAccess(context, new SMTShellAPI.RootAccessCallback() {
    @Override
    public void onResult(boolean granted) {
        if (granted) {
            // Root access was granted
        } else {
            // Root access was denied
        }
    }
});
```

### executeCommand

`executeCommand(Context context, String cmd)`
`executeCommand(Context context, String cmd, CommandCallback cb)`

Execute shell commands as root:

```java
SMTShellAPI.executeCommand(context, "ls -la /data/data/", new SMTShellAPI.CommandCallback() {
    @Override
    public void onComplete(String stdout, String stderr, int exitCode) {
        Log.d(TAG, "stdout: " + stdout);
        Log.d(TAG, "stderr: " + stderr);
        Log.d(TAG, "exit code: " + exitCode);
    }
});
```

### loadLibrary

`loadLibrary(Context context, String path)`
`loadLibrary(Context context, String path, LoadLibraryCallback cb)`

Load a shared object as root:

```java
SMTShellAPI.loadLibrary(this, getApplicationInfo().nativeLibraryDir + "/" + "libexample.so", new SMTShellAPI.LoadLibraryCallback() {
    @Override
    public void onComplete(boolean success) {
        if (success) {
            // library loaded!
        } else {
            // something went wrong!
        }
    }
});
```

### ping

`ping(Context context)`

Check if the API is ready. Now just broadcasts the API_READY intent for compatibility:

```java
BroadcastReceiver mApiReadyReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        // API is ready!
    }
};

@Override
protected void onResume() {
    super.onResume();
    registerReceiver(mApiReadyReceiver, new IntentFilter(SMTShellAPI.ACTION_API_READY));
    SMTShellAPI.ping(this);
}

@Override
protected void onPause() {
    super.onPause();
    unregisterReceiver(mApiReadyReceiver);
}
```

## Requirements

- **Rooted Android device** (Android 7.0+ recommended)
- **SuperSU**, **Magisk**, or other root management solution
- Device must grant superuser permissions to your app

## Migration from v1.x

If you're upgrading from the Samsung SMTShell-dependent version:

1. Update your dependency to the new version
2. Add root permissions to your manifest
3. Add root access checks before calling API methods
4. Test on rooted devices instead of Samsung devices with SMTShell

## Sample App

A sample application demonstrating the usage is included in the `app` module. Build and install it to see the API in action.

## Building

To build the library and sample app:

```bash
./gradlew build
./gradlew assembleDebug  # For debug APK
./gradlew assembleRelease  # For release APK
```
