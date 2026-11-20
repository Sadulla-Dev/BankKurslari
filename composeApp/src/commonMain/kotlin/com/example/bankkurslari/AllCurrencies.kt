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
import androidx.compose.foundation.shape.CircleShape
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
    // Bank cash rates for the few extra currencies banks publish (GBP, KZT); static files, so loading both is free.
    var extra by remember { mutableStateOf<Map<String, List<ExtraBankRate>>>(emptyMap()) }
    LaunchedEffect(Unit) {
        extra = BANK_RATE_EXTRAS.associateWith { cachedExtraRates(it) }.filterValues { it.isNotEmpty() }
        for (code in BANK_RATE_EXTRAS) runCatching { fetchExtraRates(code) }.onSuccess { extra = extra + (code to it) }
    }
    var sellSide by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(appStore().getString("cbu_selected") ?: "CNY") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.example.bankkurslari.ui.theme.VsCard,
        shape = VsShape.Tray,
        tonalElevation = 0.dp
    ) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.foundation.LocalOverscrollFactory provides null) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).vsSheetScrollGuard(sheetState).padding(horizontal = 20.dp)) {
            Text(tr("Barcha valyutalar", "Все валюты", "All currencies"), style = VsType.Title, color = VsInk)
            val date = list.firstOrNull()?.date
            Text(
                tr("Markaziy bank kursi", "Курс ЦБ", "Central Bank rate") + (date?.let { " · $it" } ?: "") +
                    tr(" · banklarda naqd kurs boshqacha boʻlishi mumkin", " · наличный курс в банках может отличаться", " · bank cash rates may differ"),
                style = VsType.Meta, color = VsInkSoft
            )

            val cur = list.firstOrNull { it.code == selected }
            if (cur != null) Converter(cur)

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

            // Bank rows are inserted above the search field when they load or the currency changes; the list would keep
            // its place on the search field and hide them, so jump back to the top.
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val hasBanks = extra[selected] != null
            LaunchedEffect(selected, hasBanks) { listState.scrollToItem(0) }
            LazyColumn(Modifier.fillMaxWidth().weight(1f), state = listState) {
                if (cur != null) bankRatesItems(selected, extra[selected], sellSide) { sellSide = it }
                item(key = "search") {
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = VsType.Body.copy(color = VsInk), cursorBrush = SolidColor(VsEmerald),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    decorationBox = { inner ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(CircleShape).background(com.example.bankkurslari.ui.theme.VsGround).padding(horizontal = 14.dp),
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
                }
                items(shown, key = { it.code }) { c ->
                    CurrencyRow(
                        c = c, fav = c.code in favs, selected = c.code == selected, bankCount = extra[c.code]?.size ?: 0,
                        onFav = { toggleFav(c.code) },
                        onClick = { selected = c.code; appStore().putString("cbu_selected", c.code) }
                    )
                    HorizontalDivider(color = com.example.bankkurslari.ui.theme.VsGround)
                }
                item { Spacer(Modifier.height(16.dp)); Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
            }
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
        Modifier.fillMaxWidth().padding(top = 16.dp).clip(VsShape.Stat).background(com.example.bankkurslari.ui.theme.VsGround).padding(16.dp)
    ) {
        Text(c.localName(), style = VsType.Meta, color = VsInkSoft)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = amount,
                onValueChange = { v -> amount = v.filter { it.isDigit() }.take(12) },
                singleLine = true,
                textStyle = VsType.Amount.copy(color = VsInk),
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
                style = VsType.Display, color = VsEmerald, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            "${c.nominal} ${c.code} = ${rateText(c.rate)} $som",
            style = VsType.Meta, color = VsInkSoft
        )
    }
}

@Composable
private fun CurrencyRow(c: CbuCurrency, fav: Boolean, selected: Boolean, bankCount: Int, onFav: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            c.code, style = VsType.Value,
            color = if (selected) VsEmerald else VsInk, modifier = Modifier.width(52.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(c.localName(), style = VsType.Body, color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (c.nominal != 1) Text(tr("${c.nominal} birlik uchun", "за ${c.nominal} ед.", "per ${c.nominal} units"), style = VsType.Meta, color = VsInkSoft)
            if (bankCount > 0) Text(tr("$bankCount ta bankda", "в $bankCount банках", "at $bankCount banks"), style = VsType.Meta, color = VsEmerald)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(rateText(c.rate), style = VsType.Value, color = VsInk, textAlign = TextAlign.End)
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
                else tr("Sevimlilarga qoʻshish", "Добавить в избранное", "Add to favourites"),
                tint = if (fav) VsEmerald else VsInkSoft, modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** "15 550" for pound-sized rates, "26.10" for tenge. */
private fun bankRateText(v: Double): String = if (v >= 1000) v.roundToLong().formatUzsL() else v.fixed(2)

/** Gap to the best rate without trailing zeros: "50", "0.3". */
private fun gapText(v: Double): String {
    val r = (v * 100).roundToLong() / 100.0
    return if (r >= 1000 || r == r.roundToLong().toDouble()) r.roundToLong().formatUzsL() else r.fixed(2).trimEnd('0').trimEnd('.')
}

/**
 * Bank cash rates for the selected currency, only from banks that publish one; other currencies get a short note.
 * [sell]: the user sells the currency, so the bank's buy rate counts and the highest is best.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.bankRatesItems(
    code: String, banks: List<ExtraBankRate>?, sell: Boolean, onSide: (Boolean) -> Unit
) {
    if (code !in BANK_RATE_EXTRAS) {
        item(key = "banks-none") {
            Text(
                tr("Banklar bu valyuta kursini eʼlon qilmagan — faqat Markaziy bank kursi.",
                    "Банки не публикуют курс этой валюты — только курс ЦБ.",
                    "Banks don’t publish a rate for this currency — Central Bank rate only."),
                style = VsType.Meta, color = VsInkSoft,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clip(VsShape.Stat).background(com.example.bankkurslari.ui.theme.VsGround).padding(12.dp)
            )
        }
        return
    }
    if (banks.isNullOrEmpty()) return
    val ranked = banks.mapNotNull { b -> (if (sell) b.buy else b.sell)?.let { b.bank to it } }
        .sortedBy { if (sell) -it.second else it.second }
    if (ranked.isEmpty()) return
    val best = ranked.first().second
    item(key = "banks-head") {
        Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Banklarda (${banks.size})", "В банках (${banks.size})", "At banks (${banks.size})"), style = VsType.Value, color = VsInk, modifier = Modifier.weight(1f))
                SegmentedHug(
                    options = listOf(tr("Sotsangiz", "Продать", "Sell"), tr("Olsangiz", "Купить", "Buy")),
                    selected = if (sell) 0 else 1, onSelect = { onSide(it == 0) }, height = 36.dp
                )
            }
            Text(
                if (sell) tr("Bank sizdan oladi — eng qimmati tepada", "Банк покупает у вас — самый дорогой сверху", "The bank buys from you — highest first")
                else tr("Bank sizga sotadi — eng arzoni tepada", "Банк продаёт вам — самый дешёвый сверху", "The bank sells to you — cheapest first"),
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }
    }
    items(ranked, key = { "bank-" + it.first }) { (bank, rate) ->
        val gap = abs(rate - best)
        val top = gap < 1e-9
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (top) com.example.bankkurslari.ui.theme.VsEmeraldSoft else androidx.compose.ui.graphics.Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BankTile(bank, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(shortBankName(bank), style = VsType.Body, color = VsInk, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(bankRateText(rate), style = VsType.Value, color = VsInk)
            Text(
                if (top) tr("eng yaxshi", "лучший", "best") else (if (sell) "−" else "+") + gapText(gap),
                style = VsType.Meta, color = if (top) VsEmerald else VsInkSoft, textAlign = TextAlign.End,
                modifier = Modifier.width(76.dp)
            )
        }
    }
}
