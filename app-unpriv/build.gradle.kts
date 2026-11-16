import com.android.build.api.dsl.ApplicationExtension
import im.angry.openeuicc.build.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
}

signingKeystoreProperties {
    keyAliasField = "unprivKeyAlias"
    keyPasswordField = "unprivKeyPassword"
}

pluginManager.apply(MyVersioningPlugin::class.java)
pluginManager.apply(MySigningPlugin::class.java)

configure<ApplicationExtension> {
    namespace = "im.angry.easyeuicc"
    compileSdk = 37

    defaultConfig {
        applicationId = "im.angry.easyeuicc"
        minSdk = 28
        targetSdk = 37

        emitAssetStatements("https://easyeuicc.org", "https://preview.easyeuicc.org")
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    buildTypes {
        defaultConfig {
            versionNameSuffix = "-unpriv"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":app-common"))
    testImplementation(libs.junit4)
}
