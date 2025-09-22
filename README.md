# Clix Android SDK

Clix Android SDK is a powerful tool for managing push notifications and user events in your Android application. It provides a simple and intuitive interface for user engagement and analytics.

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
    implementation("so.clix:clix-android-sdk:1.1.0")
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

You can initialize the SDK in your `Application` class. The `endpoint` and `logLevel` parameters are optional.

```kotlin
import so.clix.Clix
import so.clix.ClixConfig
import so.clix.ClixLogLevel

class MyApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    lifecycleScope.launch {
      try {
        Clix.initialize(
          ClixConfig(
            apiKey = "YOUR_API_KEY",
            endpoint = "https://api.clix.so", // Optional: default is https://api.clix.so
            logLevel = ClixLogLevel.INFO      // Optional: default is INFO
          )
        )
      } catch (e: Exception) {
        // Handle initialization failure
      }
    }
  }
}
```

### User Management

```kotlin
// Set user ID
Clix.setUserId("user123")

// Set user properties
Clix.setUserProperties(
  mapOf(
    "name" to "John Doe",
    "email" to "john@example.com",
    "age" to 25,
    "premium" to true
  )
)

// Remove a property
Clix.removeUserProperty("name")

// Remove user ID
Clix.removeUserId()
```

### Event Tracking

```kotlin
// Track an event with properties
Clix.trackEvent(
  "button_clicked",
  mapOf(
    "button_id" to "login_button",
    "screen" to "login"
  )
)
```

### Reset SDK State

```kotlin
Clix.reset()
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

Clix SDK supports push notification integration via `ClixMessagingService`.

#### Using ClixMessagingService

This approach automates push notification handling, device token management, and event tracking.

1. **Enable Push Notifications in your project**

- Configure Firebase Cloud Messaging and add `google-services.json` to your app module.

2. **Inherit from ClixMessagingService in your service**

```kotlin
import com.google.firebase.messaging.RemoteMessage
import so.clix.ClixMessagingService

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

- Permission requests, device token registration, and event tracking are handled automatically.
- You can override: `onMessageReceived`, `onNewToken`.
- Always call super to retain default SDK behavior.

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

We welcome contributions! Please read the [CONTRIBUTING.md](CONTRIBUTING.md) guide before submitting issues or pull requests.
