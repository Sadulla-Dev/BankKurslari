plugins {
    kotlin("multiplatform") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("org.jetbrains.compose") version "1.11.1"
}

val shared = rootDir.resolve("../../composeApp/src/commonMain")

kotlin {
    jvm { mainRun { mainClass.set("com.example.bankkurslari.ShotsKt") } }
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.addAll("kotlin.time.ExperimentalTime", "androidx.compose.material3.ExperimentalMaterial3Api")
    }
    sourceSets {
        commonMain {
            kotlin.srcDir(shared.resolve("kotlin"))
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
                implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
                implementation("org.jetbrains.compose.ui:ui:1.11.1")
                implementation("org.jetbrains.compose.ui:ui-backhandler:1.11.1")
                implementation("org.jetbrains.compose.components:components-resources:1.11.1")
                implementation("org.jetbrains.compose.material3:material3:1.9.0")
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
                implementation("io.ktor:ktor-client-core:3.6.0")
            }
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("io.ktor:ktor-client-okhttp:3.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
        }
    }
}

compose.resources {
    packageOfResClass = "com.example.bankkurslari.generated"
    generateResClass = always
    customDirectory("commonMain", provider { layout.projectDirectory.dir("../../composeApp/src/commonMain/composeResources") })
}

// Writes the runtime classpath so the renderer can be started with plain `java`.
tasks.register("cp") {
    dependsOn("jvmJar")
    doLast {
        val cp = kotlin.jvm().compilations["main"].runtimeDependencyFiles!!.files + tasks.named("jvmJar").get().outputs.files.files
        file("build/cp.txt").writeText(cp.joinToString(File.pathSeparator))
    }
}
