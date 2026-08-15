import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.materialIconsExtended)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "eivydas.senkus.logcruiser.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.AppImage, TargetFormat.Exe)
            packageName = "logcruiser"
            packageVersion = project.findProperty("packageVersion") as? String ?: "1.0.0"
        }
    }
}
