import com.android.build.api.dsl.LibraryExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
}

configure<LibraryExtension> {
    namespace = "net.typeblog.lpac_jni"
    compileSdk = 37
    ndkVersion = "30.0.16248370" // from https://developer.android.com/ndk/downloads

    defaultConfig {
        minSdk = 27

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            ndkBuild {
                cFlags(
                    "-fmacro-prefix-map=${project.projectDir}=/fake/path/",
                    "-fdebug-prefix-map=${project.projectDir}=/fake/path/",
                    "-ffile-prefix-map=${project.projectDir}=/fake/path/"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    externalNativeBuild {
        ndkBuild {
            path("src/main/jni/lpac-jni.mk")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.kotlin.bom))
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
