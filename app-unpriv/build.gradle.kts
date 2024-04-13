import im.angry.openeuicc.build.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

signingKeystoreProperties {
    keyAliasField = "unprivKeyAlias"
    keyPasswordField = "unprivKeyPassword"
}

apply {
    plugin<MyVersioningPlugin>()
    plugin<MySigningPlugin>()
}

android {
    namespace = "im.angry.easyeuicc"
    compileSdk = 34
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "im.angry.easyeuicc"
        minSdk = 28
        targetSdk = 34
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":app-common"))
}