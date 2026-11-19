package com.example.bankkurslari

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Renders the real shared UI ([App]) headlessly and saves PNGs. Screen = 360 x H dp at density 3.
 * A tiny script language drives it: tap x y (dp), scroll x y dy, wait ms, shot name.
 */
val W_DP = (System.getenv("QK_W") ?: "412").toInt()
val H_DP = (System.getenv("QK_H") ?: "860").toInt()
val DENS = 1080f / W_DP

@OptIn(ExperimentalComposeUiApi::class)
class Driver(val out: File) {
    val scene = ImageComposeScene(
        (W_DP * DENS).toInt(), (H_DP * DENS).toInt(), Density(DENS), coroutineContext = Dispatchers.Unconfined
    ) { App() }
    private var t = 0L
    fun pump(ms: Long) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) { t += 16_000_000L; scene.render(t); Thread.sleep(8) }
        repeat(80) { t += 16_000_000L; scene.render(t) } // let animations settle
    }
    fun tap(x: Float, y: Float) {
        val p = Offset(x * DENS, y * DENS)
        scene.sendPointerEvent(PointerEventType.Move, p); scene.sendPointerEvent(PointerEventType.Press, p)
        t += 50_000_000L; scene.render(t)
        scene.sendPointerEvent(PointerEventType.Release, p)
        pump(700)
    }
    fun scroll(x: Float, y: Float, dy: Float) {
        val p = Offset(x * DENS, y * DENS)
        scene.sendPointerEvent(PointerEventType.Move, p)
        scene.sendPointerEvent(PointerEventType.Scroll, p, scrollDelta = Offset(0f, dy))
        pump(500)
    }
    fun shot(name: String) {
        val img = scene.render(t)
        File(out, "$name.png").writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        println("shot $name")
    }
}

fun main(args: Array<String>) {
    val out = File(System.getenv("QK_OUT") ?: "out").apply { mkdirs() }
    val s = shotStore()
    s.putBoolean(ONBOARDED_KEY, true)
    s.putString("lang", System.getenv("QK_LANG") ?: "uz")
    s.putInt("theme_mode", if (System.getenv("QK_DARK") != null) 2 else 1)
    s.putString("amount", System.getenv("QK_AMOUNT") ?: "1000")
    s.putString("direction", "sell")
    System.getenv("QK_ALERTS")?.let { s.putString("alerts_json", File(it).readText()) }

    val d = Driver(out)
    d.pump(6000)
    val script = File(args.firstOrNull() ?: "script.txt").readLines()
    for (line in script) {
        val p = line.trim().split(Regex("\\s+"))
        if (p.isEmpty() || p[0].isBlank() || p[0].startsWith("#")) continue
        when (p[0]) {
            "tap" -> d.tap(p[1].toFloat(), p[2].toFloat())
            "scroll" -> d.scroll(p[1].toFloat(), p[2].toFloat(), p[3].toFloat())
            "wait" -> d.pump(p[1].toLong())
            "shot" -> d.shot(p[1])
        }
    }
    d.scene.close()
    kotlin.system.exitProcess(0)
}
