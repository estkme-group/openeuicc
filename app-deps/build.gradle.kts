import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.lineageos.generatebp.GenerateBpPlugin
import org.lineageos.generatebp.GenerateBpPluginExtension
import org.lineageos.generatebp.models.Module

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

apply {
    plugin<GenerateBpPlugin>()
}

android {
    namespace = "im.angry.openeuicc_deps"
    compileSdk = 37

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    api("androidx.core:core-ktx:1.19.0")
    api("androidx.appcompat:appcompat:1.7.1")
    api("com.google.android.material:material:1.14.0")
    api("androidx.constraintlayout:constraintlayout:2.2.2")
    //noinspection KtxExtensionAvailable
    api("androidx.preference:preference:1.2.1")
    api("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    api("androidx.lifecycle:lifecycle-service:2.11.0")
    api("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    api("androidx.cardview:cardview:1.0.0")
    api("androidx.viewpager2:viewpager2:1.1.0")
    api("androidx.datastore:datastore-preferences:1.2.1")
    api("com.journeyapps:zxing-android-embedded:4.3.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

configure<GenerateBpPluginExtension> {
    targetSdk.set(android.compileSdk!!)
    availableInAOSP.set { module: Module ->
        when {
            module.group == "androidx.datastore" -> false
            module.group.startsWith("androidx") -> true
            module.group == "com.google.android.material" -> true
            module.group.startsWith("org.jetbrains") -> true
            else -> false
        }
    }
}
