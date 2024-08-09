import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    application
    kotlin("multiplatform") version "2.0.0"
    id("maven-publish")
}

group = "com.magicreg"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        withJava()
    }
    wasmJs {
        binaries.executable()
        browser()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
        }
    }
}

application {
    mainClass.set("com.magicreg.kijijune.MainKt")
}

