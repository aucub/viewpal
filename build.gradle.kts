import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("io.realm.kotlin") version "1.13.0"
    id("com.mikepenz.aboutlibraries.plugin") version "11.1.0-b01"
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
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
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

tasks.withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
// ./gradlew exportLibraryDefinitions -PaboutLibraries.exportPath=src/main/resources/
aboutLibraries {
    registerAndroidTasks = false
    prettyPrint = true
}