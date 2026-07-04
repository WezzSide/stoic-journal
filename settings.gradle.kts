pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "StoicJournal"
include(":app")
include(":feature:today")
include(":feature:journal")
include(":feature:library")
include(":feature:history")
include(":feature:settings")
include(":feature:onboarding")
include(":core:data")
include(":core:domain")
include(":core:common")
include(":core:notifications")
