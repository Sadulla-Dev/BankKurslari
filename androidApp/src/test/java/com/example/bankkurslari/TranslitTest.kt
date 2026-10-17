package com.example.bankkurslari

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslitTest {
    private fun t(s: String) = uzLatinToCyrillic(s)

    @Test
    fun commonUiStrings() {
        assertEquals("Қўлингизга тегади", t("Qoʻlingizga tegadi"))
        assertEquals("Марказий банк олтин қуймалари", t("Markaziy bank oltin quymalari"))
        assertEquals("Ёнғин ва шаҳар", t("Yongʻin va shahar"))
        assertEquals("Янгиланган: 16.10.2026, 09:53 · cbu.uz", t("Yangilangan: 16.10.2026, 09:53 · cbu.uz"))
        assertEquals("1 USD = 12 040 сўм", t("1 USD = 12 040 soʻm"))
        assertEquals("МБ курси тарихи", t("MB kursi tarixi"))
        assertEquals("Эслатма: маълумот", t("Eslatma: maʼlumot"))
        assertEquals("Чет эл валюталари", t("Chet el valyutalari"))
        assertEquals("Ёки", t("Yoki"))
        assertEquals("Ўзбекистон", t("Oʻzbekiston"))
    }
}
