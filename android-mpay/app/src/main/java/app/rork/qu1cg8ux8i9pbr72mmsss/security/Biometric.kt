package app.rork.qu1cg8ux8i9pbr72mmsss.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object Biometric {
    private const val AUTH = BIOMETRIC_STRONG or BIOMETRIC_WEAK

    fun available(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun Context.findActivity(): FragmentActivity? {
        var c: Context? = this
        while (c is ContextWrapper) {
            if (c is FragmentActivity) return c
            c = c.baseContext
        }
        return null
    }

    fun prompt(
        context: Context,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFallback: () -> Unit,
    ) {
        val activity = context.findActivity() ?: return onFallback()
        if (!available(activity)) return onFallback()
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFallback()
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Ввести PIN-код")
            .setAllowedAuthenticators(AUTH)
            .build()
        prompt.authenticate(info)
    }
}
