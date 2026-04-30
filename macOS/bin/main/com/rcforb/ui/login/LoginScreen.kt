package com.rcforb.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rcforb.services.ConnectionManagerViewModel
import com.rcforb.services.CredentialStore
import com.rcforb.ui.theme.AppColors
import com.rcforb.ui.theme.noRippleClickable

@Composable
fun LoginScreen(vm: ConnectionManagerViewModel) {
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun doLogin() {
        if (user.isEmpty() || password.isEmpty()) return
        loading = true
        error = ""
        vm.authenticate(user, password) { result ->
            if (result.success) {
                if (rememberMe) CredentialStore.save(user, password)
            } else {
                error = result.message.ifEmpty { "Login failed" }
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val creds = CredentialStore.load()
        if (creds != null) {
            user = creds.user
            password = creds.password
            rememberMe = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
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
