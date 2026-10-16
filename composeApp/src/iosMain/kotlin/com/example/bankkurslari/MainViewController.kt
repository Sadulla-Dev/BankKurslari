package com.example.bankkurslari

import androidx.compose.ui.window.ComposeUIViewController
import kotlin.concurrent.AtomicInt
import kotlin.experimental.ExperimentalNativeApi
import platform.UIKit.UIViewController

private val hookInstalled = AtomicInt(0)

/**
 * Records uncaught Kotlin exceptions before the process aborts: NSLog (device console) and
 * Caches/cache-json/last-crash.txt, which can be pulled from the app container over USB.
 */
@OptIn(ExperimentalNativeApi::class)
private fun installCrashRecorder() {
    if (!hookInstalled.compareAndSet(0, 1)) return
    setUnhandledExceptionHook { t ->
        val text = "time=${nowMillis()}\n" + t.stackTraceToString()
        // File first: it must survive even if logging fails.
        runCatching { writeCacheFile("last-crash.txt", text) }
        runCatching { debugLog("CRASH $text") }
    }
}

/** Entry point used by the Swift app (iosApp/ContentView.swift). */
fun MainViewController(): UIViewController {
    installCrashRecorder()
    return ComposeUIViewController { App() }
}
