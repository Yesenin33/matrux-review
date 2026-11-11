pluginManagement {
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
        if (providers.gradleProperty("matrux.storeSdk").orNull == "true") {
            maven("https://nexus-external.rustore.ru/repository/maven-rustore-exposed")
            maven("https://nexus-external.vkteam.ru/repository/maven/")
        }
    }
}
rootProject.name = "matrux"
include(":app")
