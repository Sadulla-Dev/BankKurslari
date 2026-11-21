package com.example.bankkurslari

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsEmeraldDeep
import com.example.bankkurslari.ui.theme.VsEmeraldSoft
import com.example.bankkurslari.ui.theme.VsGround
import com.example.bankkurslari.ui.theme.VsHair
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsRoseText
import com.example.bankkurslari.ui.theme.VsType
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Hamyon tab: "What are my savings worth today, and how do I turn one currency into another for the least loss?"
 * Holdings live only on the phone (appStore); rates are the same API files the Kurs tab reads.
 */
@Composable
fun HamyonScreen(innerPadding: PaddingValues, lang: Lang, onOpenSettings: () -> Unit) {
    var holdings by remember { mutableStateOf(WalletStore.load()) }
    var quotes by remember { mutableStateOf<Map<WalletCode, Quote>>(emptyMap()) }
    var failed by remember { mutableStateOf(false) }
    var stale by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Holding?>(null) }
    var histories by remember { mutableStateOf<Map<WalletCode, List<HistoryPoint>>>(emptyMap()) }
    var goal by remember { mutableStateOf(GoalStore.load()) }
    var goalSheet by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        histories = cachedWalletHistories()
        launch { histories = histories + fetchWalletHistories() }
        quotes = cachedQuotes()
        val fresh = fetchQuotes()
        quotes = quotes + fresh
        failed = fresh.isEmpty() && quotes.isEmpty()
        stale = fresh.isEmpty() && quotes.isNotEmpty()
    }

    fun save(list: List<Holding>) { holdings = list; WalletStore.save(list) }

    Column(
        Modifier.fillMaxSize().background(VsBg).padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pick(lang, "Hamyon", "Кошелёк", "Wallet"), style = VsType.Title.copy(fontSize = 26.sp, lineHeight = 30.sp), color = VsInk)
                Text(
                    pick(lang, "Jamgʻarmangiz bugun qancha turadi", "Сколько стоят ваши сбережения сегодня", "What your savings are worth today"),
                    style = VsType.Meta, color = VsInkSoft
                )
            }
            VsIconButton(Icons.Outlined.Settings, pick(lang, "Sozlamalar", "Настройки", "Settings"), onClick = onOpenSettings)
        }

        val summary = walletSummary(holdings, quotes)
        if (holdings.isEmpty()) {
            EmptyWallet(lang) { adding = true }
        } else {
            WalletHero(summary, lang)
            GoalCard(goal, summary.total, quotes, lang) { goalSheet = true }
            HoldingsPanel(summary, lang, onEdit = { editing = it }, onAdd = { adding = true })
            TrendCard(holdings, histories, quotes, lang)
        }
        if (stale) {
            val at = quotes[WalletCode.USD]?.updatedAt?.formatUpdatedAt().orEmpty()
            Text(
                pick(lang, "Internet yoʻq: oxirgi saqlangan kurslar", "Нет интернета: последние сохранённые курсы", "Offline: last saved rates") + if (at.isNotEmpty()) ", $at" else "",
                style = VsType.Meta, color = VsRoseText
            )
        }
        if (failed) {
            Text(
                pick(lang, "Kurslarni yuklab boʻlmadi. Internetni tekshiring.", "Не удалось загрузить курсы. Проверьте интернет.", "Could not load rates. Check your connection."),
                style = VsType.Meta, color = VsRoseText
            )
        }

        ExchangeCard(quotes, lang)

        Text(
            pick(
                lang,
                "Qiymat bugun eng koʻp toʻlaydigan bankning naqd kursi boʻyicha (oltin: Markaziy bank qaytarib sotib olish narxi, 10 g quyma). Maʼlumotlaringiz faqat telefoningizda saqlanadi.",
                "Стоимость по наличному курсу банка, который сегодня платит больше всех (золото: цена обратного выкупа ЦБ, слиток 10 г). Ваши данные хранятся только на телефоне.",
                "Valued at the cash rate of the bank paying the most today (gold: CBU buyback price, 10 g bar). Your data stays on this phone."
            ),
            style = VsType.Meta, color = VsInkSoft
        )
    }

    if (goalSheet) {
        GoalSheet(goal, lang, onDismiss = { goalSheet = false }, onSave = { g -> goal = g; GoalStore.save(g); goalSheet = false })
    }

    val sheetFor = editing
    if (adding || sheetFor != null) {
        HoldingSheet(
            initial = sheetFor,
            taken = holdings.map { it.code }.toSet(),
            lang = lang,
            onDismiss = { adding = false; editing = null },
            onSave = { h ->
                save(WalletStore.upsert(holdings, h, replace = sheetFor != null))
                adding = false; editing = null
            },
            onDelete = sheetFor?.let { e -> { save(holdings.filter { it.code != e.code }); editing = null } }
        )
    }
}

@Composable
private fun EmptyWallet(lang: Lang, onAdd: () -> Unit) {
    VsCardBox {
        Text(pick(lang, "Jamgʻarmangizni qoʻshing", "Добавьте свои сбережения", "Add your savings"), style = VsType.Title, color = VsInk)
        Text(
            pick(
                lang,
                "Dollar, yevro, rubl, oltin yoki soʻmda qancha pulingiz borligini yozing. Ilova har kuni u bugun banklarda qancha turishini va kechagidan qancha oʻzgarganini koʻrsatadi.",
                "Укажите, сколько у вас долларов, евро, рублей, золота или сумов. Приложение каждый день покажет, сколько это стоит в банках сегодня и как изменилось со вчера.",
                "Enter how much you keep in dollars, euros, rubles, gold or so'm. Every day the app shows what it is worth at banks and how it changed since yesterday."
            ),
            style = VsType.Body, color = VsInkSoft
        )
        VsPrimaryButton(pick(lang, "Qoʻshish", "Добавить", "Add"), onClick = onAdd, icon = Icons.Outlined.Add, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

@Composable
private fun WalletHero(s: WalletSummary, lang: Lang) {
    val unit = somUnit()
    VsCardBox {
        Text(pick(lang, "Banklarda sotsangiz, bugun", "Если продать в банках сегодня", "If you sold at banks today"), style = VsType.Meta, color = VsInkSoft)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                s.total.roundToLong().formatUzsL(), style = VsType.Hero.copy(fontSize = if (s.total >= 1e9) 34.sp else 42.sp),
                color = VsEmeraldDeep, maxLines = 1, modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(8.dp))
            Text(unit, style = VsType.Body.copy(fontSize = 18.sp), color = VsInkSoft, modifier = Modifier.padding(bottom = 6.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val d = s.dayChange.roundToLong()
            VsStat(
                label = pick(lang, "Kecha bilan farq", "За день", "Since yesterday"),
                value = (if (d > 0) "+" else "") + d.formatUzsL(),
                delta = pick(lang, "MB kursi boʻyicha", "по курсу ЦБ", "at CBU rate"),
                deltaColor = VsInkSoft,
                modifier = Modifier.weight(1f)
            )
            VsStat(
                label = pick(lang, "Dollarda", "В долларах", "In dollars"),
                value = s.totalUsd?.let { "$" + it.roundToLong().formatUzsL() } ?: "—",
                delta = pick(lang, "MB kursi boʻyicha", "по курсу ЦБ", "at CBU rate"),
                deltaColor = VsInkSoft,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HoldingsPanel(s: WalletSummary, lang: Lang, onEdit: (Holding) -> Unit, onAdd: () -> Unit) {
    VsPanel {
        s.rows.forEachIndexed { i, r ->
            if (i > 0) VsDivider()
            HoldingRow(r, lang) { onEdit(r.holding) }
        }
        VsDivider()
        val src = remember { MutableInteractionSource() }
        Row(
            Modifier.fillMaxWidth().clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onAdd)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(VsEmeraldSoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Add, null, tint = VsEmeraldDeep, modifier = Modifier.size(20.dp))
            }
            Text(pick(lang, "Valyuta yoki oltin qoʻshish", "Добавить валюту или золото", "Add currency or gold"), style = VsType.BodyMedium, color = VsEmeraldDeep)
        }
    }
}

@Composable
private fun CodeBadge(code: WalletCode, size: Int = 40) {
    val gold = code == WalletCode.XAU
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(if (gold) Color(0xFFF2D58A) else VsGround),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (code == WalletCode.UZS) "S" else code.symbol,
            style = VsType.Value.copy(fontSize = if (code.symbol.length > 1 && code != WalletCode.UZS) 14.sp else 17.sp),
            color = if (gold) Color(0xFF6B4E0E) else VsInk
        )
    }
}

@Composable
private fun HoldingRow(r: HoldingValue, lang: Lang, onClick: () -> Unit) {
    val h = r.holding
    val src = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CodeBadge(h.code)
        Column(Modifier.weight(1f)) {
            val amountText = fmtAmount(h.amount, h.code.decimals) + " " + when (h.code) {
                WalletCode.UZS -> somUnit()
                WalletCode.XAU -> pick(lang, "g oltin", "г золота", "g gold")
                else -> h.code.code
            }
            Text(amountText, style = VsType.Value, color = VsInk, maxLines = 1)
            val meta = when {
                h.code == WalletCode.UZS -> pick(lang, "Naqd soʻm", "Наличные сумы", "Cash so'm")
                h.code == WalletCode.XAU && r.rate != null -> pick(lang, "MB qaytarib oladi", "ЦБ выкупает", "CBU buys back") + " · ${fmtUnitRate(r.rate)}"
                r.bank != null && r.rate != null -> "${bankShortName(r.bank)} · ${fmtUnitRate(r.rate)}"
                else -> pick(lang, "Kurs yuklanmoqda…", "Курс загружается…", "Loading rate…")
            }
            Text(meta, style = VsType.Meta, color = VsInkSoft, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(r.som?.roundToLong()?.formatUzsL() ?: "—", style = VsType.Value, color = VsInk, maxLines = 1)
            val d = r.dayChange.roundToLong()
            if (h.code != WalletCode.UZS && h.code != WalletCode.XAU && d != 0L) {
                Text(
                    (if (d > 0) "+" else "") + d.formatUzsL(),
                    style = VsType.Meta.copy(fontWeight = FontWeight.SemiBold), color = if (d < 0) VsRoseText else VsEmeraldDeep
                )
            }
        }
    }
}

/* ---------- exchange ---------- */

@Composable
private fun ExchangeCard(quotes: Map<WalletCode, Quote>, lang: Lang) {
    var from by remember { mutableStateOf(WalletCode.of(WalletStore.exchangeFrom())?.takeIf { it in WalletCode.exchangeable } ?: WalletCode.USD) }
    var to by remember { mutableStateOf(WalletCode.of(WalletStore.exchangeTo())?.takeIf { it in WalletCode.exchangeable } ?: WalletCode.EUR) }
    var amountText by remember { mutableStateOf(WalletStore.exchangeAmount()) }
    fun persist() = WalletStore.saveExchange(from.code, to.code, amountText)

    VsCardBox {
        Text(pick(lang, "Valyutani valyutaga almashtirish", "Обмен валюты на валюту", "Currency to currency"), style = VsType.Title, color = VsInk)
        Text(
            pick(
                lang,
                "Masalan, safarga yevro kerak, sizda dollar bor. Banklar toʻgʻridan-toʻgʻri almashtirmaydi: avval soʻmga sotasiz, keyin kerakli valyutani olasiz. Eng foydali yoʻlni hisoblaymiz.",
                "Например, для поездки нужны евро, а у вас доллары. Банки меняют через сумы: сначала продаёте, потом покупаете. Считаем самый выгодный путь.",
                "Say you need euros for a trip and hold dollars. Banks go through so'm: sell first, then buy. We find the cheapest route."
            ),
            style = VsType.Meta, color = VsInkSoft
        )
        Text(pick(lang, "Menda bor", "У меня есть", "I have"), style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 4.dp))
        CodeChips(WalletCode.exchangeable, from) { if (it == to) to = from; from = it; persist() }
        AmountInput(amountText, from, lang) { amountText = it; persist() }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pick(lang, "Menga kerak", "Мне нужно", "I need"), style = VsType.Meta, color = VsInkSoft, modifier = Modifier.weight(1f))
            VsIconButton(Icons.Outlined.SwapVert, pick(lang, "Almashtirish", "Поменять местами", "Swap"), onClick = {
                val f = from; from = to; to = f; persist()
            })
        }
        CodeChips(WalletCode.exchangeable, to) { if (it == from) from = to; to = it; persist() }

        val amount = parseAmount(amountText) ?: 0.0
        val x = exchange(from, to, amount, quotes)
        if (x == null) {
            Text(
                if (amount <= 0) pick(lang, "Summani kiriting", "Введите сумму", "Enter an amount")
                else pick(lang, "Kurslar yuklanmoqda…", "Курсы загружаются…", "Loading rates…"),
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            ExchangeResult(x, lang)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CodeChips(codes: List<WalletCode>, selected: WalletCode, onSelect: (WalletCode) -> Unit) {
    // Up to 6 fit in one row at equal widths; longer lists (the add sheet, with gold) wrap onto a second row.
    val fit = codes.size <= 6
    val lang = LocalLang.current
    val chip: @Composable (WalletCode) -> Unit = { c ->
            val on = c == selected
            val bg by animateColorAsState(if (on) VsEmerald else VsGround, Motion.color(), label = "chip")
            val src = remember { MutableInteractionSource() }
            Box(
                Modifier.then(if (fit) Modifier.fillMaxWidth() else Modifier).heightIn(min = 40.dp).clip(RoundedCornerShape(14.dp)).background(bg)
                    .clickable(interactionSource = src, indication = null, role = Role.Button) { onSelect(c) }
                    .padding(horizontal = if (fit) 2.dp else 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (c == WalletCode.XAU) pick(lang, "Oltin", "Золото", "Gold") else c.code, maxLines = 1, style = VsType.BodyMedium.copy(fontWeight = FontWeight.SemiBold), color = if (on) VsOnAccent else VsInk)
            }
    }
    if (fit) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            codes.forEach { c -> Box(Modifier.weight(1f)) { chip(c) } }
        }
    } else {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            codes.forEach { c -> chip(c) }
        }
    }
}

@Composable
private fun AmountInput(text: String, code: WalletCode, lang: Lang, onChange: (String) -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val line by animateColorAsState(if (focused) VsEmerald else VsHair, Motion.color(), label = "line")
    val unitText = when (code) {
        WalletCode.UZS -> somUnit()
        WalletCode.XAU -> pick(lang, "gramm", "грамм", "grams")
        else -> code.code
    }
    Row(verticalAlignment = Alignment.Bottom) {
        BasicTextField(
            value = text,
            onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' }.take(14)) },
            singleLine = true,
            textStyle = VsType.Amount.copy(color = VsInk),
            cursorBrush = SolidColor(VsEmerald),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            interactionSource = src,
            modifier = Modifier.weight(1f).semantics { contentDescription = unitText },
            decorationBox = { inner ->
                Box(
                    Modifier.heightIn(min = 52.dp).drawWithContent {
                        drawContent()
                        val h = 2.dp.toPx()
                        drawRect(line, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
                    },
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) Text("0", style = VsType.Amount, color = VsInkSoft.copy(alpha = 0.5f))
                    inner()
                }
            }
        )
        Spacer(Modifier.width(10.dp))
        Text(unitText, style = VsType.Body.copy(fontSize = 16.sp), color = VsInkSoft, modifier = Modifier.padding(bottom = 12.dp))
    }
}

@Composable
private fun ExchangeResult(x: Exchange, lang: Lang) {
    val unit = somUnit()
    fun unitOf(c: WalletCode) = if (c == WalletCode.UZS) unit else c.code
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(18.dp)).background(VsEmeraldSoft).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(pick(lang, "Qoʻlingizga tegadi", "Вы получите", "You get"), style = VsType.Meta, color = VsEmeraldDeep)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(fmtAmount(x.result, x.to.decimals.coerceAtLeast(if (x.to == WalletCode.UZS) 0 else 2)), style = VsType.Display.copy(fontSize = 34.sp), color = VsEmeraldDeep, maxLines = 1)
            Spacer(Modifier.width(8.dp))
            Text(unitOf(x.to), style = VsType.Body.copy(fontSize = 17.sp), color = VsEmeraldDeep, modifier = Modifier.padding(bottom = 4.dp))
        }
        // Steps: where to sell, where to buy.
        if (x.sellBank != null && x.sellRate != null) {
            Step(
                1,
                pick(
                    lang,
                    "${bankShortName(x.sellBank)}: ${fmtAmount(x.amount, x.from.decimals)} ${x.from.code} sotasiz, kurs ${fmtUnitRate(x.sellRate)} → ${x.som.roundToLong().formatUzsL()} $unit",
                    "${bankShortName(x.sellBank)}: продаёте ${fmtAmount(x.amount, x.from.decimals)} ${x.from.code}, курс ${fmtUnitRate(x.sellRate)} → ${x.som.roundToLong().formatUzsL()} $unit",
                    "${bankShortName(x.sellBank)}: sell ${fmtAmount(x.amount, x.from.decimals)} ${x.from.code} at ${fmtUnitRate(x.sellRate)} → ${x.som.roundToLong().formatUzsL()} $unit"
                )
            )
        }
        if (x.buyBank != null && x.buyRate != null) {
            Step(
                if (x.sellBank != null) 2 else 1,
                pick(
                    lang,
                    "${bankShortName(x.buyBank)}: ${x.to.code} olasiz, kurs ${fmtUnitRate(x.buyRate)}",
                    "${bankShortName(x.buyBank)}: покупаете ${x.to.code}, курс ${fmtUnitRate(x.buyRate)}",
                    "${bankShortName(x.buyBank)}: buy ${x.to.code} at ${fmtUnitRate(x.buyRate)}"
                )
            )
        }
        val one = x.oneBankResult
        if (x.oneBank != null && one != null && x.sellBank != x.buyBank) {
            Text(
                pick(
                    lang,
                    "Ikkalasi bitta bankda (${bankShortName(x.oneBank)}): ${fmtAmount(one, 2)} ${unitOf(x.to)}, yaʼni ${fmtAmount(x.result - one, 2)} kam",
                    "Всё в одном банке (${bankShortName(x.oneBank)}): ${fmtAmount(one, 2)} ${unitOf(x.to)}, на ${fmtAmount(x.result - one, 2)} меньше",
                    "At one bank (${bankShortName(x.oneBank)}): ${fmtAmount(one, 2)} ${unitOf(x.to)}, ${fmtAmount(x.result - one, 2)} less"
                ),
                style = VsType.Meta, color = VsInk
            )
        }
        x.loss?.let { loss ->
            val pct = (loss * 100).fixed(1)
            Text(
                pick(
                    lang,
                    "MB kursi boʻyicha ${fmtAmount(x.cbuResult ?: 0.0, 2)} ${unitOf(x.to)} boʻlardi. Banklar farqi: $pct%",
                    "По курсу ЦБ было бы ${fmtAmount(x.cbuResult ?: 0.0, 2)} ${unitOf(x.to)}. Разница банков: $pct%",
                    "At the CBU rate: ${fmtAmount(x.cbuResult ?: 0.0, 2)} ${unitOf(x.to)}. Bank margin: $pct%"
                ),
                style = VsType.Meta, color = VsInkSoft
            )
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(VsEmerald), contentAlignment = Alignment.Center) {
            Text("$n", style = VsType.Meta.copy(fontWeight = FontWeight.Bold), color = VsOnAccent, textAlign = TextAlign.Center)
        }
        // Keep "11 995 000" on one line: the group spaces inside numbers become no-break spaces.
        Text(text.replace(Regex("(?<=\\d) (?=\\d)"), "\u00A0"), style = VsType.Body.copy(fontSize = 14.sp), color = VsInk, modifier = Modifier.weight(1f))
    }
}

/* ---------- add / edit sheet ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HoldingSheet(
    initial: Holding?,
    taken: Set<WalletCode>,
    lang: Lang,
    onDismiss: () -> Unit,
    onSave: (Holding) -> Unit,
    onDelete: (() -> Unit)?
) {
    var code by remember { mutableStateOf(initial?.code ?: WalletCode.entries.firstOrNull { it !in taken } ?: WalletCode.USD) }
    var text by remember { mutableStateOf(initial?.amount?.let { fmtAmount(it, initial.code.decimals).replace(" ", "") } ?: "") }
    val amount = parseAmount(text)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VsCard,
        shape = VsShape.Tray,
        tonalElevation = 0.dp,
        scrimColor = VsInk.copy(alpha = 0.42f),
        dragHandle = { VsSheetHandle() }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (initial == null) pick(lang, "Jamgʻarma qoʻshish", "Добавить сбережения", "Add savings")
                else pick(lang, "Oʻzgartirish", "Изменить", "Edit"),
                style = VsType.Title, color = VsInk
            )
            if (initial == null) {
                CodeChips(WalletCode.entries, code) { code = it }
                if (code in taken) {
                    Text(
                        pick(lang, "Bu valyuta bor: summa unga qoʻshiladi.", "Эта валюта уже есть: сумма прибавится.", "Already in the wallet: the amount is added to it."),
                        style = VsType.Meta, color = VsInkSoft
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CodeBadge(code, 32)
                    Text(code.displayName(lang), style = VsType.BodyMedium, color = VsInk)
                }
            }
            AmountInput(text, code, lang) { text = it }
            VsPrimaryButton(
                pick(lang, "Saqlash", "Сохранить", "Save"),
                onClick = { amount?.takeIf { it > 0 }?.let { onSave(Holding(code, it)) } },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
            if (onDelete != null) {
                val src = remember { MutableInteractionSource() }
                Text(
                    pick(lang, "Oʻchirish", "Удалить", "Remove"),
                    style = VsType.BodyMedium, color = VsRoseText, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onDelete).padding(vertical = 12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

/* ---------- goal ---------- */

@Composable
private fun GoalCard(goal: WalletGoal?, totalSom: Double, quotes: Map<WalletCode, Quote>, lang: Lang, onEdit: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    if (goal == null) {
        Row(
            Modifier.fillMaxWidth().vsDashedBorder().clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onEdit)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🎯", fontSize = 22.sp)
            Column(Modifier.weight(1f)) {
                Text(pick(lang, "Maqsad qoʻying", "Поставьте цель", "Set a goal"), style = VsType.BodyMedium, color = VsInk)
                Text(
                    pick(lang, "Masalan, mashina uchun 20 000 $: qancha yigʻilganini koʻrib borasiz", "Например, 20 000 $ на машину: видно, сколько уже собрано", "E.g. 20 000 $ for a car: see how much is already there"),
                    style = VsType.Meta, color = VsInkSoft
                )
            }
        }
        return
    }
    val p = goalProgress(goal, totalSom, quotes)
    val unit = if (goal.code == WalletCode.UZS) somUnit() else goal.code.code
    VsCardBox(Modifier.clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onEdit)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                goal.name.ifBlank { pick(lang, "Maqsad", "Цель", "Goal") },
                style = VsType.Title.copy(fontSize = 18.sp), color = VsInk, modifier = Modifier.weight(1f), maxLines = 1
            )
            Text(p?.let { "${(it.share * 100).roundToLong()}%" } ?: "—", style = VsType.Value, color = VsEmeraldDeep)
        }
        // Progress bar.
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(VsGround)) {
            Box(Modifier.fillMaxWidth((p?.share ?: 0.0).toFloat()).height(10.dp).clip(CircleShape).background(VsEmerald))
        }
        if (p != null) {
            Text(
                "${fmtAmount(p.saved, 0)} / ${fmtAmount(p.target, 0)} $unit",
                style = VsType.Meta, color = VsInkSoft
            )
            val left = p.target - p.saved
            Text(
                if (left <= 0) pick(lang, "Maqsadga yetdingiz! 🎉", "Цель достигнута! 🎉", "Goal reached! 🎉")
                else pick(lang, "Yana ${fmtAmount(left, 0)} $unit kerak", "Осталось ${fmtAmount(left, 0)} $unit", "${fmtAmount(left, 0)} $unit to go"),
                style = VsType.BodyMedium, color = if (left <= 0) VsEmeraldDeep else VsInk
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalSheet(goal: WalletGoal?, lang: Lang, onDismiss: () -> Unit, onSave: (WalletGoal?) -> Unit) {
    var code by remember { mutableStateOf(goal?.code ?: WalletCode.USD) }
    var text by remember { mutableStateOf(goal?.amount?.let { fmtAmount(it, 0).replace(" ", "") } ?: "") }
    var name by remember { mutableStateOf(goal?.name ?: "") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VsCard, shape = VsShape.Tray, tonalElevation = 0.dp,
        scrimColor = VsInk.copy(alpha = 0.42f), dragHandle = { VsSheetHandle() }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(pick(lang, "Maqsad", "Цель", "Goal"), style = VsType.Title, color = VsInk)
            // Name: one line, optional.
            val nsrc = remember { MutableInteractionSource() }
            val focused by nsrc.collectIsFocusedAsState()
            BasicTextField(
                value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                textStyle = VsType.Body.copy(color = VsInk, fontSize = 17.sp), cursorBrush = SolidColor(VsEmerald),
                interactionSource = nsrc,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(VsGround)
                    .border(1.dp, if (focused) VsEmerald else Color.Transparent, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (name.isEmpty()) Text(pick(lang, "Nima uchun? Masalan: Mashina", "На что? Например: Машина", "What for? E.g. Car"), style = VsType.Body.copy(fontSize = 17.sp), color = VsInkSoft)
                    inner()
                }
            )
            CodeChips(GOAL_CODES, code) { code = it }
            AmountInput(text, code, lang) { text = it }
            VsPrimaryButton(
                pick(lang, "Saqlash", "Сохранить", "Save"),
                onClick = { parseAmount(text)?.takeIf { it > 0 }?.let { onSave(WalletGoal(code, it, name.trim())) } },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
            if (goal != null) {
                val src = remember { MutableInteractionSource() }
                Text(
                    pick(lang, "Maqsadni oʻchirish", "Удалить цель", "Remove goal"),
                    style = VsType.BodyMedium, color = VsRoseText, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(interactionSource = src, indication = null, role = Role.Button) { onSave(null) }.padding(vertical = 12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

/* ---------- trend ---------- */

@Composable
private fun TrendCard(holdings: List<Holding>, histories: Map<WalletCode, List<HistoryPoint>>, quotes: Map<WalletCode, Quote>, lang: Lang) {
    var range by remember { mutableStateOf(appStore().getString("wallet_range")?.toIntOrNull() ?: 0) }
    val days = if (range == 0) 30 else 365
    val today = todayIso()
    val since = remember(today, days) { LocalDate.parse(today).minus(days, DateTimeUnit.DAY).toString() }
    val pts = remember(holdings, histories, quotes, since, today) { walletTrend(holdings, histories, quotes, since, today) }
    val unit = somUnit()
    VsCardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pick(lang, "Jamgʻarma dinamikasi", "Динамика сбережений", "Savings over time"), style = VsType.Title.copy(fontSize = 18.sp), color = VsInk, modifier = Modifier.weight(1f))
            SegmentedHug(
                listOf(pick(lang, "30 kun", "30 дн", "30 d"), pick(lang, "1 yil", "1 год", "1 y")), range,
                onSelect = { range = it; appStore().putString("wallet_range", it.toString()) }, height = 34.dp
            )
        }
        if (pts.size < 2) {
            Text(pick(lang, "Tarix yuklanmoqda…", "История загружается…", "Loading history…"), style = VsType.Meta, color = VsInkSoft)
            return@VsCardBox
        }
        val first = pts.first().rate
        val last = pts.last().rate
        val change = last - first
        val pct = if (first > 0) change / first * 100 else 0.0
        val up = change >= 0
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                (if (change.roundToLong() > 0) "+" else "") + change.roundToLong().formatUzsL() + " $unit",
                style = VsType.Value.copy(fontSize = 22.sp), color = if (up) VsEmeraldDeep else VsRoseText
            )
            Spacer(Modifier.width(8.dp))
            Text((if (pct > 0) "+" else "") + pct.fixed(1) + "%", style = VsType.Meta.copy(fontWeight = FontWeight.SemiBold), color = if (up) VsEmeraldDeep else VsRoseText, modifier = Modifier.padding(bottom = 3.dp))
        }
        val line = if (up) VsEmerald else VsRoseText
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val min = pts.minOf { it.rate }
            val max = pts.maxOf { it.rate }
            val span = (max - min).takeIf { it > 0 } ?: 1.0
            val w = size.width
            val h = size.height
            val pad = 6.dp.toPx()
            fun x(i: Int) = w * i / (pts.size - 1).toFloat()
            fun y(v: Double) = (pad + (h - 2 * pad) * (1 - (v - min) / span)).toFloat()
            val path = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(x(i), y(p.rate)) else lineTo(x(i), y(p.rate)) } }
            val fill = Path().apply { addPath(path); lineTo(w, h); lineTo(0f, h); close() }
            drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.18f), line.copy(alpha = 0f))))
            drawPath(path, line, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(line, 4.dp.toPx(), androidx.compose.ui.geometry.Offset(w, y(last)))
        }
        Row {
            Text(fmtDay(pts.first().date), style = VsType.Meta, color = VsInkSoft, modifier = Modifier.weight(1f))
            Text(pick(lang, "bugun", "сегодня", "today"), style = VsType.Meta, color = VsInkSoft)
        }
        Text(
            pick(
                lang,
                "Hozirgi jamgʻarmangiz MB kursi boʻyicha: valyuta kursi oʻzgarishi qiymatga qanday taʼsir qilgani.",
                "Ваши текущие сбережения по курсу ЦБ: как изменение курса повлияло на их стоимость.",
                "Your current savings at CBU rates: how rate moves changed their value."
            ),
            style = VsType.Meta, color = VsInkSoft
        )
    }
}

/** "2026-10-22" -> "22.10.2026". */
private fun fmtDay(iso: String): String = iso.split('-').takeIf { it.size == 3 }?.let { "${it[2]}.${it[1]}.${it[0]}" } ?: iso
