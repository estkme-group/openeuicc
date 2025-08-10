import im.angry.openeuicc.build.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

apply {
    plugin<MyVersioningPlugin>()
    plugin<MySigningPlugin>()
}

android {
    namespace = "im.angry.openeuicc"
    compileSdk = 35

    defaultConfig {
        applicationId = "im.angry.openeuicc"
        minSdk = 30
        targetSdk = 35

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        defaultConfig {
            versionNameSuffix = "-priv"
        }
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
    compileOnly(project(":libs:hidden-apis-stub"))
    implementation(project(":libs:hidden-apis-shim"))
    implementation(project(":libs:lpac-jni"))
    implementation(project(":app-common"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}