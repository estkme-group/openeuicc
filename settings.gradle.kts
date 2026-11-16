pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

buildscript {
    repositories {
        maven("https://raw.githubusercontent.com/lineage-next/gradle-generatebp/1e4404ddbdff066a2a95de77c1e7515f77c394cc/.m2")
    }

    dependencies {
        classpath("org.lineageos:gradle-generatebp:1.33")
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
