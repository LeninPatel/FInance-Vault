package com.example.domain.security

import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Cryptographic PIN hashing, sensitive field masking, and hardware Biometric authentication
 * (Fingerprint & Face Unlock) utility.
 * Never stores authentication PINs as plain text.
 */
object PinSecurityUtil {

    fun generateSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPinSha256(pin: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$saltHex:$pin:finance_vault_v1".toByteArray(Charsets.UTF_8)
        val hashed = digest.digest(input)
        return hashed.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(enteredPin: String, storedHash: String?, storedSalt: String?): Boolean {
        if (storedHash.isNullOrBlank() || storedSalt.isNullOrBlank()) return false
        val computed = hashPinSha256(enteredPin, storedSalt)
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Masks an account or card number so only the last 4 digits are visible.
     * Returns "Not provided" if blank.
     */
    fun maskAccountNumber(raw: String, isPrivacyActive: Boolean = false): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "Not provided"
        if (isPrivacyActive) return "•••• ••••"
        val digitsOnly = trimmed.filter { it.isLetterOrDigit() }
        if (digitsOnly.length <= 4) return "••••$digitsOnly"
        return "•••• •••• ${digitsOnly.takeLast(4)}"
    }
}

enum class BiometricAvailability(
    val canAuthenticate: Boolean,
    val statusMessage: String
) {
    AVAILABLE(true, "Fingerprint / Face Unlock ready on this device"),
    NONE_ENROLLED(false, "Biometric sensor available — enroll Fingerprint or Face Unlock in Android Settings"),
    UNAVAILABLE(false, "Biometric sensor not active on this device/emulator (PIN fallback active)")
}

object BiometricAuthHelper {

    fun checkBiometricAvailability(context: Context): BiometricAvailability {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bm = context.getSystemService(BiometricManager::class.java)
                ?: return BiometricAvailability.UNAVAILABLE
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            return when (bm.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
                else -> BiometricAvailability.UNAVAILABLE
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(BiometricManager::class.java)
                ?: return BiometricAvailability.UNAVAILABLE
            @Suppress("DEPRECATION")
            return when (bm.canAuthenticate()) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
                else -> BiometricAvailability.UNAVAILABLE
            }
        } else {
            val pm = context.packageManager
            val hasFp = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
            return if (hasFp) BiometricAvailability.AVAILABLE else BiometricAvailability.UNAVAILABLE
        }
    }

    /**
     * Launches the Android system BiometricPrompt for Fingerprint or Face ID authentication,
     * with a negative button allowing immediate fallback to the 4-digit PIN keypad.
     */
    fun promptBiometricUnlock(
        context: Context,
        onSuccess: () -> Unit,
        onUsePinFallback: () -> Unit = {},
        onErrorMessage: (String) -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onErrorMessage("Biometric prompt requires Android 9+. Please enter your PIN.")
            onUsePinFallback()
            return
        }

        val availability = checkBiometricAvailability(context)
        if (!availability.canAuthenticate) {
            onErrorMessage(availability.statusMessage)
            onUsePinFallback()
            return
        }

        runCatching {
            val executor = ContextCompat.getMainExecutor(context)
            val cancellationSignal = CancellationSignal()

            val builder = BiometricPrompt.Builder(context)
                .setTitle("Unlock Finance Vault")
                .setSubtitle("Verify with Fingerprint or Face ID")
                .setDescription("Touch the fingerprint sensor or look at the front camera, or use your Security PIN.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            }

            builder.setNegativeButton(
                "Use Security PIN",
                executor,
                DialogInterface.OnClickListener { _, _ ->
                    onUsePinFallback()
                }
            )

            val prompt = builder.build()
            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                        ) {
                            errString?.toString()?.let { msg ->
                                if (msg.isNotBlank()) onErrorMessage(msg)
                            }
                        }
                        onUsePinFallback()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onErrorMessage("Biometric not recognized. Try again or enter your PIN.")
                    }
                }
            )
        }.onFailure { e ->
            onErrorMessage("Biometric prompt unavailable (${e.localizedMessage ?: "use PIN"}).")
            onUsePinFallback()
        }
    }
}
