import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion: String by project

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("io.realm.kotlin") version "1.13.0"
    id("com.mikepenz.aboutlibraries.plugin") version "11.1.0"
    id("com.google.devtools.ksp") version "1.9.22-1.0.18"
    kotlin("plugin.serialization") version "1.9.23"
}

group = "com.pal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.nju.edu.cn/repository/maven-public/")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.0")
    implementation("dev.langchain4j:langchain4j:0.27.1")
    implementation("dev.langchain4j:langchain4j-open-ai:0.27.1")
    implementation("io.realm.kotlin:library-base:1.13.0")
    implementation("org.jetbrains.compose.material3:material3-desktop:1.6.0")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.6.0")
    implementation("com.mikepenz:aboutlibraries-core:11.1.0")
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.1.0")
    implementation("com.bumble.appyx:spotlight-desktop:2.0.0-alpha10")
    implementation("com.freeletics.flowredux:flowredux:1.2.1")
    implementation("io.github.oshai:kotlin-logging:6.0.3")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.3")
    implementation("org.slf4j:slf4j-simple:2.1.0-alpha1")
    implementation("com.darkrockstudios:mpfilepicker:3.1.0")
    implementation("com.darkrockstudios:mpfilepicker-jvm:3.1.0")
    implementation("io.github.softartdev:theme-material3:0.6.4")
    implementation("cafe.adriel.lyricist:lyricist:1.6.2-1.8.20")
    ksp("cafe.adriel.lyricist:lyricist-processor:1.6.2-1.8.20")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.mikepenz:multiplatform-markdown-renderer:0.13.0")
    implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.13.0")
    implementation("com.mikepenz:multiplatform-markdown-renderer-jvm:0.13.0")
}

ksp {
    arg("lyricist.internalVisibility", "true")
    arg("lyricist.generateStringsProperty", "true")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb,
            )
            packageName = "viewpal"
            packageVersion = "1.0.0"
            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
// ./gradlew exportLibraryDefinitions -PaboutLibraries.exportPath=src/main/resources/
aboutLibraries {
    registerAndroidTasks = false
    prettyPrint = true
}
