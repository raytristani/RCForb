import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

kotlin {
    // iOS device + simulator targets. The Xcode project consumes the
    // matching framework via embedAndSignAppleFrameworkForXcode.
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // Silence the LV2 dynamic-export warning Compose triggers.
            freeCompilerArgs += listOf("-Xbinary=bundleId=com.rcforb.shared")
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        // Match desktop ports' JVM-target-equivalent settings for shared code.
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }
        iosMain.dependencies {
            // No additional iOS-only deps yet — audio + persistence use
            // platform.* imports directly via Kotlin/Native interop.
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.rcforb.resources"
    generateResClass = always
}
