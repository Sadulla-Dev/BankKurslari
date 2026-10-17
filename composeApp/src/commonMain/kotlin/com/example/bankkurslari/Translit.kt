package com.example.bankkurslari

/**
 * Uzbek Latin -> Uzbek Cyrillic (1995 alphabet rules). Used to show every Uzbek UI string in Cyrillic
 * without a second copy of each text.
 *
 * Left untouched: 3-letter all-caps currency codes (USD, UZS), tokens with '.', '/', '@' or digits (cbu.uz, 12:31),
 * so currency codes, abbreviations and addresses keep their spelling.
 */
fun uzLatinToCyrillic(text: String): String = TOKEN.replace(text) { m -> word(m.value) }

private val TOKEN = Regex("""[A-Za-z][A-Za-z‘’ʻʼ'`.@/0-9]*""")
private const val APOS = "‘’ʻʼ'`"

private val SINGLE = mapOf(
    'a' to "а", 'b' to "б", 'd' to "д", 'e' to "е", 'f' to "ф", 'g' to "г", 'h' to "ҳ", 'i' to "и",
    'j' to "ж", 'k' to "к", 'l' to "л", 'm' to "м", 'n' to "н", 'o' to "о", 'p' to "п", 'q' to "қ",
    'r' to "р", 's' to "с", 't' to "т", 'u' to "у", 'v' to "в", 'x' to "х", 'y' to "й", 'z' to "з",
    'c' to "с", 'w' to "в"
)
private val VOWELS = "aeiouAEIOU"

private fun word(w: String): String {
    if (w.any { it == '.' || it == '/' || it == '@' || it.isDigit() }) return w
    val letters = w.filter { it.isLetter() }
    // ISO currency codes stay Latin (USD, UZS); other abbreviations follow the script (MB -> МБ).
    if (letters.length == 3 && letters.all { it.isUpperCase() }) return w
    val sb = StringBuilder()
    var i = 0
    while (i < w.length) {
        val c = w[i]
        val lc = c.lowercaseChar()
        val next = w.getOrNull(i + 1)?.lowercaseChar()
        val upper = c.isUpperCase()
        fun emit(s: String, len: Int) {
            sb.append(if (upper) s.replaceFirstChar { it.uppercaseChar() } else s)
            i += len
        }
        when {
            c in APOS -> {
                // tutuq belgisi (maʼlumot, saʼat): hard sign
                sb.append('ъ'); i++
            }
            (lc == 'o' || lc == 'g') && next != null && next in APOS -> emit(if (lc == 'o') "ў" else "ғ", 2)
            lc == 's' && next == 'h' -> emit("ш", 2)
            lc == 'c' && next == 'h' -> emit("ч", 2)
            lc == 'y' && next == 'a' -> emit("я", 2)
            lc == 'y' && next == 'o' && w.getOrNull(i + 2)?.let { it in APOS } != true -> emit("ё", 2)
            lc == 'y' && next == 'u' -> emit("ю", 2)
            lc == 'y' && next == 'e' -> emit("е", 2)
            lc == 'e' && (i == 0 || w[i - 1] in VOWELS) -> emit("э", 1)
            SINGLE.containsKey(lc) -> emit(SINGLE.getValue(lc), 1)
            else -> { sb.append(c); i++ }
        }
    }
    return sb.toString()
}
