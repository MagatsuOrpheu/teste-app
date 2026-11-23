plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Motor financeiro: Kotlin puro, sem Android, testável em JVM.
kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
