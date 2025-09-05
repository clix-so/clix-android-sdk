# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2025-08-28

### Fixed

- **Core SDK**
  - Update a default value of Device.platform to sync with an updated IDL definition

## [1.0.0] - 2025-06-01

### Added

- **Core SDK**
  - ClixConfig-based initialization with projectId, apiKey, endpoint configuration
  - Coroutine-based asynchronous operations
  - Thread-safe singleton implementation with automatic initialization handling
  - Kotlin 1.9.25 with full coroutines support

- **User Management**
  - User identification: `setUserId()`
  - User properties: `setUserProperty()`, `setUserProperties()`
  - Persistent storage of user data using SharedPreferences

- **Push Notifications**
  - Firebase Cloud Messaging (FCM) integration
  - ClixMessagingService for automated push token management
  - Rich notification support with images
  - Deep linking support via custom landing URLs
  - Android 13+ POST_NOTIFICATIONS permission handling
  - Automatic device token registration and updates

- **Device & Logging**
  - Automatic device information collection (model, OS version, locale, timezone)
  - Configurable logging system with 5 levels (NONE, ERROR, WARN, INFO, DEBUG)
  - Comprehensive error handling with ClixError types
