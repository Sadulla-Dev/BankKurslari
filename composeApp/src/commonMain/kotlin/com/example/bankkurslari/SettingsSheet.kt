package com.example.bankkurslari

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsHair
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsType

/** Uzbek city names (UZ, RU). The UZ name is the stored key. */
val UZ_CITIES: List<Pair<String, String>> = listOf(
    "Toshkent" to "Ташкент",
    "Samarqand" to "Самарканд",
    "Buxoro" to "Бухара",
    "Andijon" to "Андижан",
    "Namangan" to "Наманган",
    "Fargʻona" to "Фергана",
    "Qarshi" to "Карши",
    "Nukus" to "Нукус",
    "Urganch" to "Ургенч",
    "Navoiy" to "Навои",
    "Jizzax" to "Джизак",
    "Guliston" to "Гулистан",
    "Termiz" to "Термез"
)

/**
 * Settings: a full screen on the ground colour (back button, big title, grouped cards).
 * Language opens a bottom sheet, the default amount a small tray. Sits above the whole app, so the tab bar is covered.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    lang: Lang,
    onLang: (Lang) -> Unit,
    themeMode: Int, // 0 system, 1 light, 2 dark
    onThemeMode: (Int) -> Unit,
    defaultCurrency: Currency,
    onDefaultCurrency: (Currency) -> Unit,
    amountText: String,
    onAmount: (String) -> Unit,
    onLocationGranted: () -> Unit,
    onOpenAlerts: () -> Unit
) {
    BackHandler(enabled = true, onBack = onDismiss)
    var city by remember { mutableStateOf(RateService.city()) }
    var cityMenu by remember { mutableStateOf(false) }
    var routeApp by remember { mutableStateOf(rememberedMapApp()) }
    var routeMenu by remember { mutableStateOf(false) }
    var sourcesOpen by remember { mutableStateOf(false) }
    var langSheet by remember { mutableStateOf(false) }
    var amountSheet by remember { mutableStateOf(false) }
    var locationOn by remember { mutableStateOf(hasLocationPermission()) }
    val askLocation = rememberLocationPermissionRequest { ok -> locationOn = ok; if (ok) onLocationGranted() }
    val alertCount = remember { Alerts.load().size }
    val ru = lang == Lang.RU
    val amount = amountText.filter { it.isDigit() }.toLongOrNull()

    Column(
        Modifier.fillMaxSize().background(VsBg)
            // Swallow touches so nothing underneath reacts while Settings is open.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()
        ) {
            Spacer(Modifier.height(8.dp))
            VsIconButton(Icons.AutoMirrored.Outlined.ArrowBack, tr("Orqaga", "Назад", "Back"), onDismiss)
            Text(
                tr("Sozlamalar", "Настройки", "Settings"),
                style = VsType.Display.copy(fontSize = 34.sp, lineHeight = 40.sp), color = VsInk,
                modifier = Modifier.padding(top = 20.dp, start = 4.dp)
            )

            // ---- Siz uchun
            SectionLabel(tr("Siz uchun", "Для вас", "For you"))
            VsPanel {
                Box {
                    ValueRow(
                        title = tr("Shahar", "Город", "City"),
                        value = UZ_CITIES.firstOrNull { it.first == city }?.let { pick(lang, it.first, it.second, it.first) }
                            ?: tr("Tanlanmagan", "Не выбран", "Not selected"),
                        onClick = { cityMenu = true }
                    )
                    DropdownMenu(expanded = cityMenu, onDismissRequest = { cityMenu = false }, containerColor = VsCard, shape = VsShape.Panel) {
                        UZ_CITIES.forEach { (uz, rus) ->
                            DropdownMenuItem(
                                text = { Text(pick(lang, uz, rus, uz), style = VsType.Body, color = if (uz == city) VsEmerald else VsInk) },
                                trailingIcon = { if (uz == city) Icon(Icons.Outlined.Check, null, tint = VsEmerald) },
                                onClick = { city = uz; RateService.setCity(uz); cityMenu = false },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
                VsDivider()
                ValueRow(title = tr("Til", "Язык", "Language"), value = lang.label, onClick = { langSheet = true })
                VsDivider()
                RowShell(title = tr("Asosiy valyuta", "Основная валюта", "Main currency"), subtitle = null) {
                    SegmentedHug(
                        options = Currency.entries.map { it.code },
                        selected = defaultCurrency.ordinal,
                        onSelect = { onDefaultCurrency(Currency.entries[it]) },
                        height = 36.dp
                    )
                }
                VsDivider()
                ValueRow(
                    title = tr("Odatiy summa", "Сумма по умолчанию", "Default amount"),
                    subtitle = tr("Kurs ekranida avval koʻrsatiladi", "Сначала показывается на экране курса", "Shown first on the Rate screen"),
                    value = amount?.takeIf { it > 0 }?.let { kGroup(it) } ?: "—",
                    onClick = { amountSheet = true }
                )
            }

            // ---- Koʻrinish
            SectionLabel(tr("Koʻrinish", "Оформление", "Appearance"))
            VsPanel {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr("Mavzu", "Тема", "Theme"), style = VsType.Body.copy(fontWeight = FontWeight.Medium), color = VsInk)
                    SettingsSegmented(
                        options = listOf(tr("Tizim", "Система", "System"), tr("Yorugʻ", "Светлая", "Light"), tr("Qorongʻi", "Тёмная", "Dark")),
                        selected = themeMode.coerceIn(0, 2),
                        onSelect = onThemeMode
                    )
                }
            }

            // ---- Xabarnomalar
            SectionLabel(tr("Xabarnomalar", "Уведомления", "Notifications"))
            VsPanel {
                ValueRow(
                    title = tr("Ogohlantirishlar", "Оповещения", "Alerts"),
                    subtitle = tr("Kurs kerakli qiymatga yetganda", "Когда курс достигнет нужного значения", "When the rate reaches your target"),
                    value = if (alertCount > 0) tr("$alertCount ta", "$alertCount шт.", "$alertCount") else "",
                    onClick = onOpenAlerts
                )
            }

            // ---- Xarita
            SectionLabel(tr("Xarita", "Карта", "Map"))
            VsPanel {
                RowShell(
                    title = tr("Joylashuv", "Геолокация", "Location"),
                    subtitle = tr("Masofa va eng yaqin filial uchun", "Для расстояния и ближайшего филиала", "For distance and the nearest branch")
                ) {
                    if (locationOn) {
                        Text(tr("Yoqilgan", "Включена", "On"), style = VsType.Body.copy(fontWeight = FontWeight.Medium), color = VsInkSoft)
                    } else {
                        Box(
                            Modifier.heightIn(min = 44.dp).clip(VsShape.Pill).clickable(role = Role.Button) { askLocation() }.padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tr("Yoqish", "Включить", "Turn on"), style = VsType.Body.copy(fontWeight = FontWeight.SemiBold), color = VsEmerald)
                        }
                    }
                }
                VsDivider()
                Box {
                    ValueRow(
                        title = tr("Marshrut ilovasi", "Приложение для маршрута", "Directions app"),
                        value = mapAppLabel(routeApp, ru),
                        onClick = { routeMenu = true }
                    )
                    DropdownMenu(expanded = routeMenu, onDismissRequest = { routeMenu = false }, containerColor = VsCard, shape = VsShape.Panel) {
                        (listOf<MapApp?>(null) + MapApp.entries).forEach { app ->
                            DropdownMenuItem(
                                text = { Text(mapAppLabel(app, ru), style = VsType.Body, color = if (app == routeApp) VsEmerald else VsInk) },
                                trailingIcon = { if (app == routeApp) Icon(Icons.Outlined.Check, null, tint = VsEmerald) },
                                onClick = { routeApp = app; setRememberedMapApp(app); routeMenu = false },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            }

            // ---- Haqida
            SectionLabel(tr("Haqida", "О приложении", "About"))
            VsPanel {
                ValueRow(title = tr("Maʼlumot manbalari", "Источники данных", "Data sources"), value = "", onClick = { sourcesOpen = !sourcesOpen })
                if (sourcesOpen) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            tr(
                                "Tijorat banklari kurslari: bank.uz. Markaziy bank kursi va tarixi: cbu.uz. Xarita: OpenStreetMap va Esri.",
                                "Курсы коммерческих банков: bank.uz. Курс и история ЦБ: cbu.uz. Карта: OpenStreetMap и Esri."
                            ),
                            style = VsType.Meta, color = VsInkSoft
                        )
                        Text(
                            tr(
                                "Yangilanish: ilova ochiq paytda kamida 5 daqiqada bir marta. Fonda taxminan har 15–30 daqiqada. Har bir kursda yangilanish vaqti koʻrsatiladi.",
                                "Обновление: при открытом приложении не чаще раза в 5 минут. В фоне примерно раз в 15–30 минут. Для каждого курса указано время обновления."
                            ),
                            style = VsType.Meta, color = VsInkSoft
                        )
                    }
                }
                VsDivider()
                ValueRow(
                    title = tr("Fikr yuborish", "Обратная связь", "Send feedback"), value = "",
                    onClick = { openUri("mailto:?subject=Qulay%20Kurs") }
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (langSheet) LanguageSheet(lang, onLang = { onLang(it); langSheet = false }, onDismiss = { langSheet = false })
    if (amountSheet) AmountSheet(
        initial = amount?.toString().orEmpty(),
        onSave = { onAmount(it); amountSheet = false },
        onDismiss = { amountSheet = false }
    )
}

/* ---------- rows ---------- */

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, style = VsType.Meta.copy(fontWeight = FontWeight.Medium), color = VsInkSoft,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp)
    )
}

/** Title (+ optional subtitle) on the left, [trailing] on the right. Not clickable by itself. */
@Composable
private fun RowShell(title: String, subtitle: String?, modifier: Modifier = Modifier, trailing: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = VsType.Body.copy(fontWeight = FontWeight.Medium), color = VsInk)
            if (subtitle != null) Text(subtitle, style = VsType.Meta, color = VsInkSoft)
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

/** Row with a grey value and a chevron; the whole row is the tap target. */
@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit, subtitle: String? = null) {
    RowShell(title, subtitle, Modifier.clickable(role = Role.Button, onClick = onClick)) {
        if (value.isNotEmpty()) {
            Text(value, style = VsType.Body, color = VsInkSoft, maxLines = 1, modifier = Modifier.widthIn(max = 180.dp))
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = VsInkSoft, modifier = Modifier.size(18.dp))
    }
}

/* ---------- language sheet ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSheet(lang: Lang, onLang: (Lang) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = VsShape.Tray, containerColor = VsCard, contentColor = VsInk, tonalElevation = 0.dp,
        scrimColor = VsInk.copy(alpha = 0.42f),
        dragHandle = { VsSheetHandle() }
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Til", "Язык", "Language"), style = VsType.Title, color = VsInk, modifier = Modifier.weight(1f))
                VsIconButton(Icons.Outlined.Close, tr("Yopish", "Закрыть", "Close"), onDismiss)
            }
            Column(Modifier.fillMaxWidth().clip(VsShape.Panel).border(1.dp, VsHair, VsShape.Panel)) {
                Lang.entries.forEachIndexed { i, l ->
                    if (i > 0) VsDivider()
                    val sel = l == lang
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .clickable { onLang(l) }
                            .semantics { role = Role.RadioButton; this.selected = sel }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                l.label, color = if (sel) VsEmerald else VsInk,
                                style = if (sel) VsType.Body.copy(fontWeight = FontWeight.SemiBold) else VsType.Body
                            )
                            when (l) {
                                Lang.UZ -> Text("lotin", style = VsType.Meta, color = VsInkSoft)
                                Lang.UZ_CYRL -> Text("кирилл", style = VsType.Meta, color = VsInkSoft)
                                else -> {}
                            }
                        }
                        if (sel) Icon(Icons.Outlined.Check, null, tint = VsEmerald, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Text(
                tr("Bank nomlari va manzillar oʻzgarmaydi.", "Названия банков и адреса не переводятся.", "Bank names and addresses are not translated."),
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/* ---------- default amount tray ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountSheet(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = VsShape.Tray, containerColor = VsCard, contentColor = VsInk, tonalElevation = 0.dp,
        scrimColor = VsInk.copy(alpha = 0.42f),
        dragHandle = { VsSheetHandle() }
    ) {
        Column(Modifier.padding(horizontal = 20.dp).imePadding().navigationBarsPadding().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Odatiy summa", "Сумма по умолчанию", "Default amount"), style = VsType.Title, color = VsInk, modifier = Modifier.weight(1f))
                VsIconButton(Icons.Outlined.Close, tr("Yopish", "Закрыть", "Close"), onDismiss)
            }
            Text(
                tr("Kurs ekrani shu summa bilan ochiladi.", "Экран курса откроется с этой суммой.", "The Rate screen opens with this amount."),
                style = VsType.Meta, color = VsInkSoft
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() }.trimStart('0').take(9) },
                singleLine = true,
                textStyle = VsType.Amount.copy(color = VsInk),
                cursorBrush = SolidColor(VsEmerald),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                decorationBox = { inner ->
                    Column {
                        Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) Text("100", style = VsType.Amount, color = VsInkSoft.copy(alpha = 0.5f))
                            inner()
                        }
                        Box(Modifier.fillMaxWidth().height(2.dp).background(VsEmerald))
                    }
                }
            )
            VsPrimaryButton(
                text = tr("Saqlash", "Сохранить", "Save"),
                onClick = { onSave(text.ifEmpty { "100" }) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingsSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) =
    Segmented(options, selected, onSelect)
