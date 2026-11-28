plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(libs.openvireo.core.jvm)
    // JNA comes with the core (its POM); coroutines is listed because the examples use it directly
    implementation(libs.kotlinx.coroutines.core)
}

application {
    mainClass = "examples.MainKt"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`   // some examples ask for a code or a passcode
}
