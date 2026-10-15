package com.example.bankkurslari

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Entry point used by the Swift app (iosApp/ContentView.swift). */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
