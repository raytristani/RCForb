package com.rcforb.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import org.jetbrains.skia.Image
import java.io.File

/** Helpers to load PNGs and the Digital‑7 Mono font from JVM resources. */

private fun loadResourceBytes(path: String): ByteArray =
    object {}.javaClass.getResourceAsStream(path)?.readBytes()
        ?: error("Resource not found: $path")

@Composable
fun rememberResourcePainter(path: String): Painter {
    return remember(path) {
        val bytes = loadResourceBytes(path)
        val image = Image.makeFromEncoded(bytes)
        BitmapPainter(image.toComposeImageBitmap())
    }
}

@Composable
fun rememberResourceImage(path: String): ImageBitmap {
    return remember(path) {
        val bytes = loadResourceBytes(path)
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }
}

object Digital7Loader {
    @Volatile private var cached: FontFamily? = null

    fun family(): FontFamily {
        return cached ?: synchronized(this) {
            cached ?: createFamily().also { cached = it }
        }
    }

    private fun createFamily(): FontFamily {
        val bytes = loadResourceBytes("/font/digital_7_mono.ttf")
        val tmp = File.createTempFile("digital_7_mono", ".ttf")
        tmp.deleteOnExit()
        tmp.outputStream().use { it.write(bytes) }
        // Compose Desktop ships an extension overload that accepts a File for TTF/OTF assets.
        return FontFamily(Font(file = tmp, weight = FontWeight.Normal, style = FontStyle.Normal))
    }
}

val Digital7MonoFamily: FontFamily by lazy { Digital7Loader.family() }
