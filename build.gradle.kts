plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(libs.openvireo.core.jvm)
    // The release jar carries no POM, so its two dependencies are listed here
    implementation(libs.jna)
    implementation(libs.kotlinx.coroutines.core)
}

application {
    mainClass = "examples.MainKt"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`   // some examples ask for a code or a passcode
}
