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

fun secretFrom(path: String): String = path.takeIf { it.isNotBlank() }
    ?.let { rootProject.file(it) }
    ?.takeIf { it.isFile }
    ?.readText()
    ?.trim()
    .orEmpty()

val storeSdk = prop("matrux.storeSdk") == "true"
val vkidClientId = prop("matrux.vkid.clientId")
val payScheme = prop("matrux.payScheme").ifEmpty { "matruxpay" }

val matruxVersionName = prop("matrux.versionName")
val matruxVersionCode = prop("matrux.versionCode").toInt()

android {
    namespace = "ru.matrux.app"
    lint {
        abortOnError = false
        disable += "ExpiredTargetSdkVersion"
    }
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.matrux.app"
        minSdk = 26
        targetSdk = 30
        versionCode = matruxVersionCode
        versionName = matruxVersionName
        buildConfigField("String", "API_BASE", quoted(prop("matrux.apiBase")))
        buildConfigField("String", "MANIFEST_PUBKEY", quoted(prop("matrux.manifestPubkey")))
        buildConfigField("String", "PRODUCT_PRO_MONTH", quoted(prop("matrux.product1m")))
        buildConfigField("String", "PRODUCT_PRO_QUARTER", quoted(prop("matrux.product3m")))
        buildConfigField("String", "PRODUCT_PRO_HALF_YEAR", quoted(prop("matrux.product6m")))
        buildConfigField("String", "VKID_CLIENT_ID", quoted(vkidClientId))
        buildConfigField("String", "RUSTORE_APP_ID", quoted(prop("matrux.rustore.appId")))
        manifestPlaceholders += mapOf(
            "rustoreAppId" to prop("matrux.rustore.appId"),
            "payScheme" to payScheme,
            "VKIDClientID" to vkidClientId,
            "VKIDClientSecret" to secretFrom(prop("matrux.vkid.secretFile")),
            "VKIDRedirectHost" to "vk.ru",
            "VKIDRedirectScheme" to "vk$vkidClientId",
        )
        buildConfigField("String", "PRICE_PRO_MONTH", quoted(prop("matrux.priceProMonth")))
        buildConfigField("String", "PRICE_PRO_QUARTER", quoted(prop("matrux.priceProQuarter")))
        buildConfigField("String", "PRICE_PRO_HALF_YEAR", quoted(prop("matrux.priceProHalfYear")))
        buildConfigField("String", "PER_MONTH_PRO_MONTH", quoted(prop("matrux.perMonthProMonth")))
        buildConfigField("String", "PER_MONTH_PRO_QUARTER", quoted(prop("matrux.perMonthProQuarter")))
        buildConfigField("String", "PER_MONTH_PRO_HALF_YEAR", quoted(prop("matrux.perMonthProHalfYear")))
        buildConfigField("int", "TRIAL_DAYS", prop("matrux.trialDays").ifEmpty { "0" })
        buildConfigField("boolean", "TEST_UNLOCKED", prop("matrux.testUnlocked").ifEmpty { "false" })
        buildConfigField("boolean", "AUTH_REQUIRED", prop("matrux.authRequired").ifEmpty { "false" })
        resourceConfigurations += listOf("ru")
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
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

    sourceSets["main"].java.srcDir(if (storeSdk) "src/store/java" else "src/nostore/java")

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all { it.environment("ANDROID_HOME", android.sdkDirectory.absolutePath) }
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

    if (storeSdk) {
        implementation(platform("ru.rustore.sdk:bom:${prop("matrux.sdk.rustoreBom")}"))
        implementation("ru.rustore.sdk:pay")
        implementation("com.vk.id:vkid:${prop("matrux.sdk.vkid")}")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
    testImplementation("com.github.luben:zstd-jni:1.5.6-3")
}
