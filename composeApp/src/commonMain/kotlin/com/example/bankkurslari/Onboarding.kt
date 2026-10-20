package com.example.bankkurslari

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.bankkurslari.ui.theme.VsBg
import com.example.bankkurslari.ui.theme.VsEmerald
import com.example.bankkurslari.ui.theme.VsTealTint
import com.example.bankkurslari.ui.theme.VsCard
import com.example.bankkurslari.ui.theme.VsHair
import com.example.bankkurslari.ui.theme.VsInk
import com.example.bankkurslari.ui.theme.VsInkSoft
import com.example.bankkurslari.ui.theme.VsType

const val ONBOARDED_KEY = "onboarded_v1"

/**
 * First launch: language, main currency, usual direction. Each choice is saved immediately, so leaving
 * half-way still keeps what was picked. [onLang] applies the language live so the next steps read in it.
 */
@Composable
fun Onboarding(lang: Lang, onLang: (Lang) -> Unit, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var currency by remember { mutableStateOf(Currency.entries.firstOrNull { it.code == RateService.selectedCurrency() } ?: Currency.USD) }
    var sell by remember { mutableStateOf((appStore().getString("direction") ?: "sell") != "buy") }

    fun finish() {
        appStore().putBoolean(ONBOARDED_KEY, true)
        onDone()
    }

    Column(
        Modifier.fillMaxSize().background(VsBg).safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // progress dots + skip
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Box(Modifier.size(width = if (i == step) 20.dp else 6.dp, height = 6.dp).clip(CircleShape).background(if (i <= step) VsEmerald else VsHair))
                }
            }
            Text(
                tr("Oʻtkazib yuborish", "Пропустить", "Skip"), style = VsType.BodyMedium, color = VsInkSoft,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { finish() }.padding(8.dp)
            )
        }

        AnimatedContent(
            step,
            transitionSpec = {
                (slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it / 6 } + fadeIn(tween(Motion.MEDIUM))) togetherWith
                    (slideOutHorizontally(tween(Motion.SHORT)) { -it / 6 } + fadeOut(tween(Motion.SHORT)))
            },
            modifier = Modifier.weight(1f),
            label = "onb"
        ) { s ->
            Column(Modifier.fillMaxSize().padding(top = 24.dp)) {
                when (s) {
                    0 -> {
                        Text(tr("Qulay Kurs", "Qulay Kurs", "Qulay Kurs"), style = VsType.Hero, color = VsEmerald)
                        Text(
                            tr("Qaysi bankda foydali — bir qarashda.",
                                "Где выгоднее — с первого взгляда.",
                                "The best bank, at a glance."),
                            style = VsType.Body, color = VsInk, modifier = Modifier.padding(top = 8.dp)
                        )
                        StepTitle(tr("Tilni tanlang", "Выберите язык", "Choose your language"))
                        Lang.entries.forEach { l -> Choice(l.label, l == lang) { onLang(l) } }
                    }
                    1 -> {
                        StepTitle(tr("Asosan qaysi valyuta?", "Какая валюта в основном?", "Which currency do you use most?"))
                        Currency.entries.forEach { c ->
                            Choice("${c.code} · ${c.displayName(lang)}", c == currency) {
                                currency = c
                                RateService.setSelectedCurrency(c.code)
                            }
                        }
                    }
                    else -> {
                        StepTitle(tr("Odatda nima qilasiz?", "Что вы обычно делаете?", "What do you usually do?"))
                        Choice(
                            tr("Valyuta sotish", "Продать валюту", "Sell currency"), sell,
                            sub = tr("Eng koʻp soʻm beradigan bank", "Где дают больше сумов", "Bank that pays the most"),
                            icon = Icons.Outlined.ArrowUpward
                        ) { sell = true; appStore().putString("direction", "sell") }
                        Choice(
                            tr("Valyuta olish", "Купить валюту", "Buy currency"), !sell,
                            sub = tr("Eng arzon sotadigan bank", "Где валюта дешевле", "Bank that sells cheapest"),
                            icon = Icons.Outlined.ArrowDownward
                        ) { sell = false; appStore().putString("direction", "buy") }
                    }
                }
            }
        }

        VsPrimaryButton(
            text = if (step < 2) tr("Davom etish", "Далее", "Continue") else tr("Boshlash", "Начать", "Get started"),
            onClick = { if (step < 2) step++ else finish() },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.AutoMirrored.Outlined.ArrowForward
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(text, style = VsType.Title, color = VsInk, modifier = Modifier.padding(top = 32.dp, bottom = 12.dp))
}

@Composable
private fun Choice(label: String, selected: Boolean, sub: String? = null, icon: ImageVector? = null, onClick: () -> Unit) {
    val shape = VsShape.Panel
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(min = 56.dp).clip(shape)
            .background(if (selected) VsTealTint else VsCard)
            .border(if (selected) 2.dp else 1.dp, if (selected) VsEmerald else VsHair, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) VsEmerald else VsInkSoft, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = VsType.BodyMedium, color = VsInk)
            if (sub != null) Text(sub, style = VsType.Meta, color = VsInkSoft)
        }
        if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(VsEmerald))
    }
}
