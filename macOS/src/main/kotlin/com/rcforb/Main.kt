package com.rcforb

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.rcforb.models.ConnectionState
import com.rcforb.services.ConnectionManagerViewModel
import com.rcforb.ui.lobby.LobbyScreen
import com.rcforb.ui.login.LoginScreen
import com.rcforb.ui.radio.RadioScreen
import com.rcforb.ui.theme.AppColors
import com.rcforb.ui.theme.RCForbTheme
import com.rcforb.ui.theme.noRippleClickable

fun main() = application {
    // One instance per process. Build the VM once.
    val vm = remember { ConnectionManagerViewModel() }

    // Ensure resources clean up on JVM shutdown.
    DisposableEffect(Unit) {
        onDispose { vm.cleared() }
    }

    val state: WindowState = rememberWindowState(size = DpSize(1395.dp, 833.dp))

    Window(
        onCloseRequest = ::exitApplication,
        title = "RCForb",
        state = state
    ) {
        RCForbTheme { RCForbApp(vm) }
    }
}

@Composable
fun RCForbApp(vm: ConnectionManagerViewModel) {
    val connectionState by vm.connectionState.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        errorMessage?.let { error ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.ErrorBg)
                    .padding(horizontal = AppColors.dp16, vertical = AppColors.dp8),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = error,
                    color = AppColors.ErrorText,
                    fontSize = AppColors.sp13
                )
                Text(
                    text = "Dismiss",
                    color = AppColors.ErrorDismiss,
                    fontSize = AppColors.sp13,
                    modifier = Modifier.noRippleClickable { vm.clearError() }
                )
            }
        }

        when (connectionState) {
            ConnectionState.DISCONNECTED, ConnectionState.FAILED -> LoginScreen(vm)
            ConnectionState.AUTHENTICATING -> LoadingView("Authenticating...")
            ConnectionState.AUTHENTICATED -> LobbyScreen(vm)
            ConnectionState.CONNECTING -> LoadingView("Connecting to station...")
            ConnectionState.CONNECTED -> RadioScreen(vm)
        }
    }
}

@Composable
fun LoadingView(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AppColors.Cream,
            fontSize = AppColors.sp18
        )
    }
}
