package com.rcforb.android.ui.login

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.rcforb.android.services.ConnectionManagerViewModel
import com.rcforb.android.services.CredentialStore
import com.rcforb.android.ui.theme.AppColors
import com.rcforb.android.ui.theme.noRippleClickable

private enum class LoginMode { LOADING, BIOMETRIC_PENDING, FORM }

@Composable
fun LoginScreen(vm: ConnectionManagerViewModel) {
    val context = LocalContext.current
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(LoginMode.LOADING) }

    fun doLogin() {
        if (user.isEmpty() || password.isEmpty()) return
        loading = true
        error = ""
        vm.authenticate(user, password) { result ->
            if (result.success) {
                if (rememberMe) CredentialStore.save(context, user, password)
            } else {
                error = result.message.ifEmpty { "Login failed" }
                loading = false
            }
        }
    }

    fun showBiometricPrompt() {
        val activity = context as? FragmentActivity ?: run {
            mode = LoginMode.FORM
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.i("Biometric", "Authentication succeeded")
                loading = true
                doLogin()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.w("Biometric", "Error: $errorCode - $errString")
                // User tapped "Use password" or cancelled — show login form
                mode = LoginMode.FORM
            }
            override fun onAuthenticationFailed() {
                // Single attempt failed but prompt stays open — do nothing
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("RCForb Login")
            .setSubtitle("Authenticate to sign in as $user")
            .setNegativeButtonText("Use password")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) {
        val creds = CredentialStore.load(context)
        if (creds != null) {
            user = creds.user
            password = creds.password
            rememberMe = true

            val bioManager = BiometricManager.from(context)
            val canAuth = bioManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                mode = LoginMode.BIOMETRIC_PENDING
            } else {
                mode = LoginMode.FORM
            }
        } else {
            mode = LoginMode.FORM
        }
    }

    // Auto-prompt biometric when mode switches to BIOMETRIC_PENDING
    LaunchedEffect(mode) {
        if (mode == LoginMode.BIOMETRIC_PENDING) {
            showBiometricPrompt()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        when (mode) {
            LoginMode.LOADING -> {
                // Brief splash while checking credentials/biometrics
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RCForb", color = AppColors.Cream, fontSize = AppColors.sp24, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Remote Ham Radio Control", color = AppColors.MutedForeground, fontSize = AppColors.sp13)
                }
            }

            LoginMode.BIOMETRIC_PENDING -> {
                // Minimal screen while biometric prompt is showing
                val cardShape = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .widthIn(max = 384.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .shadow(20.dp, cardShape)
                        .clip(cardShape)
                        .background(AppColors.ChassisGradientTo)
                        .border(2.dp, AppColors.BtnBorder, cardShape)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("RCForb", color = AppColors.Cream, fontSize = AppColors.sp24, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Remote Ham Radio Control", color = AppColors.CreamDark, fontSize = AppColors.sp13)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Authenticating as $user...",
                        color = AppColors.MutedForeground,
                        fontSize = AppColors.sp13,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    // Retry biometric button
                    val btnShape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(btnShape)
                            .background(AppColors.CreamDark)
                            .border(2.dp, AppColors.Cream, btnShape)
                            .noRippleClickable { showBiometricPrompt() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sign in with Biometrics", color = AppColors.TextDark, fontSize = AppColors.sp13, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Use password instead",
                        color = AppColors.MutedForeground,
                        fontSize = AppColors.sp12,
                        modifier = Modifier.noRippleClickable { mode = LoginMode.FORM }
                    )
                }
            }

            LoginMode.FORM -> {
                LoginForm(
                    user = user,
                    password = password,
                    rememberMe = rememberMe,
                    loading = loading,
                    error = error,
                    onUserChange = { user = it },
                    onPasswordChange = { password = it },
                    onRememberMeChange = { rememberMe = it },
                    onLogin = { doLogin() }
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    user: String,
    password: String,
    rememberMe: Boolean,
    loading: Boolean,
    error: String,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onLogin: () -> Unit
) {
    val cardShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .widthIn(max = 384.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(20.dp, cardShape)
            .clip(cardShape)
            .background(AppColors.ChassisGradientTo)
            .border(2.dp, AppColors.BtnBorder, cardShape)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("RCForb", color = AppColors.Cream, fontSize = AppColors.sp24, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Remote Ham Radio Control", color = AppColors.CreamDark, fontSize = AppColors.sp13)
        Spacer(modifier = Modifier.height(24.dp))

        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = AppColors.ErrorText,
                fontSize = AppColors.sp13,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.ErrorBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Username", color = AppColors.Cream, fontSize = AppColors.sp13)
        Spacer(modifier = Modifier.height(4.dp))
        CompactLoginField(
            value = user,
            onValueChange = onUserChange,
            placeholder = "Your RemoteHams.com username",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Password", color = AppColors.Cream, fontSize = AppColors.sp13)
        Spacer(modifier = Modifier.height(4.dp))
        CompactLoginField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Password",
            isPassword = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onLogin() })
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = onRememberMeChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = AppColors.Cream,
                    checkmarkColor = AppColors.TextDark
                )
            )
            Text("Remember me", color = AppColors.CreamDark, fontSize = AppColors.sp13)
        }
        Spacer(modifier = Modifier.height(16.dp))

        val loginShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(loginShape)
                .background(if (loading) AppColors.InputBgBottom else AppColors.CreamDark)
                .border(2.dp, AppColors.Cream, loginShape)
                .noRippleClickable { if (!loading) onLogin() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (loading) "Logging in..." else "Login",
                color = AppColors.TextDark,
                fontSize = AppColors.sp13,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CompactLoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(shape)
            .background(AppColors.InputBgBottom)
            .border(1.dp, AppColors.MetalDarkBorder, shape)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = AppColors.LabelDim, fontSize = AppColors.sp13)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = AppColors.Cream,
                fontSize = AppColors.sp13
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Cream),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
