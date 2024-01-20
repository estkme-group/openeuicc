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

buildscript {
    repositories {
        maven("https://raw.githubusercontent.com/lineage-next/gradle-generatebp/4356136ecccc68cf6796a5dcd2388c66b80e0c11/.m2")
    }

    dependencies {
        classpath("org.lineageos:gradle-generatebp:+")
    }
}

rootProject.name = "OpenEUICC"
include(":app")
include(":libs:hidden-apis-stub")
include(":libs:hidden-apis-shim")
include(":libs:lpac-jni")
include(":app-common")
include(":app-unpriv")
include(":app-deps")
