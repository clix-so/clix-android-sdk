# Clix Basic Sample App

This is a basic sample application demonstrating the integration of the Clix Android SDK.

## Setup

### 1. Configure Clix

Before running the app, you need to configure your Clix credentials:

1. Copy the example configuration file:
   ```bash
   cp src/main/assets/ClixConfig.json.example src/main/assets/ClixConfig.json
   ```

2. Edit `src/main/assets/ClixConfig.json` with your actual Clix credentials:
   ```json
   {
     "projectId": "YOUR_PROJECT_ID",
     "apiKey": "YOUR_API_KEY",
     "endpoint": "https://external-api-dev.clix.so",
     "extraHeaders": {
       "Cf-Access-Token": "YOUR_CF_ACCESS_TOKEN"
     }
   }
   ```

### 2. Configuration Fields

- **projectId**: Your Clix project ID
- **apiKey**: Your Clix API key
- **endpoint**: The Clix API endpoint (default: `https://external-api-dev.clix.so`)
- **extraHeaders**: Additional HTTP headers to include in API requests
  - **Cf-Access-Token**: Cloudflare Access token for authentication

### 3. Security Notice

⚠️ **Important**: The `ClixConfig.json` file contains sensitive credentials and is excluded from version control via `.gitignore`.

- **Never commit** the actual `ClixConfig.json` file to the repository
- Only the `ClixConfig.json.example` file should be committed
- Keep your credentials secure and do not share them publicly

### 4. Firebase Configuration

This sample app also requires Firebase configuration:

1. Download your `google-services.json` from the Firebase Console
2. Place it in the `samples/basic-app/` directory

Note: `google-services.json` is also excluded from version control.

## Building and Running

Once configured, you can build and run the app:

```bash
./gradlew :samples:basic-app:installDebug
```

Or open the project in Android Studio and run the `basic-app` module.

## Configuration Loading

The app loads configuration at startup via `BasicApplication.onCreate()`:

```kotlin
ClixConfiguration.initialize(this)
```

The configuration is loaded from the `ClixConfig.json` file in the assets directory. If the file is missing or invalid, the app will throw a `ClixConfigException` with a descriptive error message.

## Troubleshooting

### App crashes on startup with "Failed to read config file"

- Make sure you've created the `ClixConfig.json` file by copying `ClixConfig.json.example`
- Verify the file is located at `src/main/assets/ClixConfig.json`

### App crashes with "Failed to parse config file"

- Check that your JSON syntax is valid
- Ensure all required fields are present: `projectId`, `apiKey`, `endpoint`, `extraHeaders`

### Build errors related to serialization

- Make sure you've synced Gradle after modifying `build.gradle.kts`
- Clean and rebuild: `./gradlew clean build`
