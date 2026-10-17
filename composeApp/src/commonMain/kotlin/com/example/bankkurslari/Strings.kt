package com.example.bankkurslari

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** UI languages. Uzbek Cyrillic is generated from the Uzbek Latin text ([uzLatinToCyrillic]). */
enum class Lang(val code: String, val label: String) {
    UZ("uz", "Oʻzbekcha"),
    UZ_CYRL("uz-cyrl", "Ўзбекча"),
    RU("ru", "Русский"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String?): Lang = entries.firstOrNull { it.code == code } ?: UZ
    }
}

val LocalLang = staticCompositionLocalOf { Lang.UZ }

/**
 * Current UI language for plain (non-composable) text helpers such as distance or opening-hours labels.
 * [App] assigns it during composition, before any screen reads it, so it always matches [LocalLang].
 */
var appLang: Lang = Lang.UZ
    internal set

/** Shorthand used by text helpers: `pick(lang, uz, ru, en)`. Locals named `lang` take precedence. */
val lang: Lang get() = appLang

/** Picks the text for [lang]. English falls back to Russian until a string has an English version. */
fun pick(lang: Lang, uz: String, ru: String, en: String? = null): String = when (lang) {
    Lang.UZ -> uz
    Lang.UZ_CYRL -> uzLatinToCyrillic(uz)
    Lang.RU -> ru
    Lang.EN -> en ?: ru
}

/** Inline UI text in the current language. */
@Composable
@ReadOnlyComposable
fun tr(uz: String, ru: String, en: String? = null): String = pick(LocalLang.current, uz, ru, en)

/** Same pick outside composition (widget, notifications), from the saved language. */
fun trSaved(uz: String, ru: String, en: String? = null): String = pick(Lang.fromCode(RateService.language()), uz, ru, en)

/** So'm unit in the current language. */
@Composable
@ReadOnlyComposable
fun somUnit(): String = tr("soʻm", "сум", "UZS")
