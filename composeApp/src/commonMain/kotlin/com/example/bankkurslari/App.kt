package com.example.bankkurslari

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.bankkurslari.ui.theme.BankKurslariTheme

/**
 * Root of the app on every platform. [onDarkTheme] lets the host style its system bars to match the app theme
 * (which may differ from the system one).
 */
@Composable
fun App(onDarkTheme: (Boolean) -> Unit = {}) {
    val systemDark = isSystemInDarkTheme()
    var themeMode by remember { mutableStateOf(RateService.themeMode()) }
    var lang by remember { mutableStateOf(Lang.fromCode(RateService.language())) }
    appLang = lang
    val dark = when (themeMode) { 1 -> false; 2 -> true; else -> systemDark }
    LaunchedEffect(dark) { onDarkTheme(dark) }
    BankKurslariTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalLang provides lang) {
            // No onboarding: language comes from the device, location is asked in context.
            CurrencyTrackerApp(
                themeMode = themeMode,
                onThemeMode = {
                    themeMode = it
                    RateService.setThemeMode(it)
                },
                lang = lang,
                onLang = {
                    lang = it
                    RateService.setLanguage(it.code)
                }
            )
        }
    }
}
