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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "toss-watch"
include(":app")
include(":core:model")
include(":core:common")
include(":core:network")
include(":core:datastore")
include(":core:database")
include(":core:designsystem")
include(":feature:auth")
include(":feature:dashboard")
include(":feature:setting")
include(":feature:tosskey")
