package io.github.govindtank.biometrics

/**
 * Types of biometric hardware available on the user's device.
 */
enum class BiometricType {
    NONE,
    FINGERPRINT,
    FACE,
    IRIS,
    MULTIPLE
}

/**
 * Hardware capability and enrollment status for biometrics.
 */
enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED
}

/**
 * Result of a biometric authentication attempt.
 */
sealed class BiometricResult {
    data object Success : BiometricResult()
    data class Error(val code: Int, val message: String) : BiometricResult()
    data object UserCanceled : BiometricResult()
    data object Lockout : BiometricResult()
    data object NotAvailable : BiometricResult()
    data object Failed : BiometricResult()
}

/**
 * Prompt configuration passed to the native platform biometric sheet/dialog.
 */
data class BiometricPromptConfig(
    val title: String = "Biometric Authentication",
    val subtitle: String? = null,
    val description: String? = null,
    val negativeButtonText: String = "Cancel",
    val allowDeviceCredential: Boolean = false
)
