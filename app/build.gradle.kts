plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Local files are optional. CI values take precedence, including explicitly empty values.
fun configuration(environment: String): Map<String, String> {
    val file = rootProject.file(".env.$environment")
    return if (!file.exists()) emptyMap() else file.readLines().mapNotNull { line ->
        val text = line.trim()
        if (text.isEmpty() || text.startsWith("#")) null else {
            require('=' in text) { "Invalid environment assignment" }
            text.substringBefore('=').trim() to text.substringAfter('=').trim().removeSurrounding("\"")
        }
    }.toMap()
}
fun Map<String, String>.value(key: String, default: String): String = System.getenv(key) ?: get(key) ?: default
fun boolean(value: String): Boolean {
    require(value == "true" || value == "false") { "Boolean configuration must be true or false" }
    return value.toBooleanStrict()
}
fun quoted(value: String): String = "\"" + value.flatMap { character ->
    when (character) {
        '\\' -> "\\\\".toList()
        '"' -> "\\\"".toList()
        '\n' -> "\\n".toList()
        '\r' -> "\\r".toList()
        '\t' -> "\\t".toList()
        else -> { require(!character.isISOControl()) { "Control character in configuration" }; listOf(character) }
    }
}.joinToString("") + "\""

android {
    namespace = "com.example.base"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.example.base"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("releaseVersionCode").orNull?.toIntOrNull() ?: 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    flavorDimensions += "environment"
    productFlavors {
        listOf("dev", "production").forEach { environment ->
            create(environment) {
                dimension = "environment"
                if (environment == "dev") {
                    applicationIdSuffix = ".dev"
                    versionNameSuffix = "-dev"
                }
                val config = configuration(environment)
                val mode = config.value("DEMO_MODE", "offline")
                require(mode in setOf("offline", "public")) { "DEMO_MODE must be offline or public" }
                buildConfigField("String", "ENVIRONMENT", quoted(environment))
                buildConfigField("String", "DEMO_MODE", quoted(mode))
                buildConfigField("boolean", "SANDBOX", boolean(config.value("SANDBOX", (environment == "dev").toString())).toString())
                resValue("string", "app_name", config.value("APP_NAME", if (environment == "dev") "Base Example [DEV]" else "Base Example"))
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Deliberately unsigned until the new app supplies its signing configuration.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true; buildConfig = true; resValues = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    // Align app/test runtime graphs required by AndroidX Test 1.3.0.
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
