import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("app.cash.paparazzi")
}

fun prop(name: String): String = (project.findProperty(name) as String?)?.trim().orEmpty()
fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val signingFile = rootProject.file("keystore.properties")
val signing = Properties().apply {
    if (signingFile.isFile) signingFile.inputStream().use { load(it) }
}

val palmaVersionName = prop("palma.versionName")
val palmaVersionCode = prop("palma.versionCode").toInt()

android {
    namespace = "ru.palma.app"
    lint {
        abortOnError = false
        disable += "ExpiredTargetSdkVersion"
    }
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.palma.app"
        minSdk = 26
        targetSdk = 30
        versionCode = palmaVersionCode
        versionName = palmaVersionName
        buildConfigField("String", "API_BASE", quoted(prop("palma.apiBase")))
        buildConfigField("String", "MANIFEST_PUBKEY", quoted(prop("palma.manifestPubkey")))
        buildConfigField("String", "PRODUCT_PRO_MONTH", quoted(prop("palma.productProMonth")))
        buildConfigField("String", "PRODUCT_PRO_YEAR", quoted(prop("palma.productProYear")))
        buildConfigField("String", "PRODUCT_LIFETIME", quoted(prop("palma.productLifetime")))
        buildConfigField("String", "PRICE_PRO_MONTH", quoted(prop("palma.priceProMonth")))
        buildConfigField("String", "PRICE_PRO_YEAR", quoted(prop("palma.priceProYear")))
        buildConfigField("String", "PRICE_LIFETIME", quoted(prop("palma.priceLifetime")))
        buildConfigField("int", "TRIAL_DAYS", prop("palma.trialDays").ifEmpty { "0" })
        buildConfigField("boolean", "TEST_UNLOCKED", prop("palma.testUnlocked").ifEmpty { "false" })
        resourceConfigurations += listOf("ru")
    }

    signingConfigs {
        create("release") {
            if (signing.isNotEmpty()) {
                storeFile = file(signing.getProperty("storeFile"))
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (signing.isNotEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += listOf("META-INF/versions/9/OSGI-INF/MANIFEST.MF", "META-INF/DEPENDENCIES")
        jniLibs.useLegacyPackaging = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.tukaani:xz:1.9")
    implementation("com.github.luben:zstd-jni:1.5.6-3@aar")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
    testImplementation("com.github.luben:zstd-jni:1.5.6-3")
}
