# cmp-biometrics

[![JitPack](https://jitpack.io/v/govindtank/cmp-biometrics.svg)](https://jitpack.io/#govindtank/cmp-biometrics)

**Compose Multiplatform unified Biometrics (Face ID, Touch ID, Android BiometricPrompt).**

A lightweight, multiplatform library that allows Compose Multiplatform applications to seamlessly query biometric hardware and trigger biometric authentication dialogs across Android and iOS with a single unified Kotlin API.

---

## Features

- 📱 **Unified API**: One declarative Composable & Manager API for Android and iOS.
- 🔒 **Biometric Detection**: Distinguish between Face ID, Touch ID / Fingerprint, Iris, and multi-hardware enrollments.
- 🛡️ **Device Fallback**: Optional fallback to PIN/Pattern/Password device credentials when biometrics are unavailable.
- ⚡ **Coroutines & Compose**: Direct `suspend` invocation or asynchronous callback handling with `rememberBiometricManager()`.
- 🛠️ **Zero Boilerplate**: Pre-configured error classification (`Success`, `UserCanceled`, `Lockout`, `NotAvailable`, `Failed`).

---

## Installation

Add JitPack repository and the dependency to your `build.gradle.kts`:

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.govindtank:cmp-biometrics:1.0.0")
}
```

---

## Quick Start

### 1. Android Initialization (in your MainActivity)

```kotlin
import io.github.govindtank.biometrics.biometricInit

class MainActivity : FragmentActivity() { // or ComponentActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricInit(this)

        setContent {
            App()
        }
    }
}
```

### 2. Compose Usage

```kotlin
import androidx.compose.runtime.*
import androidx.compose.material3.*
import io.github.govindtank.biometrics.*

@Composable
fun SecureVaultScreen() {
    val biometricManager = rememberBiometricManager()
    var statusText by remember { mutableStateOf("Tap to Authenticate") }

    Button(onClick = {
        val availability = biometricManager.checkAvailability()
        if (availability == BiometricAvailability.AVAILABLE) {
            biometricManager.authenticate(
                BiometricPromptConfig(
                    title = "Unlock Your Vault",
                    subtitle = "Verify your identity to proceed",
                    negativeButtonText = "Cancel"
                )
            ) { result ->
                statusText = when (result) {
                    is BiometricResult.Success -> "Unlocked Successfully! 🎉"
                    is BiometricResult.UserCanceled -> "Authentication cancelled"
                    is BiometricResult.Lockout -> "Too many failed attempts. Locked."
                    is BiometricResult.Error -> "Error: ${result.message}"
                    else -> "Authentication failed"
                }
            }
        } else {
            statusText = "Biometrics not available: $availability"
        }
    }) {
        Text("Authenticate with Biometrics")
    }

    Text(statusText)
}
```

---

## Platform Hardware Support

| Platform | Native Mechanism | Supported Types |
| :--- | :--- | :--- |
| **Android** | `androidx.biometric.BiometricPrompt` | Fingerprint, Face Unlock, Iris Scan, PIN/Pattern fallback |
| **iOS** | `LocalAuthentication.framework` (LAContext) | Face ID, Touch ID, Optic ID, Passcode fallback |

---

## iOS Permission Configuration

In your iOS application's `Info.plist`, ensure you include `NSFaceIDUsageDescription`:

```xml
<key>NSFaceIDUsageDescription</key>
<string>We use Face ID to authenticate and secure your personal account.</string>
```

---

## License

Apache License 2.0
