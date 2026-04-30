package com.rcforb.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rcforb.android.models.ConnectionState
import com.rcforb.android.services.ConnectionManagerViewModel
import com.rcforb.android.ui.theme.RCForbTheme
import com.rcforb.android.ui.theme.AppColors
import com.rcforb.android.ui.theme.noRippleClickable
import com.rcforb.android.ui.login.LoginScreen
import com.rcforb.android.ui.lobby.LobbyScreen
import com.rcforb.android.ui.radio.RadioScreen

class MainActivity : FragmentActivity() {
    private var vm: ConnectionManagerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RCForbTheme {
                val viewModel: ConnectionManagerViewModel = viewModel()
                vm = viewModel
                RCForbApp(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            vm?.disconnect()
        }
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
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Error banner
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
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = text,
            color = AppColors.Cream,
            fontSize = AppColors.sp18
        )
    }
}
