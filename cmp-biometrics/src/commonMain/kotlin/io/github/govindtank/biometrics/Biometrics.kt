package io.github.govindtank.biometrics

/**
 * Initializes the biometric provider on platforms requiring context (e.g. Android).
 */
expect fun biometricInit(context: Any? = null)

/**
 * Checks whether biometric hardware is present and ready on this device.
 */
expect fun checkBiometricAvailability(allowDeviceCredential: Boolean = false): BiometricAvailability

/**
 * Returns the specific type of biometric supported/enrolled on this device.
 */
expect fun getAvailableBiometricType(): BiometricType

/**
 * Triggers native biometric authentication dialog.
 */
expect suspend fun authenticateBiometrics(
    config: BiometricPromptConfig = BiometricPromptConfig()
): BiometricResult
