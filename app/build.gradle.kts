import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing is resolved from (in order):
//   1. A gitignored keystore.properties at the repo root
//      (storeFile / storePassword / keyAlias / keyPassword), or
//   2. Environment variables XXAUTO_STORE_FILE / XXAUTO_STORE_PASSWORD /
//      XXAUTO_KEY_ALIAS / XXAUTO_KEY_PASSWORD.
// If neither is present the release build type stays unsigned (build an
// unsigned APK and sign it yourself, or use the debug build for testing).
// Signature-level suite doors (xx-launcher THEME_SYNC, the suite BACKUP door)
// only work when every xx app is signed with the same key, so the release
// config reads the family keystore rather than minting a key.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

fun signingValue(propKey: String, envKey: String): String? =
    keystoreProps.getProperty(propKey) ?: System.getenv(envKey)

val releaseStoreFile = signingValue("storeFile", "XXAUTO_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "XXAUTO_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "XXAUTO_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "XXAUTO_KEY_PASSWORD")
val hasReleaseSigning = !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.piercingxx.xxauto"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.piercingxx.xxauto"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    // AU1/AU12: two product flavors. `noGms` (listed first, so it is the
    // default) carries zero Google on the classpath and is what most installs
    // get. `gms` is the opt-in flavor that adds the Android Auto templated
    // media surface (src/gms). Both share one applicationId: they are the same
    // app, and xx-apps lists both variants with noGms as the default.
    flavorDimensions += "dist"
    productFlavors {
        create("noGms") {
            dimension = "dist"
            isDefault = true
        }
        create("gms") {
            dimension = "dist"
        }
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    testOptions {
        unitTests {
            // Let JVM unit tests instantiate android.jar stubs (BroadcastReceiver's
            // no-arg constructor) instead of throwing "Stub!" — same convention as
            // the suite. Behavior still comes through injected pure seams.
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)

    // Media3 session only — no exoplayer, no ui. xx-auto is a MediaController
    // over the suite players (AU4), never a player itself.
    implementation(libs.androidx.media3.session)

    // One DataStore file for all settings (AU11).
    implementation(libs.androidx.datastore.preferences)

    // AU12: the car-screen media surface. gms source set ONLY — the noGms
    // build must never see androidx.car.app (scripts/check-nogms.sh).
    "gmsImplementation"(libs.androidx.car.app)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
// The flavor split renames the unit-test tasks (testNoGmsDebugUnitTest,
// testGmsDebugUnitTest). The GATE in todo.md, CI and the operator contracts all
// run `testDebugUnitTest` (some with `--tests`), so keep that name as a real
// Test task over the default noGms variant's tests, and make it compile the gms
// car code too so a broken car surface fails the gate.
tasks.register<Test>("testDebugUnitTest") {
    group = "verification"
    description = "Debug unit tests (noGms variant) + gms compile check."
    val source = tasks.named<Test>("testNoGmsDebugUnitTest").get()
    testClassesDirs = source.testClassesDirs
    classpath = source.classpath
    dependsOn(source.dependsOn, "compileNoGmsDebugUnitTestKotlin", "compileGmsDebugKotlin")
}
