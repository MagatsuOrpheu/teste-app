import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Assinatura fixa: keystore no repositório (base64), senha no Secret KEYSTORE_PASSWORD do GitHub.
// Sem a senha (build local), usa a chave de debug.
val keystorePassword: String? = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
val keystoreB64 = rootProject.file("keystore/release.p12.b64")
val releaseKeystore = layout.buildDirectory.file("signing/release.p12").get().asFile
if (keystorePassword != null && keystoreB64.exists()) {
    releaseKeystore.parentFile.mkdirs()
    releaseKeystore.writeBytes(Base64.getMimeDecoder().decode(keystoreB64.readText()))
}
// Cada build do GitHub Actions ganha um número de versão maior.
val buildNumber: Int = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.jhowww.financeiro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jhowww.financeiro"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "2.0.$buildNumber"
    }

    signingConfigs {
        create("release") {
            storeFile = releaseKeystore
            storePassword = keystorePassword
            keyAlias = "meufinanceiro"
            keyPassword = keystorePassword
            storeType = "PKCS12"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystorePassword != null) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

// O workflow roda "testDebugUnitTest"; isso garante que os testes do motor (módulo JVM) rodem junto.
tasks.matching { it.name == "testDebugUnitTest" }.configureEach {
    dependsOn(":core:domain:test")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    debugImplementation(libs.compose.ui.tooling)
}
