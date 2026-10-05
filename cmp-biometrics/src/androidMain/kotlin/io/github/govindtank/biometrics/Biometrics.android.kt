package io.github.govindtank.biometrics

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

private var currentActivityRef: WeakReference<FragmentActivity>? = null
private var appContext: Context? = null

actual fun biometricInit(context: Any?) {
    when (context) {
        is FragmentActivity -> {
            currentActivityRef = WeakReference(context)
            appContext = context.applicationContext
        }
        is Context -> {
            appContext = context.applicationContext
        }
    }
}

actual fun checkBiometricAvailability(allowDeviceCredential: Boolean): BiometricAvailability {
    val ctx = appContext ?: currentActivityRef?.get() ?: return BiometricAvailability.HARDWARE_UNAVAILABLE
    val manager = AndroidBiometricManager.from(ctx)

    val authenticators = if (allowDeviceCredential) {
        AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or AndroidBiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK
    }

    return when (manager.canAuthenticate(authenticators)) {
        AndroidBiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
        AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
        AndroidBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
        AndroidBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
        AndroidBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
        else -> BiometricAvailability.UNSUPPORTED
    }
}

actual fun getAvailableBiometricType(): BiometricType {
    val ctx = appContext ?: currentActivityRef?.get() ?: return BiometricType.NONE
    val pm = ctx.packageManager

    val hasFingerprint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
    } else false

    val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FACE)
    } else false

    val hasIris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_IRIS)
    } else false

    return when {
        hasFingerprint && (hasFace || hasIris) -> BiometricType.MULTIPLE
        hasFingerprint -> BiometricType.FINGERPRINT
        hasFace -> BiometricType.FACE
        hasIris -> BiometricType.IRIS
        else -> BiometricType.NONE
    }
}

actual suspend fun authenticateBiometrics(config: BiometricPromptConfig): BiometricResult {
    val activity = currentActivityRef?.get()
        ?: return BiometricResult.Error(-1, "Activity not initialized. Call biometricInit(activity) first.")

    return suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) {
                    continuation.resume(BiometricResult.Success)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (continuation.isActive) {
                    val res = when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> BiometricResult.UserCanceled
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricResult.Lockout
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE,
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricResult.NotAvailable
                        else -> BiometricResult.Error(errorCode, errString.toString())
                    }
                    continuation.resume(res)
                }
            }

            override fun onAuthenticationFailed() {
                // Keep dialog open for user retry; if cancelled or failed finally, error/cancel callback fires
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(config.title)

        config.subtitle?.let { promptInfoBuilder.setSubtitle(it) }
        config.description?.let { promptInfoBuilder.setDescription(it) }

        if (config.allowDeviceCredential) {
            promptInfoBuilder.setAllowedAuthenticators(
                AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or AndroidBiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            promptInfoBuilder.setNegativeButtonText(config.negativeButtonText)
            promptInfoBuilder.setAllowedAuthenticators(AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG)
        }

        continuation.invokeOnCancellation {
            prompt.cancelAuthentication()
        }

        prompt.authenticate(promptInfoBuilder.build())
    }
}
