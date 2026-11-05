plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

// Artifact inspection only; never installs SDKs in the application.
if (providers.gradleProperty("verifyKits").orNull == "true") {
    val kitArtifacts = configurations.create("kitArtifacts") { isTransitive = false }
    dependencies { add(kitArtifacts.name, libs.paymentkit); add(kitArtifacts.name, libs.adskit) }
    tasks.register<Copy>("inspectKitArtifacts") {
        from(kitArtifacts)
        into(layout.buildDirectory.dir("kit-inspection"))
    }
}
