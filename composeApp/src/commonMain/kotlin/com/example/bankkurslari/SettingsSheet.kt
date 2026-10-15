package com.example.bankkurslari

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsLine
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

/** Settings sheet (spec 6.5). Language is limited to UZ / RU for now. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    lang: Lang,
    onLang: (Lang) -> Unit,
    themeMode: Int, // 0 system, 1 light, 2 dark
    onThemeMode: (Int) -> Unit,
    defaultCurrency: Currency,
    onDefaultCurrency: (Currency) -> Unit,
    onOpenAlerts: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var city by remember { mutableStateOf(RateService.city()) }
    var cityMenu by remember { mutableStateOf(false) }
    var sourcesOpen by remember { mutableStateOf(false) }
    val alertCount = remember { Alerts.load().size }
    val ru = lang == Lang.RU

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = VsBg,
        contentColor = VsInk,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(tr("Sozlamalar", "Настройки"), style = VsType.Title, color = VsInk, modifier = Modifier.heightIn(min = 48.dp).padding(top = 8.dp))

            // Til
            SettingsLabel(tr("Til", "Язык"))
            LangRow("Oʻzbekcha", lang == Lang.UZ) { onLang(Lang.UZ) }
            LangRow("Русский", lang == Lang.RU) { onLang(Lang.RU) }
            HorizontalDivider(color = VsLine)

            // Mavzu
            SettingsLabel(tr("Mavzu", "Тема"))
            SettingsSegmented(
                options = listOf(tr("Tizim", "Система"), tr("Yorugʻ", "Светлая"), tr("Qorongʻi", "Тёмная")),
                selected = themeMode.coerceIn(0, 2),
                onSelect = onThemeMode
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = VsLine)

            // Standart valyuta
            SettingsLabel(tr("Standart valyuta", "Валюта по умолчанию"))
            SettingsSegmented(
                options = Currency.entries.map { it.code },
                selected = defaultCurrency.ordinal,
                onSelect = { onDefaultCurrency(Currency.entries[it]) }
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = VsLine)

            // Shahar
            Box {
                SettingsRow(
                    title = tr("Shahar", "Город"),
                    value = UZ_CITIES.firstOrNull { it.first == city }?.let { if (ru) it.second else it.first }
                        ?: tr("Tanlanmagan", "Не выбран"),
                    onClick = { cityMenu = true }
                )
                DropdownMenu(expanded = cityMenu, onDismissRequest = { cityMenu = false }, containerColor = VsBg) {
                    UZ_CITIES.forEach { (uz, rus) ->
                        DropdownMenuItem(
                            text = { Text(if (ru) rus else uz, style = VsType.Body, color = if (uz == city) VsEmerald else VsInk) },
                            onClick = {
                                city = uz
                                RateService.setCity(uz)
                                cityMenu = false
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = VsLine)

            // Marshrut ilovasi: ask every time (default) or always use one app
            var routeApp by remember { mutableStateOf(rememberedMapApp()) }
            var routeMenu by remember { mutableStateOf(false) }
            Box {
                SettingsRow(
                    title = tr("Marshrut ilovasi", "Приложение для маршрута"),
                    value = mapAppLabel(routeApp, ru),
                    onClick = { routeMenu = true }
                )
                DropdownMenu(expanded = routeMenu, onDismissRequest = { routeMenu = false }, containerColor = VsBg) {
                    (listOf<MapApp?>(null) + MapApp.entries).forEach { app ->
                        DropdownMenuItem(
                            text = { Text(mapAppLabel(app, ru), style = VsType.Body, color = if (app == routeApp) VsEmerald else VsInk) },
                            onClick = { routeApp = app; setRememberedMapApp(app); routeMenu = false },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = VsLine)

            // Ogohlantirishlar
            SettingsRow(
                title = tr("Ogohlantirishlar", "Оповещения"),
                value = if (alertCount > 0) tr("$alertCount ta", "$alertCount шт.") else "",
                onClick = onOpenAlerts
            )
            HorizontalDivider(color = VsLine)

            // Manbalar
            SettingsRow(
                title = tr("Maʻlumot manbalari va yangilanish tartibi", "Источники данных и порядок обновления"),
                value = "",
                onClick = { sourcesOpen = !sourcesOpen }
            )
            if (sourcesOpen) {
                Column(modifier = Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            HorizontalDivider(color = VsLine)

            // Fikr bildirish
            SettingsRow(
                title = tr("Fikr bildirish", "Обратная связь"),
                value = "",
                onClick = {
                    openUri("mailto:?subject=Qulay%20Kurs")
                }
            )
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text, style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
private fun LangRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics { role = Role.RadioButton; this.selected = selected },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = VsEmerald, unselectedColor = VsInkSoft)
        )
        Text(label, style = VsType.Body, color = VsInk, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SettingsRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = VsType.Body, color = VsInk, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(value, style = VsType.Body, color = VsInkSoft, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
fun SettingsSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) =
    Segmented(options, selected, onSelect)
