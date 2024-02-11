import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.target.Family

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("io.realm.kotlin") version "1.13.0"
    id("com.mikepenz.aboutlibraries.plugin") version "11.1.0-b01"
    id("org.openjfx.javafxplugin") version "0.1.0"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")
    implementation("io.github.givimad:whisper-jni:1.5.2")
    implementation("dev.langchain4j:langchain4j:0.27.1")
    implementation("dev.langchain4j:langchain4j-open-ai:0.27.1")
    implementation("io.realm.kotlin:library-base:1.13.0")
    implementation("org.jetbrains.compose.material3:material3-desktop:1.5.12")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.5.12")
    implementation("com.mikepenz:aboutlibraries-core:11.1.0-b01")
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.1.0-b01")
    implementation("com.bumble.appyx:spotlight-desktop:2.0.0-alpha10")
    implementation("com.freeletics.flowredux:flowredux:1.2.1")
    implementation("io.github.oshai:kotlin-logging:6.0.3")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.3")
    implementation("org.slf4j:slf4j-simple:2.1.0-alpha1")
    api("com.mohamedrejeb.calf:calf-ui:0.3.1")
    implementation("com.mohamedrejeb.calf:calf-ui-desktop:0.3.1")
    implementation("com.mohamedrejeb.calf:calf-file-picker:0.3.1")
    implementation("com.mohamedrejeb.calf:calf-file-picker-desktop:0.3.1")
    implementation("com.darkrockstudios:mpfilepicker:3.1.0")
    implementation("com.darkrockstudios:mpfilepicker-jvm:3.1.0")
    // https://mvnrepository.com/artifact/org.openjfx/javafx
    implementation("org.openjfx:javafx:17")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            modules("jdk.unsupported")
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb
            )
            packageName = "viewpal"
            packageVersion = "1.0.0"
            macOS {
                iconFile.set(appResourcesRootDir.file("icon.icns"))
            }
            windows {
                iconFile.set(appResourcesRootDir.file("icon.ico"))
            }
            linux {
                iconFile.set(appResourcesRootDir.file("icon.png"))
            }
        }
    }
}
kotlin {
    targets
        .filterIsInstance<KotlinNativeTarget>()
        .forEach {
            it.binaries.framework {
                export("com.mohamedrejeb.calf:calf-ui:0.3.1")
                export("com.mohamedrejeb.calf:calf-ui-desktop:0.3.1")
                export("com.mohamedrejeb.calf:calf-file-picker:0.3.1")
                export("com.mohamedrejeb.calf:calf-file-picker-desktop:0.3.1")
            }
        }
}
javafx {
    version = "17"
    modules("javafx.controls")
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