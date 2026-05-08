import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.zip.ZipFile

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
// We bundle the libspeex C sources from android/app/src/main/cpp/ and compile
// them into libspeex_jni.dylib using clang. No cmake required.

val androidCppDir = file("../android/app/src/main/cpp")
val nativeBuildDir = layout.buildDirectory.dir("native").get().asFile
val nativeOutDir = layout.buildDirectory.dir("native/out").get().asFile
val dylibFile = file("$nativeOutDir/libspeex_jni.dylib")

val buildSpeexJni by tasks.registering {
    inputs.dir(androidCppDir)
    outputs.file(dylibFile)
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
            "/usr/bin/clang",
            "-O2", "-fPIC", "-arch", "arm64", "-arch", "x86_64",
            "-shared",
            "-DHAVE_CONFIG_H", "-DFLOATING_POINT",
            "-DEXPORT=__attribute__((visibility(\"default\")))",
            "-I", androidCppDir.absolutePath,
            "-I", "$androidCppDir/speex",
            "-I", "$androidCppDir/include",
            "-o", dylibFile.absolutePath
        )
        sources.forEach { args.add("$androidCppDir/$it") }

        // Replace the JNI shim with a JNA-friendly shim so the macOS port
        // can dlopen this dylib and call functions directly (no Java JNI runtime).
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

        // Build with shim instead of speex_jni.c (which is JNI-only)
        val argsNoShim = args.toMutableList()
        val jniIdx = argsNoShim.indexOfFirst { it.endsWith("speex_jni.c") }
        if (jniIdx >= 0) argsNoShim[jniIdx] = jnaShim.absolutePath

        println("Building libspeex_jni.dylib via clang...")
        val proc = ProcessBuilder(argsNoShim).redirectErrorStream(true).start()
        val output = proc.inputStream.bufferedReader().readText()
        val rc = proc.waitFor()
        if (rc != 0) {
            throw GradleException("clang failed (exit $rc):\n$output")
        }
        println("Built: ${dylibFile.absolutePath}")
    }
}

tasks.processResources {
    dependsOn(buildSpeexJni)
    from(dylibFile) { into("native") }
}

// ---------- Sequoia-safe packaging ----------
// JDK 21's jpackage runs `codesign -s -` (ad-hoc) on the .app it produces.
// macOS Sequoia (15+) auto-attaches `com.apple.FinderInfo` /
// `com.apple.fileprovider.fpfs#P` xattrs to user-created files which codesign
// refuses to sign. Fix: invoke jpackage ourselves with `--type app-image`,
// re-bundle the result via `ditto --noextattr --noqtn`, sign the clean copy,
// and package via `hdiutil` (DMG) or `ditto -c -k` (Zip). This bypasses
// Compose Desktop's `:createDistributable` / `:packageDmg` entirely.
val runtimeClasspath: Configuration = configurations.getByName("runtimeClasspath")

val pkgVersion = "1.0.0"

// Shared output: a fully signed RCForb.app (hardened runtime if Developer ID).
// Both macSafeDmg and macSafeZip consume this. We stage under /tmp to dodge
// the iCloud Drive file-provider xattrs that break codesign.
val signedAppStageDir = file("/tmp/rcforb-mac-build/macOS")
val signedAppFile = signedAppStageDir.resolve("RCForb.app")

val prepareSignedApp by tasks.registering {
    group = "compose desktop"
    description = "Build, clean, and codesign RCForb.app (no packaging). Output: /tmp/rcforb-mac-build/macOS/RCForb.app"
    dependsOn("createRuntimeImage", tasks.named("jar"), tasks.named("processResources"))

    val runtimeFiles = runtimeClasspath
    val mainJarTask = tasks.named("jar")

    // Mark output dir so Gradle skips when nothing changed and inputs are stable.
    outputs.dir(signedAppStageDir)

    doLast {
        val javaHome = System.getProperty("java.home")
        val jpackageBin = file("$javaHome/bin/jpackage")
        require(jpackageBin.exists()) { "jpackage not found at $jpackageBin" }

        // IMPORTANT: stage outside ~/Desktop. iCloud Drive's Desktop sync
        // attaches com.apple.provenance + com.apple.fileprovider.fpfs#P to
        // every file in synced folders, and the OS re-attaches them instantly
        // when stripped. codesign refuses to sign anything carrying those
        // xattrs ("resource fork, Finder information, or similar detritus
        // not allowed"). Building under /tmp sidesteps the file provider.
        val workRoot = signedAppStageDir
        workRoot.deleteRecursively()
        workRoot.mkdirs()

        val rawAppDir = workRoot.resolve("raw")
        rawAppDir.mkdirs()

        // Build a fresh libs dir holding our jar + every runtime dependency.
        val libsDir = workRoot.resolve("libs").apply { mkdirs() }
        val mainJarSrc = (mainJarTask.get().outputs.files.singleFile)
        val mainJar = libsDir.resolve(mainJarSrc.name)
        mainJarSrc.copyTo(mainJar, overwrite = true)
        runtimeFiles.files.forEach { dep ->
            if (dep.isFile && dep.extension == "jar") {
                dep.copyTo(libsDir.resolve(dep.name), overwrite = true)
            }
        }
        val runtimeDir = layout.buildDirectory.dir("compose/tmp/main/runtime").get().asFile
        require(runtimeDir.exists()) { "Runtime image missing — :createRuntimeImage didn't run." }
        val iconFile = project.file("icon/AppIcon.icns")

        fun run(vararg cmd: String, ignoreFailure: Boolean = false): String {
            val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val out = proc.inputStream.bufferedReader().readText()
            val rc = proc.waitFor()
            if (rc != 0 && !ignoreFailure) {
                throw GradleException("${cmd.joinToString(" ")} failed (exit $rc):\n$out")
            }
            return out
        }

        println("Running jpackage --type app-image (codesign failure is expected and ignored)…")
        run(
            jpackageBin.absolutePath,
            "--type", "app-image",
            "--name", "RCForb",
            "--app-version", pkgVersion,
            "--description", "Remote Ham Radio Control",
            "--copyright", "© 2026 Ramon E. Tristani",
            "--vendor", "Ramon E. Tristani",
            "--input", libsDir.absolutePath,
            "--runtime-image", runtimeDir.absolutePath,
            "--main-jar", mainJar.name,
            "--main-class", "com.rcforb.MainKt",
            "--icon", iconFile.absolutePath,
            "--mac-package-identifier", "com.rcforb.macos",
            "--mac-package-name", "RCForb",
            "--java-options", "-Dapple.awt.application.appearance=system",
            "--java-options", "-Dcompose.application.configure.swing.globals=true",
            "--java-options", "-Dskiko.library.path=\$APPDIR",
            "--java-options", "-Xdock:name=RCForb",
            "--dest", rawAppDir.absolutePath,
            ignoreFailure = true
        )

        val rawApp = rawAppDir.resolve("RCForb.app")
        require(rawApp.exists()) { "jpackage didn't produce ${rawApp.absolutePath}" }

        val cleanApp = workRoot.resolve("RCForb.app")
        if (cleanApp.exists()) cleanApp.deleteRecursively()

        println("Building fresh bundle dir without bundle-level xattrs …")
        // The actual blocking xattrs (com.apple.FinderInfo,
        // com.apple.fileprovider.fpfs#P) attach to the bundle *directory*
        // and are read-only. We can't strip them. Workaround: mkdir a fresh
        // .app dir (which inherits no FinderInfo) and ditto in only the
        // Contents/ subtree.
        cleanApp.mkdirs()
        run("/usr/bin/ditto", "--noextattr", "--noqtn",
            rawApp.resolve("Contents").absolutePath,
            cleanApp.resolve("Contents").absolutePath)

        // Extract skiko native dylib from its jar — Compose Desktop normally
        // does this in :createDistributable, but our hand-rolled jpackage
        // invocation skips the resource staging step.
        val appDir = cleanApp.resolve("Contents/app")
        val skikoJar = appDir.listFiles()?.firstOrNull {
            it.name.startsWith("skiko-awt-runtime-macos-") && it.extension == "jar"
        }
        if (skikoJar != null) {
            val zf = ZipFile(skikoJar)
            try {
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue
                    if (!(entry.name.endsWith(".dylib") || entry.name.endsWith(".dylib.sha256"))) continue
                    val baseName: String = entry.name.substringAfterLast('/')
                    val outFile = appDir.resolve(baseName as String)
                    val input = zf.getInputStream(entry)
                    val output = outFile.outputStream()
                    try { input.copyTo(output) } finally { output.close(); input.close() }
                    println("Extracted ${outFile.name}")
                }
            } finally { zf.close() }
        }

        // Inject NSMicrophoneUsageDescription and LSMinimumSystemVersion into Info.plist.
        val infoPlist = cleanApp.resolve("Contents/Info.plist")
        if (infoPlist.exists()) {
            val xml = infoPlist.readText()
            val injection = """
                |	<key>NSMicrophoneUsageDescription</key>
                |	<string>RCForb needs microphone access to transmit audio to the remote radio.</string>
                |	<key>LSMinimumSystemVersion</key>
                |	<string>11.0</string>
                |</dict>
            """.trimMargin()
            if (!xml.contains("NSMicrophoneUsageDescription")) {
                infoPlist.writeText(xml.replaceFirst("</dict>", injection))
            }
        }

        // Pick a signing identity. If RCFORB_SIGN_IDENTITY env var is set
        // (e.g. "Developer ID Application: Your Name (TEAMID)"), use it —
        // that produces a Gatekeeper-trusted bundle. Otherwise fall back to
        // ad-hoc, which only runs on the build machine and triggers
        // "developer cannot be verified" on other Macs.
        val signIdentity: String = (System.getenv("RCFORB_SIGN_IDENTITY") ?: "-").trim()
        val isAdHoc = signIdentity == "-" || signIdentity.isEmpty()

        if (isAdHoc) {
            println("Ad-hoc codesigning clean .app (LOCAL-ONLY — won't run on other Macs).")
            println("To produce a Gatekeeper-trusted bundle: export RCFORB_SIGN_IDENTITY=\"Developer ID Application: NAME (TEAMID)\"")
        } else {
            println("Codesigning with Developer ID identity: $signIdentity")
        }
        // Strip every extended attribute the OS may have re-attached after we
        // wrote Info.plist / extracted skiko dylibs. codesign --strict refuses
        // to verify any bundle that has com.apple.FinderInfo or similar.
        run("/usr/bin/xattr", "-cr", cleanApp.absolutePath, ignoreFailure = true)
        run("/usr/bin/codesign", "--remove-signature", cleanApp.absolutePath, ignoreFailure = true)

        // Write hardened-runtime entitlements (only used in Developer ID mode).
        val entitlementsFile = workRoot.resolve("entitlements.plist")
        if (!isAdHoc) {
            entitlementsFile.parentFile.mkdirs()
            entitlementsFile.writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                <dict>
                    <key>com.apple.security.cs.allow-jit</key><true/>
                    <key>com.apple.security.cs.allow-unsigned-executable-memory</key><true/>
                    <key>com.apple.security.cs.disable-library-validation</key><true/>
                    <key>com.apple.security.device.audio-input</key><true/>
                </dict>
                </plist>
                """.trimIndent()
            )
        }

        // codesign one Mach-O. We sign each nested binary individually because
        // `--deep` is deprecated/unreliable; Apple's notary expects every
        // executable inside the bundle (and inside any embedded jars) to be
        // signed with the Developer ID cert + secure timestamp + hardened
        // runtime when applicable.
        fun signOne(path: String, hardened: Boolean) {
            // Sequoia keeps re-attaching com.apple.FinderInfo to anything we
            // touched (unzip, replaceFirst on Info.plist, etc). Strip per-file
            // immediately before codesign or it bails with "resource fork...".
            run("/usr/bin/xattr", "-c", path, ignoreFailure = true)
            val args = mutableListOf(
                "/usr/bin/codesign",
                "-s", signIdentity,
                "--force"
            )
            if (!isAdHoc) {
                args.addAll(listOf("--timestamp"))
                if (hardened) {
                    args.addAll(listOf("--options", "runtime",
                        "--entitlements", entitlementsFile.absolutePath))
                }
            }
            args.add(path)
            run(*args.toTypedArray())
        }

        // Walk a directory and sign every Mach-O file under it.
        fun signMachOTree(root: File, hardened: Boolean) {
            root.walkTopDown().filter { it.isFile }.forEach { f ->
                val out = run("/usr/bin/file", "-b", f.absolutePath, ignoreFailure = true)
                if (out.contains("Mach-O") || out.contains("dynamically linked shared library")) {
                    signOne(f.absolutePath, hardened)
                }
            }
        }

        // Sign native libs inside an embedded jar by extracting → signing → repacking.
        fun signNativesInJar(jar: File) {
            val tmp = workRoot.resolve("jar-rewrite/${jar.nameWithoutExtension}").apply {
                deleteRecursively(); mkdirs()
            }
            run("/usr/bin/unzip", "-q", "-o", jar.absolutePath, "-d", tmp.absolutePath)
            var changed = false
            tmp.walkTopDown().filter { it.isFile }.forEach { f ->
                val name = f.name
                if (name.endsWith(".dylib") || name.endsWith(".jnilib") || name.endsWith(".so")) {
                    signOne(f.absolutePath, hardened = true)
                    changed = true
                }
            }
            if (changed) {
                jar.delete()
                // jar uses zip format; rebuild with `zip -r` from inside tmp dir
                val proc = ProcessBuilder("/usr/bin/zip", "-qr", jar.absolutePath, ".")
                    .directory(tmp).redirectErrorStream(true).start()
                val out = proc.inputStream.bufferedReader().readText()
                if (proc.waitFor() != 0) throw GradleException("zip failed for $jar:\n$out")
            }
        }

        if (isAdHoc) {
            println("Ad-hoc codesigning clean .app (LOCAL-ONLY — won't run on other Macs).")
            println("To produce a Gatekeeper-trusted bundle: export RCFORB_SIGN_IDENTITY=\"Developer ID Application: NAME (TEAMID)\"")
            signOne(cleanApp.absolutePath, hardened = false)
        } else {
            println("Codesigning bundle bottom-up with Developer ID: $signIdentity")

            // 1. Sign native libs hidden inside jars (jna, our own jar with libspeex_jni).
            val appResDir = cleanApp.resolve("Contents/app")
            appResDir.listFiles()?.filter { it.isFile && it.extension == "jar" }?.forEach { jar ->
                signNativesInJar(jar)
            }

            // 2. Sign every Mach-O in Contents/runtime (the bundled JDK).
            signMachOTree(cleanApp.resolve("Contents/runtime"), hardened = true)

            // 3. Sign every Mach-O directly under Contents/app (skiko dylibs, etc.).
            cleanApp.resolve("Contents/app").listFiles()?.filter { it.isFile }?.forEach { f ->
                if (f.name.endsWith(".dylib") || f.name.endsWith(".so")) {
                    signOne(f.absolutePath, hardened = true)
                }
            }

            // 4. Sign the launcher under Contents/MacOS.
            cleanApp.resolve("Contents/MacOS").listFiles()?.filter { it.isFile }?.forEach { f ->
                signOne(f.absolutePath, hardened = true)
            }

            // 5. Sign the bundle itself last (this is the seal Apple checks first).
            signOne(cleanApp.absolutePath, hardened = true)
        }

        run("/usr/bin/codesign", "--verify", "--deep", "--strict", cleanApp.absolutePath)
        println("Signed app ready at: ${cleanApp.absolutePath}")
    }
}

// Helper: shell out, capture combined stdout/stderr.
fun execRun(vararg cmd: String, ignoreFailure: Boolean = false): String {
    val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
    val out = proc.inputStream.bufferedReader().readText()
    val rc = proc.waitFor()
    if (rc != 0 && !ignoreFailure) {
        throw GradleException("${cmd.joinToString(" ")} failed (exit $rc):\n$out")
    }
    return out
}

// ---------- DMG packaging ----------
val macSafeDmg by tasks.registering {
    group = "compose desktop"
    description = "Build a Sequoia-safe signed (and optionally notarized) DMG."
    dependsOn(prepareSignedApp)

    val outDmg = layout.buildDirectory.file("compose/binaries/main/dmg/RCForb-$pkgVersion.dmg")
    outputs.file(outDmg)

    doLast {
        val isAdHoc = (System.getenv("RCFORB_SIGN_IDENTITY") ?: "-").trim().let { it == "-" || it.isEmpty() }
        val outFile = outDmg.get().asFile
        outFile.parentFile.mkdirs()
        if (outFile.exists()) outFile.delete()

        // Build the DMG in /tmp — hdiutil into iCloud-synced ~/Desktop has
        // triggered provenance issues during stapling in the past.
        val stagedDmg = signedAppStageDir.resolve("RCForb-${pkgVersion}.dmg")
        if (stagedDmg.exists()) stagedDmg.delete()
        println("Creating DMG via hdiutil …")
        execRun(
            "/usr/bin/hdiutil", "create",
            "-volname", "RCForb",
            "-srcfolder", signedAppFile.absolutePath,
            "-ov", "-format", "UDZO",
            stagedDmg.absolutePath
        )

        // Optional notarization. Triggers when RCFORB_NOTARY_PROFILE is set.
        // Stapling the ticket to the DMG makes it work offline on other Macs.
        val notaryProfile: String? = System.getenv("RCFORB_NOTARY_PROFILE")?.takeIf { it.isNotBlank() }
        if (notaryProfile != null && !isAdHoc) {
            println("Submitting DMG to Apple notary (profile=$notaryProfile)…")
            execRun(
                "/usr/bin/xcrun", "notarytool", "submit",
                stagedDmg.absolutePath,
                "--keychain-profile", notaryProfile,
                "--wait"
            )
            println("Stapling notarization ticket to DMG …")
            execRun("/usr/bin/xcrun", "stapler", "staple", stagedDmg.absolutePath)
            execRun("/usr/bin/xcrun", "stapler", "validate", stagedDmg.absolutePath)
            println("Notarized.")
        } else if (notaryProfile != null && isAdHoc) {
            println("WARNING: RCFORB_NOTARY_PROFILE is set but signing is ad-hoc — skipping notarization.")
        }

        stagedDmg.copyTo(outFile, overwrite = true)
        println("DMG: ${outFile.absolutePath}")
    }
}

// ---------- Zip packaging ----------
// Zip via `ditto -c -k --keepParent --sequesterRsrc`. This is the
// Apple-recommended packager for signed bundles: it preserves symlinks
// (the bundled JDK has many under Contents/runtime/Contents/Home/), POSIX
// permissions, and codesign metadata so the signature still verifies after
// unzipping on the recipient's Mac. Gradle's stock `Zip` task can't do this.
//
// Notarization workflow for zip distribution:
//   1. Sign the .app (done in prepareSignedApp).
//   2. Zip it for submission via ditto.
//   3. Submit the zip to notarytool. (You can't staple a ticket to a zip.)
//   4. Staple the ticket to the .app inside the bundle.
//   5. Re-zip the now-stapled .app for distribution.
// The recipient unzips and double-clicks; Gatekeeper finds the stapled
// ticket on the .app and approves it, even fully offline.
val macSafeZip by tasks.registering {
    group = "compose desktop"
    description = "Build a Sequoia-safe signed (and optionally notarized) zip with full bundled JRE + native libs."
    dependsOn(prepareSignedApp)

    val outZip = layout.buildDirectory.file("compose/binaries/main/zip/RCForb-macos-$pkgVersion.zip")
    outputs.file(outZip)

    doLast {
        val isAdHoc = (System.getenv("RCFORB_SIGN_IDENTITY") ?: "-").trim().let { it == "-" || it.isEmpty() }
        val outFile = outZip.get().asFile
        outFile.parentFile.mkdirs()
        if (outFile.exists()) outFile.delete()

        require(signedAppFile.exists()) { "Signed .app missing at ${signedAppFile.absolutePath}" }

        val notaryProfile: String? = System.getenv("RCFORB_NOTARY_PROFILE")?.takeIf { it.isNotBlank() }

        // Stage the submission zip in /tmp (same xattr-avoidance reasoning).
        val submissionZip = signedAppStageDir.resolve("RCForb-submission-${pkgVersion}.zip")
        if (submissionZip.exists()) submissionZip.delete()

        println("Creating submission zip via ditto …")
        execRun(
            "/usr/bin/ditto",
            "-c", "-k", "--keepParent", "--sequesterRsrc",
            signedAppFile.absolutePath,
            submissionZip.absolutePath
        )

        if (notaryProfile != null && !isAdHoc) {
            println("Submitting zip to Apple notary (profile=$notaryProfile)…")
            execRun(
                "/usr/bin/xcrun", "notarytool", "submit",
                submissionZip.absolutePath,
                "--keychain-profile", notaryProfile,
                "--wait"
            )
            // Stapler attaches the ticket to the .app on disk (you can't
            // staple a zip). After this, re-zip to capture the stapled .app.
            println("Stapling notarization ticket to .app …")
            execRun("/usr/bin/xcrun", "stapler", "staple", signedAppFile.absolutePath)
            execRun("/usr/bin/xcrun", "stapler", "validate", signedAppFile.absolutePath)
            println("Notarized + stapled.")
        } else if (notaryProfile != null && isAdHoc) {
            println("WARNING: RCFORB_NOTARY_PROFILE is set but signing is ad-hoc — skipping notarization.")
        }

        // Final distribution zip — contains the .app with the stapled ticket
        // (if notarized). Recipient: unzip + double-click.
        val finalZip = signedAppStageDir.resolve("RCForb-macos-${pkgVersion}.zip")
        if (finalZip.exists()) finalZip.delete()
        println("Creating distribution zip via ditto …")
        execRun(
            "/usr/bin/ditto",
            "-c", "-k", "--keepParent", "--sequesterRsrc",
            signedAppFile.absolutePath,
            finalZip.absolutePath
        )

        finalZip.copyTo(outFile, overwrite = true)
        println("Zip: ${outFile.absolutePath}")
    }
}

compose.desktop {
    application {
        mainClass = "com.rcforb.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "RCForb"
            packageVersion = "1.0.0"
            description = "Remote Ham Radio Control"
            copyright = "© 2026 Ramon E. Tristani"
            vendor = "Ramon E. Tristani"

            macOS {
                bundleID = "com.rcforb.macos"
                appCategory = "public.app-category.utilities"
                iconFile.set(project.file("icon/AppIcon.icns"))
                jvmArgs("-Dapple.awt.application.appearance=system")
                // Skip ad-hoc codesign — macOS Sequoia provenance xattrs trip codesign
                // on unsigned builds. Distribute as zip, or sign manually post-build.
                signing { sign.set(false) }
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSMicrophoneUsageDescription</key>
                        <string>RCForb needs microphone access to transmit audio to the remote radio.</string>
                        <key>LSMinimumSystemVersion</key>
                        <string>11.0</string>
                    """.trimIndent()
                }
            }
        }

        buildTypes.release.proguard {
            isEnabled.set(false)
        }
    }
}
