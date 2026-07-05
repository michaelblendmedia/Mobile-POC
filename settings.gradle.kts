pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Salesforce Marketing Cloud SDK Maven repository
        maven { url = uri("https://salesforce-marketingcloud.github.io/MarketingCloudSDK-Android/repository") }
    }
}

rootProject.name = "SFMC Register"
include(":app")
