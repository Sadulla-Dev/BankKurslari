package com.example.bankkurslari

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsGround
import com.example.bankkurslari.ui.theme.VsHair
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Reports about a map place ("no branch here", wrong phone…) and general feedback. Sent to bot.qulaykurs.uz/report
 * (backend/bot/worker.js), which forwards each one to the owner's Telegram chat. Anonymous: only what the user typed,
 * the place they report on, the platform, app version and language.
 */
data class ReportPlace(val bank: String, val address: String?, val phone: String?, val lat: Double?, val lng: Double?)

private const val REPORT_URL = "https://bot.qulaykurs.uz/report"

private suspend fun sendReport(kind: String, text: String, place: ReportPlace?, lang: Lang): Boolean = runCatching {
    val body = buildJsonObject {
        put("p", usagePlatform)
        put("kind", kind)
        put("text", text.trim())
        put("lang", lang.code)
        put("v", appVersion)
        if (place != null) put("place", buildJsonObject {
            put("bank", place.bank)
            place.address?.let { put("address", it) }
            place.phone?.let { put("phone", it) }
            place.lat?.let { put("lat", JsonPrimitive(it)) }
            place.lng?.let { put("lng", JsonPrimitive(it)) }
        })
    }
    httpPostJson(REPORT_URL, body.toString(), 10_000)
}.isSuccess

/** Small teal "Report a problem" link under a place's actions. */
@Composable
fun ReportLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        tr("Xato haqida xabar berish", "Сообщить об ошибке", "Report a problem"),
        style = VsType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = VsEmerald,
        modifier = modifier.clip(VsShape.Pill).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 10.dp)
    )
}

/** A branch's numbers ("+998 71 …, 1296"), one per line; tapping one dials it. */
@Composable
fun PhoneLines(phone: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        phone.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { n ->
            Text(
                n, style = VsType.Meta.copy(fontWeight = FontWeight.Medium), color = VsEmerald,
                modifier = Modifier.clip(VsShape.Pill)
                    .clickable(role = Role.Button) { Usage.track("call"); dialPhone(n) }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

/** [place] set: report on that place (reason chips + optional comment). Null: free-text feedback about the app. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportSheet(place: ReportPlace?, onDismiss: () -> Unit) {
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    val reasons = if (place == null) emptyList() else listOf(
        "no_place" to tr("Bu joyda filial yoʻq", "Здесь нет филиала", "No branch here"),
        "wrong_address" to tr("Manzil notoʻgʻri", "Неверный адрес", "Wrong address"),
        "wrong_phone" to tr("Telefon notoʻgʻri", "Неверный телефон", "Wrong phone"),
        "wrong_hours" to tr("Ish vaqti notoʻgʻri", "Неверные часы работы", "Wrong opening hours"),
        "wrong_rate" to tr("Kurs notoʻgʻri", "Неверный курс", "Wrong rate"),
        "other" to tr("Boshqa", "Другое", "Other")
    )
    var kind by remember { mutableStateOf(if (place == null) "feedback" else "") }
    var text by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(0) } // 0 editing, 1 sending, 2 sent, 3 failed
    val needsText = kind == "feedback" || kind == "other"
    val canSend = state != 1 && kind.isNotEmpty() && (!needsText || text.isNotBlank())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = VsShape.Tray, containerColor = VsCard, contentColor = VsInk, tonalElevation = 0.dp,
        dragHandle = { VsSheetHandle() }
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).imePadding().navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (place == null) tr("Fikr-taklif", "Отзыв и предложения", "Feedback") else tr("Xato haqida xabar", "Сообщить об ошибке", "Report a problem"),
                    style = VsType.Title, color = VsInk, modifier = Modifier.weight(1f)
                )
                VsIconButton(Icons.Outlined.Close, tr("Yopish", "Закрыть", "Close"), onDismiss)
            }
            if (state == 2) {
                Text(
                    tr("Rahmat! Xabaringiz yuborildi.", "Спасибо! Сообщение отправлено.", "Thank you! Your message was sent."),
                    style = VsType.Body, color = VsInk
                )
                VsPrimaryButton(tr("Yopish", "Закрыть", "Close"), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                return@Column
            }
            if (place != null) {
                Text(listOfNotNull(place.bank, place.address).joinToString(" · "), style = VsType.Meta, color = VsInkSoft)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    reasons.forEach { (k, label) -> VsChip(label, selected = kind == k, onClick = { kind = k }) }
                }
            } else {
                Text(
                    tr("Ilova haqida fikringiz yoki taklifingizni yozing.", "Напишите отзыв или предложение о приложении.", "Tell us what you think or what to add."),
                    style = VsType.Meta, color = VsInkSoft
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(1500) },
                textStyle = VsType.Body.copy(color = VsInk),
                cursorBrush = SolidColor(VsEmerald),
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth().clip(VsShape.Button).background(VsGround).border(1.dp, VsHair, VsShape.Button),
                decorationBox = { inner ->
                    Box(Modifier.heightIn(min = 88.dp).padding(14.dp)) {
                        if (text.isEmpty()) Text(
                            if (needsText) tr("Xabaringiz…", "Ваше сообщение…", "Your message…")
                            else tr("Izoh (ixtiyoriy)", "Комментарий (необязательно)", "Comment (optional)"),
                            style = VsType.Body, color = VsInkSoft
                        )
                        inner()
                    }
                }
            )
            if (state == 3) Text(
                tr("Yuborilmadi. Internetni tekshirib, qayta urinib koʻring.", "Не отправлено. Проверьте интернет и попробуйте снова.", "Not sent. Check your connection and try again."),
                style = VsType.Meta, color = VsInkSoft
            )
            VsPrimaryButton(
                if (state == 1) tr("Yuborilmoqda…", "Отправка…", "Sending…") else tr("Yuborish", "Отправить", "Send"),
                onClick = {
                    if (!canSend) return@VsPrimaryButton
                    state = 1
                    scope.launch {
                        val ok = sendReport(kind, text, place, lang)
                        if (ok) Usage.track(if (place == null) "feedback_sent" else "report_sent")
                        state = if (ok) 2 else 3
                    }
                },
                modifier = Modifier.fillMaxWidth().alpha(if (canSend) 1f else 0.4f)
            )
        }
    }
}
