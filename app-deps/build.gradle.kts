import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.lineageos.generatebp.GenerateBpPlugin
import org.lineageos.generatebp.GenerateBpPluginExtension
import org.lineageos.generatebp.models.Module

plugins {
    id("com.android.library")
}

pluginManager.apply(GenerateBpPlugin::class.java)

configure<LibraryExtension> {
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
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.material)
    api(libs.androidx.constraintlayout)
    //noinspection KtxExtensionAvailable
    api(libs.androidx.preference)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.service)
    api(libs.androidx.swiperefreshlayout)
    api(libs.androidx.cardview)
    api(libs.androidx.viewpager2)
    api(libs.androidx.datastore.preferences)
    api(libs.zxing.android.embedded)
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

val libraryExtension = extensions.getByType(LibraryExtension::class.java)

configure<GenerateBpPluginExtension> {
    targetSdk.set(checkNotNull(libraryExtension.compileSdk) { "compileSdk must be configured for app-deps" })
    availableInAOSP.set { module: Module ->
        when {
            module.group.startsWith("androidx") -> true
            module.group == "com.google.android.material" -> true
            module.group.startsWith("org.jetbrains") -> true
            else -> false
        }
    }
}
