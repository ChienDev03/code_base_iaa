pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google(); mavenCentral()
        if (providers.gradleProperty("verifyKits").orNull == "true") {
            maven {
                name = "BitbucketPackages"
                url = uri("https://maven.apkg.io/luantechnologi")
                credentials {
                    username = providers.gradleProperty("atlassianEmail").orElse(providers.environmentVariable("BITBUCKET_PACKAGES_USERNAME")).orNull
                    password = providers.gradleProperty("atlassianApiToken").orElse(providers.environmentVariable("BITBUCKET_PACKAGES_TOKEN")).orNull
                }
                content { includeGroup("com.lutech") }
            }
        }
    }
}
rootProject.name = "base-source"
include(":app")
