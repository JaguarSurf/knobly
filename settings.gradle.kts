rootProject.name = "knobly"

pluginManagement {
    includeBuild("build-logic")
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
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Apps
include(":app:android")
include(":app:desktop")
include(":app:shared")

// Features
include(":feature:home")
include(":feature:calls")
include(":feature:history")
include(":feature:stats")
include(":feature:settings")
include(":feature:onboarding")

// Domain
include(":domain:rules")
include(":domain:stats")
include(":domain:history")
include(":domain:settings")

// Core
include(":core:model")
include(":core:phone")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")

// Platform adapters
include(":platform:android-screening")
include(":platform:ios-calldirectory")
include(":platform:fake")
