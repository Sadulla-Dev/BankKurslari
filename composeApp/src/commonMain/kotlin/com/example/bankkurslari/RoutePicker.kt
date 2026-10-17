package com.example.bankkurslari

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsType

/** Where the user wants to go. */
data class RouteTarget(val lat: Double, val lng: Double, val name: String)

/**
 * A navigation app we can hand a route to. [app] is the deep link (Android, and iOS unless [iosApp] overrides it),
 * [web] the browser fallback. [androidPkg] / [iosScheme] are used to detect whether it is installed.
 */
enum class MapApp(
    val label: String,
    val androidPkg: String?,
    val iosScheme: String?,
    val app: (RouteTarget) -> String,
    val iosApp: ((RouteTarget) -> String)?,
    val web: ((RouteTarget) -> String)?
) {
    YANDEX_MAPS(
        "Yandex Maps", "ru.yandex.yandexmaps", "yandexmaps",
        { "yandexmaps://maps.yandex.ru/?rtext=~${it.lat},${it.lng}&rtt=auto" }, null,
        { "https://yandex.uz/maps/?rtext=~${it.lat},${it.lng}&rtt=auto" }
    ),
    YANDEX_NAVI(
        "Yandex Navigator", "ru.yandex.yandexnavi", "yandexnavi",
        { "yandexnavi://build_route_on_map?lat_to=${it.lat}&lon_to=${it.lng}" }, null,
        null
    ),
    TWO_GIS(
        "2GIS", "ru.dublgis.dgismobile", "dgis",
        { "dgis://2gis.ru/routeSearch/rsType/car/to/${it.lng},${it.lat}" }, null,
        { "https://2gis.uz/routeSearch/rsType/car/to/${it.lng},${it.lat}" }
    ),
    GOOGLE_MAPS(
        "Google Maps", "com.google.android.apps.maps", "comgooglemaps",
        { "https://www.google.com/maps/dir/?api=1&destination=${it.lat},${it.lng}" },
        { "comgooglemaps://?daddr=${it.lat},${it.lng}&directionsmode=driving" },
        { "https://www.google.com/maps/dir/?api=1&destination=${it.lat},${it.lng}" }
    ),
    WAZE(
        "Waze", "com.waze", "waze",
        { "https://waze.com/ul?ll=${it.lat},${it.lng}&navigate=yes" },
        { "waze://?ll=${it.lat},${it.lng}&navigate=yes" },
        { "https://waze.com/ul?ll=${it.lat},${it.lng}&navigate=yes" }
    ),
    /** Android: any geo: handler via the system chooser. iOS: Apple Maps. */
    OTHER("", null, null, { "" }, null, null);
}

private const val KEY = "route_app"

fun rememberedMapApp(): MapApp? =
    appStore().getString(KEY)?.let { n -> MapApp.entries.firstOrNull { it.name == n } }

fun setRememberedMapApp(app: MapApp?) = appStore().putString(KEY, app?.name)

/** Opens [target] in [app]: the app itself when installed, otherwise its web version. Returns false if nothing could open it. */
fun launchRoute(app: MapApp, target: RouteTarget): Boolean {
    if (app == MapApp.OTHER) return launchMapApp(app, target)
    if (isMapAppInstalled(app) && launchMapApp(app, target)) return true
    val web = app.web?.invoke(target) ?: return false
    return openUri(web)
}

/** Call with a target; asks which map app to use unless the user chose "always use". */
val LocalRoute = staticCompositionLocalOf<(RouteTarget) -> Unit> { {} }

/** Provides [LocalRoute] to [content] and hosts the picker sheet. */
@Composable
fun RouteHost(content: @Composable () -> Unit) {
    var pending by remember { mutableStateOf<RouteTarget?>(null) }
    val request: (RouteTarget) -> Unit = remember {
        { t ->
            val saved = rememberedMapApp()
            if (saved == null || !launchRoute(saved, t)) pending = t
        }
    }
    CompositionLocalProvider(LocalRoute provides request) { content() }
    pending?.let { t -> RoutePickerSheet(t, onDismiss = { pending = null }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutePickerSheet(target: RouteTarget, onDismiss: () -> Unit) {
    var always by remember { mutableStateOf(false) }
    // Installed apps first; the rest open in the browser. Navigator has no web version -> only when installed.
    val apps = remember {
        val real = MapApp.entries.filter { it != MapApp.OTHER }
        val (inst, rest) = real.partition { isMapAppInstalled(it) }
        inst.map { it to true } + rest.filter { it.web != null }.map { it to false }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VsBg,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(tr("Qaysi xaritada ochamiz?", "Где построить маршрут?", "Open in which map?"), style = VsType.Title, color = VsInk)
            Text(target.name, style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            apps.forEach { (app, installed) ->
                PickerRow(
                    title = app.label,
                    meta = if (installed) tr("Oʻrnatilgan", "Установлено", "Installed") else tr("Brauzerda ochiladi", "Откроется в браузере", "Opens in the browser"),
                    trailingWeb = !installed
                ) {
                    if (always) setRememberedMapApp(app)
                    launchRoute(app, target)
                    onDismiss()
                }
                KHairline()
            }
            PickerRow(title = tr("Boshqa ilova…", "Другое приложение…", "Other app…"), meta = null, trailingWeb = false, more = true) {
                if (always) setRememberedMapApp(MapApp.OTHER)
                launchRoute(MapApp.OTHER, target)
                onDismiss()
            }
            Spacer(Modifier.height(8.dp))
            AlwaysToggle(always) { always = !always }
            Text(
                tr("Keyin Sozlamalarda oʻzgartirish mumkin.", "Можно изменить в настройках.", "You can change this later in Settings."),
                style = VsType.Meta, color = VsInkSoft, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun PickerRow(title: String, meta: String?, trailingWeb: Boolean, more: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = VsType.BodyMedium, color = VsInk)
            if (meta != null) Text(meta, style = VsType.Meta, color = VsInkSoft)
        }
        when {
            trailingWeb -> Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = VsInkSoft, modifier = Modifier.size(18.dp))
            more -> Icon(Icons.Outlined.MoreHoriz, null, tint = VsInkSoft, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AlwaysToggle(checked: Boolean, onToggle: () -> Unit) {
    val box by animateColorAsState(if (checked) VsEmerald else VsBg, Motion.color(), label = "chk")
    val edge by animateColorAsState(if (checked) VsEmerald else VsInkSoft, Motion.color(), label = "chke")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(onClick = onToggle)
            .semantics { role = Role.Checkbox; selected = checked },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(box).border(1.5.dp, edge, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) { if (checked) Icon(Icons.Outlined.Check, null, tint = VsBg, modifier = Modifier.size(14.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(tr("Har doim shu ilovada ochilsin", "Всегда открывать в этом приложении", "Always open in this app"), style = VsType.Body, color = VsInk)
    }
}

/** Settings row helper: current remembered app label, or null when the app asks every time. */
fun mapAppLabel(app: MapApp?, ru: Boolean): String = when (app) {
    null -> pick(lang, "Har safar soʻrash", "Спрашивать", "Ask every time")
    MapApp.OTHER -> pick(lang, "Tizim tanlovi", "Системный выбор", "System chooser")
    else -> app.label
}
