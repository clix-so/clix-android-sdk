# Clix Android SDK

Clix Android SDK is a powerful tool for managing push notifications and user events in your Android application. It
provides a simple and intuitive interface for user engagement and analytics.

## Installation

### Gradle (Kotlin DSL)

Add the following to your project's `settings.gradle.kts` or `build.gradle.kts`:

```kotlin
repositories {
  mavenCentral()
}
```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
  implementation("so.clix:clix-android-sdk:1.5.0")
}
```

### 2. Firebase Setup

1. Create or select a project in the Firebase Console.
2. Register your app with the Firebase project.
3. Download the google-services.json file and add it to your app module directory.

## Requirements

- Android API level 26 (Android 8.0) or later
- Firebase Cloud Messaging

## Usage

### Initialization

Initialize the SDK with a ClixConfig object. The config is required and contains your project settings.

```kotlin
import so.clix.core.Clix
import so.clix.core.ClixConfig
import so.clix.utils.logging.ClixLogLevel

class MyApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    Clix.initialize(
      context = this,
      config = ClixConfig(
        projectId = "YOUR_PROJECT_ID",
        apiKey = "YOUR_API_KEY",
        endpoint = "https://api.clix.so", // Optional: default is https://api.clix.so
        logLevel = ClixLogLevel.INFO       // Optional: default is INFO
      )
    )
  }
}
```

### User Management

```kotlin
import kotlinx.coroutines.launch

// Set user ID
lifecycleScope.launch {
  Clix.setUserId("user123")
}

// Set user properties
lifecycleScope.launch {
  Clix.setUserProperty("name", "John Doe")
  Clix.setUserProperties(
    mapOf(
      "age" to 25,
      "premium" to true
    )
  )
}

// Remove user properties
lifecycleScope.launch {
  Clix.removeUserProperty("name")
  Clix.removeUserProperties(listOf("age", "premium"))
}
```

### Reset

Use `reset()` when you need a completely fresh device identity (e.g., shared device scenarios). This generates a new device ID, removes the user ID, and clears session data.

```kotlin
lifecycleScope.launch {
  Clix.reset()
}
```

> **Note:** After calling `reset()`, you must call `initialize()` again before using the SDK.

### Event Tracking

```kotlin
import kotlinx.coroutines.launch

// Track an event with properties
lifecycleScope.launch {
  Clix.trackEvent(
    "signup_completed",
    mapOf(
      "method" to "email",
      "discount_applied" to true,
      "trial_days" to 14,
      "completed_at" to Instant.now(),
    )
  )
}
```

### Device Information

```kotlin
// Get device ID
val deviceId = Clix.getDeviceId()

// Get push token
val pushToken = Clix.Notification.getToken()
```

### Logging

```kotlin
Clix.setLogLevel(ClixLogLevel.DEBUG)
// Available log levels:
// - NONE: Disable logging
// - ERROR: Log errors only
// - WARN: Log warnings and errors
// - INFO: Log info, warnings, and errors
// - DEBUG: Log all
```

### Push Notification Integration

#### 1. Configure Notification Handling

Configure notification handling in your `Application` class **after** initializing the SDK:

```kotlin
import so.clix.core.Clix
import so.clix.core.ClixConfig
import so.clix.utils.logging.ClixLogLevel

class MyApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    // STEP 1: Initialize Clix SDK first
    Clix.initialize(
      context = this,
      config = ClixConfig(
        projectId = "YOUR_PROJECT_ID",
        apiKey = "YOUR_API_KEY"
      )
    )

    // STEP 2: Configure notification handling after initialization
    // Note: autoRequestPermission defaults to false
    Clix.Notification.configure(
      autoRequestPermission = true,  // Set to true to automatically request permission
      autoHandleLandingURL = true     // Set to true to automatically open landing URLs
    )

    // STEP 3: Optional callbacks (must be called after initialize)
    Clix.Notification.onMessage { notificationData ->
      // Return true to display the notification, false to suppress it
      true
    }

    Clix.Notification.onNotificationOpened { notificationData ->
      // Custom routing (called when user taps notification)
      val landingURL = (notificationData["clix"] as? Map<*, *>)?.get("landing_url") as? String
      if (landingURL != null) {
        // Handle custom routing
      }
    }

    Clix.Notification.onFcmTokenError { error ->
      Log.e("MyApp", "FCM token error: ${error.message}", error)
      // Handle token registration failures (e.g., Firebase config issues, network errors)
    }
  }
}
```

**Important:** All `Clix.Notification` methods must be called **after** `Clix.initialize()`. Calling them before
initialization will result in an error.

##### About `notificationData`

- The `notificationData` map is the full FCM payload as delivered to the device; it mirrors iOS’s `userInfo` dictionary.
- Every Clix notification callback (`onMessage`, `onBackgroundMessage`, `onNotificationOpened`) passes this map through
  untouched, so you can inspect both the serialized `"clix"` block and any custom keys your backend adds.
- `notificationData["clix"]` holds the Clix metadata JSON, while all other keys represent app-specific data.

Or request permission manually:

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    lifecycleScope.launch {
      val granted = Clix.Notification.requestPermission()
      if (granted) {
        // Permission granted
      }
    }
  }
}
```

#### 2. Create Messaging Service

Inherit from `ClixMessagingService`:

```kotlin
import com.google.firebase.messaging.RemoteMessage
import so.clix.notification.ClixMessagingService

class MyMessagingService : ClixMessagingService() {
  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    // Custom notification handling
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    // Custom token handling
  }
}
```

**Features:**

- Automatic device token registration and updates
- Push notification event tracking
- Duplicate notification prevention
- Deep linking support (automatic landing URL handling)
- Use `Clix.Notification.configure(autoHandleLandingURL = false)` to disable automatic landing URL handling

#### Deep Link Handling

The SDK automatically handles landing URLs in push notifications. When a user taps a notification:

1. **Automatic handling (default)**: The SDK opens the `landing_url` from the notification payload
2. **Custom handling**: Disable automatic handling and implement your own routing:

```kotlin
// Disable automatic handling
Clix.Notification.configure(
  autoRequestPermission = false,
  autoHandleLandingURL = false
)

// Handle custom routing
Clix.Notification.onNotificationOpened { notificationData ->
  val clixData = notificationData["clix"] as? Map<*, *>
  val landingURL = clixData?.get("landing_url") as? String

  if (landingURL != null) {
    // Parse and route to specific app screen
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(landingURL))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
  }
}
```

**Payload structure:**

```json
{
  "clix": {
    "message_id": "msg_123",
    "landing_url": "myapp://screen/detail?id=123",
    "campaign_id": "campaign_456"
  }
}
```

#### Clix.Notification API reference

- `configure(autoRequestPermission:autoHandleLandingURL:)`: Configure push notification handling
- `onMessage(handler:)`: Register handler for foreground messages
- `onBackgroundMessage(handler:)`: Register handler for background messages
- `onNotificationOpened(handler:)`: Register handler for notification taps
- `onFcmTokenError(handler:)`: Register handler for FCM token errors
- `requestPermission()`: Request notification permissions
- `getToken()`: Get current FCM token
- `deleteToken()`: Delete FCM token
- `getPermissionStatus()`: Get current permission status
- `setPermissionGranted(isGranted:)`: Update permission status on server

## Error Handling

All SDK operations can throw exceptions. Always handle potential errors:

```kotlin
try {
  Clix.setUserId("user123")
} catch (e: Exception) {
  Log.e("Clix", "Failed to set user ID", e)
}
```

## Thread Safety

The SDK is thread-safe and all operations can be called from any thread. Coroutine-based operations will automatically
wait for SDK initialization to complete.

## Troubleshooting

### Push Permission Status Not Updating

The `autoRequestPermission` parameter defaults to **`false`**. If you're not using automatic permission requests, you
must manually notify Clix when users grant or deny push permissions.

#### Update Permission Status

When using `autoRequestPermission = false` (the default), call `Clix.Notification.setPermissionGranted()` after
requesting push permissions in your app:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
  requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
}

// In your permission result callback
override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
  if (requestCode == PERMISSION_REQUEST_CODE) {
    val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

    // ✅ Notify Clix SDK about permission status
    lifecycleScope.launch {
      Clix.Notification.setPermissionGranted(granted)
    }
  }
}
```

### FCM Token Errors

If you're experiencing FCM token registration failures, use the error handler to diagnose:

```kotlin
Clix.Notification.onFcmTokenError { error ->
  Log.e("FCM", "Token error: ${error.message}", error)
  // Common causes:
  // - Missing or invalid google-services.json
  // - Network connectivity issues
  // - Firebase service errors
  // - Server-side token registration failures
}
```

Common FCM token errors:

- **"SERVICE_NOT_AVAILABLE"**: Network issues or Firebase service down
- **"INVALID_SENDER"**: Incorrect Firebase configuration (check google-services.json)
- **Token registration failure**: Backend API errors when saving token

### Debugging Checklist

If push notifications aren't working, verify:

1. ✅ `google-services.json` is added to your app module
2. ✅ Firebase Cloud Messaging is properly configured
3. ✅ `ClixMessagingService` is declared in `AndroidManifest.xml`
4. ✅ `Clix.Notification.setPermissionGranted()` is called after requesting permissions (when not using auto-request)
5. ✅ Testing on a real device or emulator with Google Play Services
6. ✅ Debug logs show "New token received" message
7. ✅ Use `onFcmTokenError()` handler to catch token registration errors

### Getting Help

If you continue to experience issues:

1. Enable debug logging (`ClixLogLevel.DEBUG`)
2. Check Logcat for Clix log messages
3. Verify your device appears in the Clix console Users page
4. Check if `push_token` field is populated for your device
5. Create an issue on [GitHub](https://github.com/clix-so/clix-android-sdk/issues) with logs and configuration details

## Proguard

If you are using Proguard, the following rules are applied automatically:

```proguard
-keep class so.clix.** { *; }
-keep class com.google.firebase.** { *; }
```

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Changelog

See the full release history and changes in the [CHANGELOG.md](CHANGELOG.md) file.

## Contributing

We welcome contributions! Please read the [CONTRIBUTING.md](CONTRIBUTING.md) guide before submitting issues or pull
requests.
