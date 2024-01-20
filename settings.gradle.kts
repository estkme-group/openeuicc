pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "OpenEUICC"
include(":app")
include(":libs:hidden-apis-stub")
include(":libs:hidden-apis-shim")
include(":libs:lpac-jni")
include(":app-common")
include(":app-unpriv")
