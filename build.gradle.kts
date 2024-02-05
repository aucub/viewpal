import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("io.realm.kotlin") version "1.13.0"
    id("com.mikepenz.aboutlibraries.plugin") version "10.10.0"
}

group = "com.pal"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://repo.nju.edu.cn/repository/maven-public/")
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")
    // https://mvnrepository.com/artifact/io.github.givimad/whisper-jni
    implementation("io.github.givimad:whisper-jni:1.5.2")
    // https://mvnrepository.com/artifact/dev.langchain4j/langchain4j
    implementation("dev.langchain4j:langchain4j:0.25.0")
    // https://mvnrepository.com/artifact/dev.langchain4j/langchain4j-open-ai
    implementation("dev.langchain4j:langchain4j-open-ai:0.25.0")
    implementation("io.realm.kotlin:library-base:1.13.0")

    implementation("org.jetbrains.compose.material3:material3-desktop:1.5.11")
    // https://mvnrepository.com/artifact/org.jetbrains.compose.material/material-icons-extended-desktop
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.5.12")

    // https://mvnrepository.com/artifact/com.mikepenz/aboutlibraries-core
    implementation("com.mikepenz:aboutlibraries-core:10.10.0")

    implementation("com.mikepenz:aboutlibraries-compose-m3:10.10.0")
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