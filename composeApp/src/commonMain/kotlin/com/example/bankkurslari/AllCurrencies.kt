package com.example.bankkurslari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
import com.example.bankkurslari.ui.theme.VsSurface
import com.example.bankkurslari.ui.theme.VsType
import kotlin.math.abs
import kotlin.math.roundToLong

private const val FAV_KEY = "cbu_fav"

/** Pinned on first use: currencies people in Uzbekistan most often need beyond USD/EUR/RUB. */
private val DEFAULT_FAVS = setOf("CNY", "KZT", "TRY", "AED", "KRW", "GBP")

/** Name in the current UI language. */
@Composable
private fun CbuCurrency.localName(): String = when (LocalLang.current) {
    Lang.UZ -> nameUz
    Lang.UZ_CYRL -> nameUzc.ifEmpty { uzLatinToCyrillic(nameUz) }
    Lang.RU -> nameRu
    Lang.EN -> nameEn
}

/** "1 803.07" for big rates, "8.21" / "0.08" for small ones (KRW, JPY per unit). */
private fun rateText(v: Double): String = when {
    v >= 1000 -> v.formatRate()
    v >= 1 -> v.fixed(2)
    else -> v.fixed(4)
}

/**
 * All CBU currencies: two-way converter (currency <-> so'm) on top, searchable list with favourites below.
 * Official CBU rate only; banks trade few of these in cash.
 */
@Composable
fun AllCurrenciesSheet(onDismiss: () -> Unit) {
    var list by remember { mutableStateOf<List<CbuCurrency>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        list = cachedCbuAll()
        runCatching { fetchCbuAll() }.onSuccess { list = it }.onFailure { if (list.isEmpty()) failed = true }
    }
    var favs by remember {
        mutableStateOf(appStore().getStringSet(FAV_KEY).ifEmpty { DEFAULT_FAVS })
    }
    fun toggleFav(code: String) {
        favs = if (code in favs) favs - code else favs + code
        appStore().putStringSet(FAV_KEY, favs)
    }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(appStore().getString("cbu_selected") ?: "CNY") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VsBg,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 16.dp)) {
            Text(tr("Barcha valyutalar", "Все валюты", "All currencies"), style = VsType.Title, color = VsInk)
            val date = list.firstOrNull()?.date
            Text(
                tr("Markaziy bank kursi", "Курс ЦБ", "Central Bank rate") + (date?.let { " · $it" } ?: "") +
                    tr(" · banklarda naqd kurs boshqacha boʻlishi mumkin", " · наличный курс в банках может отличаться", " · bank cash rates may differ"),
                style = VsType.Meta, color = VsInkSoft
            )

            val cur = list.firstOrNull { it.code == selected }
            if (cur != null) Converter(cur)

            // search
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = VsType.Body.copy(color = VsInk), cursorBrush = SolidColor(VsEmerald),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                decorationBox = { inner ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(VsSurface).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, null, tint = VsInkSoft, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text(tr("Valyuta yoki kod", "Валюта или код", "Currency or code"), style = VsType.Body, color = VsInkSoft)
                            inner()
                        }
                    }
                }
            )

            if (list.isEmpty()) {
                Text(
                    if (failed) tr("Kurslar hozircha mavjud emas.", "Курсы пока недоступны.", "Rates are not available yet.")
                    else tr("Yuklanmoqda…", "Загрузка…", "Loading…"),
                    style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 16.dp)
                )
            }

            val q = query.trim().lowercase()
            val shown = list.filter { c ->
                c.code !in setOf("USD", "EUR", "RUB") && (q.isEmpty() ||
                    c.code.lowercase().contains(q) || c.nameUz.lowercase().contains(q) || c.nameUzc.lowercase().contains(q) ||
                    c.nameRu.lowercase().contains(q) || c.nameEn.lowercase().contains(q))
            }.sortedWith(compareBy<CbuCurrency> { it.code !in favs }.thenBy { it.code })

            LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
                items(shown, key = { it.code }) { c ->
                    CurrencyRow(
                        c = c, fav = c.code in favs, selected = c.code == selected,
                        onFav = { toggleFav(c.code) },
                        onClick = { selected = c.code; appStore().putString("cbu_selected", c.code) }
                    )
                    HorizontalDivider(color = VsLine)
                }
                item { Spacer(Modifier.height(16.dp)); Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
            }
        }
    }
}

@Composable
private fun Converter(c: CbuCurrency) {
    var amount by remember(c.code) { mutableStateOf(c.nominal.toString()) }
    var toSom by remember { mutableStateOf(true) } // true: currency -> so'm
    val som = somUnit()
    val a = amount.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val result = if (toSom) a * c.perUnit else if (c.perUnit > 0) a / c.perUnit else 0.0
    val from = if (toSom) c.code else som
    val to = if (toSom) som else c.code

    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(12.dp)).background(VsSurface).padding(16.dp)
    ) {
        Text(c.localName(), style = VsType.Meta, color = VsInkSoft)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = amount,
                onValueChange = { v -> amount = v.filter { it.isDigit() }.take(12) },
                singleLine = true,
                textStyle = VsType.Title.copy(color = VsInk),
                cursorBrush = SolidColor(VsEmerald),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Text(from, style = VsType.Body, color = VsInkSoft)
            IconButton(onClick = { toSom = !toSom }) {
                Icon(Icons.Outlined.SwapVert, tr("Yoʻnalishni almashtirish", "Поменять направление", "Swap direction"), tint = VsInk)
            }
        }
        AnimatedContent(result, transitionSpec = { fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT)) }, label = "conv") { r ->
            Text(
                (if (r >= 100) r.roundToLong().formatUzsL() else r.fixed(2)) + " $to",
                style = VsType.Hero, color = VsEmerald, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            "${c.nominal} ${c.code} = ${rateText(c.rate)} $som",
            style = VsType.Meta, color = VsInkSoft
        )
    }
}

@Composable
private fun CurrencyRow(c: CbuCurrency, fav: Boolean, selected: Boolean, onFav: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            c.code, style = VsType.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) VsEmerald else VsInk, modifier = Modifier.width(52.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(c.localName(), style = VsType.Body, color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (c.nominal != 1) Text(tr("${c.nominal} birlik uchun", "за ${c.nominal} ед.", "per ${c.nominal} units"), style = VsType.Meta, color = VsInkSoft)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(rateText(c.rate), style = VsType.BodyMedium, color = VsInk, textAlign = TextAlign.End)
            if (abs(c.diff) >= 0.005) {
                Text(
                    (if (c.diff > 0) "▲ " else "▼ ") + (if (abs(c.diff) >= 1000) rateText(abs(c.diff)) else abs(c.diff).fixed(2)),
                    style = VsType.Meta, color = VsInkSoft, textAlign = TextAlign.End
                )
            }
        }
        IconButton(onClick = onFav) {
            Icon(
                if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                if (fav) tr("Sevimlilardan olib tashlash", "Убрать из избранного", "Remove from favourites")
                else tr("Sevimlilarga qoʻshish", "В избранное", "Add to favourites"),
                tint = if (fav) VsEmerald else VsInkSoft, modifier = Modifier.size(20.dp)
            )
        }
    }
}
