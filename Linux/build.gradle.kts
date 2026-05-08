import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // JNA for libspeex_jni and libopus dynamic loading
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
}

// ---------- libspeex_jni native build ----------
// Bundle the libspeex C sources from android/app/src/main/cpp/ and compile
// them into libspeex_jni.so via gcc. Same JNA-friendly shim as the macOS port.
val androidCppDir = file("../android/app/src/main/cpp")
val nativeBuildDir = layout.buildDirectory.dir("native").get().asFile
val nativeOutDir = layout.buildDirectory.dir("native/out").get().asFile
val soFile = file("$nativeOutDir/libspeex_jni.so")

val buildSpeexJni by tasks.registering {
    inputs.dir(androidCppDir)
    outputs.file(soFile)
    doLast {
        nativeOutDir.mkdirs()
        nativeBuildDir.mkdirs()

        val sources = listOf(
            "speex_jni.c",
            "speex/bits.c", "speex/cb_search.c",
            "speex/exc_10_16_table.c", "speex/exc_10_32_table.c", "speex/exc_20_32_table.c",
            "speex/exc_5_256_table.c", "speex/exc_5_64_table.c", "speex/exc_8_128_table.c",
            "speex/filters.c", "speex/gain_table.c", "speex/gain_table_lbr.c",
            "speex/hexc_10_32_table.c", "speex/hexc_table.c", "speex/high_lsp_tables.c",
            "speex/kiss_fft.c", "speex/kiss_fftr.c", "speex/lpc.c", "speex/lsp.c",
            "speex/lsp_tables_nb.c", "speex/ltp.c", "speex/modes.c", "speex/modes_wb.c",
            "speex/nb_celp.c", "speex/quant_lsp.c", "speex/sb_celp.c", "speex/smallft.c",
            "speex/speex.c", "speex/speex_callbacks.c", "speex/speex_header.c",
            "speex/stereo.c", "speex/vbr.c", "speex/vq.c", "speex/window.c"
        )

        val args = mutableListOf(
            "/usr/bin/gcc",
            "-O2", "-fPIC",
            "-shared",
            "-DHAVE_CONFIG_H", "-DFLOATING_POINT",
            "-DEXPORT=__attribute__((visibility(\"default\")))",
            "-I", androidCppDir.absolutePath,
            "-I", "$androidCppDir/speex",
            "-I", "$androidCppDir/include",
            "-Wl,-soname,libspeex_jni.so",
            "-o", soFile.absolutePath
        )
        sources.forEach { args.add("$androidCppDir/$it") }

        // JNA-friendly shim wrapping speex_jni internals (same as macOS port).
        val jnaShim = file("$nativeBuildDir/speex_jna_shim.c")
        jnaShim.writeText("""
            // Auto-generated JNA-friendly shim wrapping speex_jni internals.
            #include <stdlib.h>
            #include <string.h>
            #include "speex/speex.h"
            #include "speex/speex_bits.h"

            typedef struct { void *dec_state; SpeexBits bits; int frame_size; } SpeexHandle;
            typedef struct { void *enc_state; SpeexBits bits; int frame_size; } SpeexEncHandle;

            __attribute__((visibility("default")))
            void* rcforb_speex_create_decoder(void) {
                SpeexHandle *h = (SpeexHandle*)calloc(1, sizeof(SpeexHandle));
                if (!h) return NULL;
                h->dec_state = speex_decoder_init(&speex_nb_mode);
                speex_bits_init(&h->bits);
                int enh = 1;
                speex_decoder_ctl(h->dec_state, SPEEX_SET_ENH, &enh);
                speex_decoder_ctl(h->dec_state, SPEEX_GET_FRAME_SIZE, &h->frame_size);
                return h;
            }

            __attribute__((visibility("default")))
            int rcforb_speex_decode(void *handle, const unsigned char *in, int in_len, short *out, int out_max_samples) {
                SpeexHandle *h = (SpeexHandle*)handle;
                if (!h || !h->dec_state) return -1;
                speex_bits_read_from(&h->bits, (const char*)in, in_len);
                int total = 0;
                while (speex_bits_remaining(&h->bits) > 10 && total + h->frame_size <= out_max_samples) {
                    int ret = speex_decode_int(h->dec_state, &h->bits, out + total);
                    if (ret != 0) break;
                    total += h->frame_size;
                }
                return total;
            }

            __attribute__((visibility("default")))
            void rcforb_speex_destroy_decoder(void *handle) {
                SpeexHandle *h = (SpeexHandle*)handle;
                if (!h) return;
                if (h->dec_state) {
                    speex_bits_destroy(&h->bits);
                    speex_decoder_destroy(h->dec_state);
                }
                free(h);
            }

            __attribute__((visibility("default")))
            void* rcforb_speex_create_encoder(int quality) {
                SpeexEncHandle *h = (SpeexEncHandle*)calloc(1, sizeof(SpeexEncHandle));
                if (!h) return NULL;
                h->enc_state = speex_encoder_init(&speex_nb_mode);
                speex_bits_init(&h->bits);
                int q = quality;
                speex_encoder_ctl(h->enc_state, SPEEX_SET_QUALITY, &q);
                speex_encoder_ctl(h->enc_state, SPEEX_GET_FRAME_SIZE, &h->frame_size);
                return h;
            }

            __attribute__((visibility("default")))
            int rcforb_speex_encode(void *handle, const short *pcm, int num_samples, unsigned char *out, int out_max) {
                SpeexEncHandle *h = (SpeexEncHandle*)handle;
                if (!h || !h->enc_state) return -1;
                if (num_samples < h->frame_size) return -1;
                speex_bits_reset(&h->bits);
                int offset = 0;
                while (offset + h->frame_size <= num_samples) {
                    speex_encode_int(h->enc_state, (short*)(pcm + offset), &h->bits);
                    offset += h->frame_size;
                }
                if (offset == 0) return -1;
                return speex_bits_write(&h->bits, (char*)out, out_max);
            }

            __attribute__((visibility("default")))
            int rcforb_speex_get_frame_size(void *handle) {
                SpeexHandle *h = (SpeexHandle*)handle;
                return h ? h->frame_size : 0;
            }

            __attribute__((visibility("default")))
            void rcforb_speex_destroy_encoder(void *handle) {
                SpeexEncHandle *h = (SpeexEncHandle*)handle;
                if (!h) return;
                if (h->enc_state) {
                    speex_bits_destroy(&h->bits);
                    speex_encoder_destroy(h->enc_state);
                }
                free(h);
            }
        """.trimIndent())

        // Build with shim instead of speex_jni.c (which is JNI-only).
        val argsNoShim = args.toMutableList()
        val jniIdx = argsNoShim.indexOfFirst { it.endsWith("speex_jni.c") }
        if (jniIdx >= 0) argsNoShim[jniIdx] = jnaShim.absolutePath

        println("Building libspeex_jni.so via gcc...")
        val proc = ProcessBuilder(argsNoShim).redirectErrorStream(true).start()
        val output = proc.inputStream.bufferedReader().readText()
        val rc = proc.waitFor()
        if (rc != 0) {
            throw GradleException("gcc failed (exit $rc):\n$output")
        }
        println("Built: ${soFile.absolutePath}")
    }
}

tasks.processResources {
    dependsOn(buildSpeexJni)
    from(soFile) { into("native") }
}

compose.desktop {
    application {
        mainClass = "com.rcforb.MainKt"

        nativeDistributions {
            // Compose Desktop's stock packagers handle Linux cleanly — no
            // codesign/notary dance needed (that was macOS Sequoia-specific).
            // Deb for Debian/Ubuntu, Rpm for Fedora/RHEL, AppImage for distro-
            // agnostic single-file distribution.
            //
            // Compose validates target formats against the host OS at config
            // time, so we only declare Linux formats when actually on Linux.
            // This keeps the file parseable on any host (handy for IDE config
            // and for running CI checks from non-Linux machines).
            val osName = System.getProperty("os.name").lowercase()
            if (osName.contains("linux")) {
                targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
            }
            packageName = "RCForb"
            packageVersion = "1.0.0"
            description = "Remote Ham Radio Control"
            copyright = "© 2026 Ramon E. Tristani"
            vendor = "Ramon E. Tristani"

            linux {
                iconFile.set(project.file("icon/AppIcon.png"))
                packageName = "rcforb"
                debMaintainer = "raytristani@gmail.com"
                menuGroup = "Network;HamRadio;"
                appCategory = "Network"
                appRelease = "1"
                rpmLicenseType = "MIT"
            }
        }

        buildTypes.release.proguard {
            isEnabled.set(false)
        }
    }
}
