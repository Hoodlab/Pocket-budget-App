# Supabase Multiplatform Runtime Configuration

Pocket Budget connects to Supabase across Android, Desktop (JVM), and iOS using environment-driven configuration without modifying Kotlin source files.

---

## 1. Supported Environments

The application operates in one of three explicit environments:

| Environment | Purpose | URL Scheme | Description |
| :--- | :--- | :--- | :--- |
| `LOCAL` | Local developer stack | `http://` or `https://` | Connects to local Supabase running via Docker CLI (default: `http://127.0.0.1:54321` on Desktop/iOS, `http://10.0.2.2:54321` on Android Emulator). |
| `HOSTED_DEV` | Remote development project | `https://` (Strict) | Connects to the team hosted development Supabase project (e.g. `https://<PROJECT_REF>.supabase.co`). |
| `UNCONFIGURED` | Offline-only mode | None | Default fallback when configuration is missing, incomplete, or invalid. All local Room budgeting features remain 100% operational. Sync is disabled. |

### Resolution Rules & Fail-Safe Behavior
- If any required value (`environment`, `url`, `publishableKey`) is missing or blank, the app cleanly resolves to `UNCONFIGURED`.
- `HOSTED_DEV` requires `https://`. An `http://` URL specified for `HOSTED_DEV` is rejected as invalid and resolves to `UNCONFIGURED`.
- Invalid configuration **never** crashes startup.
- The Supabase client is initialized once at application startup. Switching environments requires relaunching the app.

---

## 2. Platform Setup

> [!IMPORTANT]
> Never commit actual credentials, service-role keys, or private secrets to git. Use only public project URLs and anon/publishable keys with Row Level Security (RLS) enabled.

### A. Desktop (JVM)

Desktop reads configuration from JVM system properties with fallback to OS environment variables.

#### Launch with JVM System Properties
```bash
# Run against Local Supabase
./gradlew :desktopApp:run \
  -Dsupabase.environment=LOCAL \
  -Dsupabase.url=http://127.0.0.1:54321 \
  -Dsupabase.publishableKey=<YOUR_LOCAL_ANON_KEY>

# Run against Hosted Dev
./gradlew :desktopApp:run \
  -Dsupabase.environment=HOSTED_DEV \
  -Dsupabase.url=https://<PROJECT_REF>.supabase.co \
  -Dsupabase.publishableKey=<YOUR_HOSTED_PUBLISHABLE_KEY>
```

#### Launch with Environment Variables
```bash
export SUPABASE_ENVIRONMENT=LOCAL
export SUPABASE_URL=http://127.0.0.1:54321
export SUPABASE_PUBLISHABLE_KEY=<YOUR_LOCAL_ANON_KEY>

./gradlew :desktopApp:run
```

---

### B. Android

Android injects configuration into `BuildConfig` during build execution.

#### Option 1: `local.properties` (Recommended for Local Dev)
Add properties to `local.properties` in your repository root (this file is ignored by git):

```properties
supabase.environment=LOCAL
# Use 10.0.2.2 to access host localhost from Android Emulator:
supabase.url=http://10.0.2.2:54321
supabase.publishableKey=<YOUR_LOCAL_ANON_KEY>
```

#### Option 2: Gradle Project Properties
```bash
./gradlew :androidApp:assembleDebug \
  -Psupabase.environment=HOSTED_DEV \
  -Psupabase.url=https://<PROJECT_REF>.supabase.co \
  -Psupabase.publishableKey=<YOUR_HOSTED_PUBLISHABLE_KEY>
```

#### Option 3: CI Environment Variables
In continuous integration environments, set:
- `SUPABASE_ENVIRONMENT`
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`

---

### C. iOS

iOS reads configuration from `Info.plist`, which references build settings supplied via `.xcconfig`.

#### 1. Create `iosApp/Config.xcconfig`
Copy the template file:
```bash
cp iosApp/Config.xcconfig.template iosApp/Config.xcconfig
```

#### 2. Populate Values
Edit `iosApp/Config.xcconfig` (this file is ignored by git):
```xcconfig
SUPABASE_ENVIRONMENT = LOCAL
SUPABASE_URL = http://127.0.0.1:54321
SUPABASE_PUBLISHABLE_KEY = <YOUR_LOCAL_ANON_KEY>
```

If `iosApp/Config.xcconfig` is absent or variables are undefined, Xcode leaves `Info.plist` with unexpanded template tokens (e.g. `$(SUPABASE_URL)`), which the iOS runtime safely detects and resolves to `UNCONFIGURED`.

---

## 3. Automated Tests

Automated tests construct isolated, in-memory configurations without relying on environment variables or external files.

Use the provided test helpers in `TestKoinHelper.kt`:
```kotlin
// Local test config double
val config = testLocalConfig(
    url = "http://127.0.0.1:54321",
    publishableKey = "test_key"
)

// Hosted dev test config double
val hostedConfig = testHostedDevConfig(
    url = "https://<PROJECT_REF>.supabase.co",
    publishableKey = "test_key"
)

// Start test Koin with custom config
startTestKoin(
    supabaseConfig = config
)
```
