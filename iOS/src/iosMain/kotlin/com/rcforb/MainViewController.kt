package com.rcforb

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Swift entry point: returned UIViewController hosts the Compose app. */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
