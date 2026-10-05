package io.github.govindtank.biometrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Controller for managing biometric capabilities and performing authentication in Compose.
 */
class BiometricManager internal constructor(
    private val scope: CoroutineScope
) {
    /**
     * Checks if biometric authentication can currently be performed.
     */
    fun checkAvailability(allowDeviceCredential: Boolean = false): BiometricAvailability {
        return checkBiometricAvailability(allowDeviceCredential)
    }

    /**
     * Identifies the primary biometric hardware available (Face, Fingerprint, Iris, etc.).
     */
    fun getBiometricType(): BiometricType {
        return getAvailableBiometricType()
    }

    /**
     * Requests biometric authentication asynchronously.
     */
    suspend fun authenticate(config: BiometricPromptConfig = BiometricPromptConfig()): BiometricResult {
        return authenticateBiometrics(config)
    }

    /**
     * Non-suspending callback-based trigger convenient for Compose UI event handlers.
     */
    fun authenticate(
        config: BiometricPromptConfig = BiometricPromptConfig(),
        onResult: (BiometricResult) -> Unit
    ) {
        scope.launch {
            val result = authenticateBiometrics(config)
            onResult(result)
        }
    }
}

/**
 * Creates and remembers a [BiometricManager] tied to the current composition's CoroutineScope.
 */
@Composable
fun rememberBiometricManager(): BiometricManager {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        BiometricManager(scope)
    }
}
