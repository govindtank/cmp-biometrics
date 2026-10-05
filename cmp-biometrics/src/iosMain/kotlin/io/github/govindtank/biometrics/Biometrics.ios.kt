package io.github.govindtank.biometrics

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorBiometryLockout
import platform.LocalAuthentication.LAErrorBiometryNotAvailable
import platform.LocalAuthentication.LAErrorBiometryNotEnrolled
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

actual fun biometricInit(context: Any?) {
    // No context required on iOS
}

@OptIn(ExperimentalForeignApi::class)
actual fun checkBiometricAvailability(allowDeviceCredential: Boolean): BiometricAvailability {
    val context = LAContext()
    val policy = if (allowDeviceCredential) {
        LAPolicyDeviceOwnerAuthentication
    } else {
        LAPolicyDeviceOwnerAuthenticationWithBiometrics
    }

    val canEvaluate = context.canEvaluatePolicy(policy, null)
    if (canEvaluate) {
        return BiometricAvailability.AVAILABLE
    }

    return BiometricAvailability.HARDWARE_UNAVAILABLE
}

@OptIn(ExperimentalForeignApi::class)
actual fun getAvailableBiometricType(): BiometricType {
    val context = LAContext()
    val canEvaluate = context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
    if (!canEvaluate) return BiometricType.NONE

    // LABiometryType: 1 = TouchID, 2 = FaceID, 4 = OpticID (Vision Pro)
    return when (context.biometryType.toInt()) {
        1 -> BiometricType.FINGERPRINT
        2 -> BiometricType.FACE
        4 -> BiometricType.IRIS
        else -> BiometricType.NONE
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun authenticateBiometrics(config: BiometricPromptConfig): BiometricResult {
    val context = LAContext()
    context.localizedCancelTitle = config.negativeButtonText

    val policy = if (config.allowDeviceCredential) {
        LAPolicyDeviceOwnerAuthentication
    } else {
        LAPolicyDeviceOwnerAuthenticationWithBiometrics
    }

    val reason = config.description ?: config.subtitle ?: config.title

    return suspendCancellableCoroutine { continuation ->
        context.evaluatePolicy(
            policy = policy,
            localizedReason = reason
        ) { success, error ->
            if (continuation.isActive) {
                if (success) {
                    continuation.resume(BiometricResult.Success)
                } else {
                    val nsError = error as? NSError
                    val code = nsError?.code?.toInt() ?: -1
                    val result = when (code) {
                        LAErrorUserCancel.toInt() -> BiometricResult.UserCanceled
                        LAErrorBiometryLockout.toInt() -> BiometricResult.Lockout
                        LAErrorBiometryNotAvailable.toInt(),
                        LAErrorBiometryNotEnrolled.toInt() -> BiometricResult.NotAvailable
                        else -> BiometricResult.Error(code, nsError?.localizedDescription ?: "Authentication failed")
                    }
                    continuation.resume(result)
                }
            }
        }
    }
}
