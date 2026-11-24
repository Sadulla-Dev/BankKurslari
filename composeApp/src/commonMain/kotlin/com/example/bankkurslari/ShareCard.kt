package com.example.bankkurslari

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsEmeraldSoft
import com.example.bankkurslari.ui.theme.VsGround
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsType

/** PNG bytes of a captured picture (Android: Bitmap.compress, iOS: Skia). */
expect fun ImageBitmap.encodePng(): ByteArray

/** Opens the system share sheet with a picture and a caption. Falls back to the caption alone if the picture fails. */
expect fun shareImage(png: ByteArray, text: String)

/** What the share picture says: the same facts as [kShareText], laid out to be read in a chat. */
class ShareCardData(
    val head: String,      // "1 000 USD sotsangiz"
    val total: String,     // "11 995 000"
    val unit: String,      // "soʻm"
    val bank: String,      // "Anorbank"
    val rateLine: String,  // "1 USD = 11 995 soʻm"
    val best: Boolean,     // shows the "best rate today" mark
    val time: String?,     // "24.11, 15:05"
    val caption: String    // text sent along with the picture (has the site link)
)

/** One pending share at a time; [ShareCardHost] draws it, captures it and clears it. */
object ShareCards {
    var pending by mutableStateOf<ShareCardData?>(null)

    fun share(data: ShareCardData) {
        Usage.track("share")
        pending = data
    }
}

private val CARD_W = 360.dp

/**
 * Put once near the root. When a share is pending it lays the card out at zero size (nothing moves), records its
 * drawing into a graphics layer without putting it on screen, turns that into a PNG and opens the share sheet.
 */
@Composable
fun ShareCardHost() {
    val data = ShareCards.pending ?: return
    val layer = rememberGraphicsLayer()
    Box(
        Modifier
            .layout { measurable, _ ->
                // Fixed width, height of the content: the picture has no empty margin below the card.
                val p = measurable.measure(Constraints.fixedWidth(CARD_W.roundToPx()))
                layout(0, 0) { p.place(0, 0) }
            }
            // Record only: the layer is not drawn, so the card never shows.
            .drawWithContent { layer.record { this@drawWithContent.drawContent() } }
    ) {
        ShareCard(data)
    }
    LaunchedEffect(data) {
        // Two frames: one to compose and draw the card, one so the recording is complete.
        withFrameNanos { }
        withFrameNanos { }
        val png = runCatching { layer.toImageBitmap().encodePng() }.getOrNull()
        if (png != null && png.isNotEmpty()) shareImage(png, data.caption) else shareText(data.caption)
        ShareCards.pending = null
    }
}

@Composable
private fun ShareCard(d: ShareCardData) {
    Box(Modifier.background(VsGround).padding(16.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(VsCard).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Qulay ", style = VsType.Title, color = VsInk)
                Text("Kurs", style = VsType.Title, color = VsEmerald)
                Spacer(Modifier.weight(1f))
                if (d.time != null) Text(d.time, style = VsType.Meta, color = VsInkSoft)
            }
            Spacer(Modifier.height(18.dp))
            Text(d.head, style = VsType.Body, color = VsInkSoft)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(d.total, style = VsType.Hero.copy(fontSize = 38.sp, lineHeight = 42.sp), color = VsInk, maxLines = 1, softWrap = false)
                Text(" " + d.unit, style = VsType.Title, color = VsInkSoft, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(d.bank, style = VsType.Title, color = VsInk, maxLines = 1)
            Text(d.rateLine, style = VsType.Meta, color = VsInkSoft)
            if (d.best) {
                Spacer(Modifier.height(10.dp))
                Text(
                    pick(lang, "Bugun eng yaxshi kurs", "Лучший курс сегодня", "Best rate today"),
                    style = VsType.Meta.copy(fontWeight = FontWeight.SemiBold),
                    color = VsEmerald,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(VsEmeraldSoft).padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("qulaykurs.uz · @qulaykursbot", style = VsType.Meta, color = VsInkSoft)
        }
    }
}

/** The share picture's data for one bank row, in the user's language. */
@Composable
fun shareCardFor(item: RankedBank, currency: Currency, amount: Double, sell: Boolean, updatedAt: String?, best: Boolean): ShareCardData {
    val ru = kursIsRu()
    val amt = if (amount > 0) amount.toLong() else 1L
    val total = if (amount > 0) item.total else item.perUnit.toLong()
    val som = kSom(ru)
    val head = if (sell) pick(lang, "${kGroup(amt)} ${currency.code} sotsangiz", "Продать ${kGroup(amt)} ${currency.code}", "Selling ${kGroup(amt)} ${currency.code}")
    else pick(lang, "${kGroup(amt)} ${currency.code} sotib olsangiz", "Купить ${kGroup(amt)} ${currency.code}", "Buying ${kGroup(amt)} ${currency.code}")
    return ShareCardData(
        head = head,
        total = kGroup(total),
        unit = som,
        bank = item.rate.bank,
        rateLine = "1 ${currency.code} = ${kGroup(item.perUnit)} $som",
        best = best,
        time = updatedAt?.formatUpdatedAt(),
        caption = kShareText(item, currency, amount, updatedAt, best)
    )
}
