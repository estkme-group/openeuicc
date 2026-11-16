import com.android.build.api.dsl.ApplicationExtension
import im.angry.openeuicc.build.MyMagiskPlugin
import im.angry.openeuicc.build.MySigningPlugin
import im.angry.openeuicc.build.MyVersioningPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
}

pluginManager.apply(MyVersioningPlugin::class.java)
pluginManager.apply(MySigningPlugin::class.java)
pluginManager.apply(MyMagiskPlugin::class.java)

configure<ApplicationExtension> {
    namespace = "im.angry.openeuicc"
    compileSdk = 37

    defaultConfig {
        applicationId = "im.angry.openeuicc"
        minSdk = 31
        targetSdk = 37

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

    testOptions {
        unitTests {
            // Robolectric needs real resources (task titles, dialog themes)
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        // Reuse app-common's Robolectric mocks (EuiccChannel / EuiccChannelManager / test application)
        getByName("test").java.directories.add("../app-common/src/test/java")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (name.contains("UnitTest")) {
        source("../app-common/src/test/java")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    @Suppress("AvoidDuplicateDependencies")
    compileOnly(project(":libs:hidden-apis-stub"))
    implementation(project(":libs:hidden-apis-shim"))
    implementation(project(":libs:lpac-jni"))
    implementation(project(":app-common"))
    // This is a massiave hack. We need testImplementation because the test classes
    // need to be loadable against only public android.jar. However, once Robolectic
    // starts running tests, the real classes from android-all.jar take precedence.
    // FIXME: Probably make it so that this is not utterly fragile...
    @Suppress("AvoidDuplicateDependencies")
    testImplementation(project(":libs:hidden-apis-stub"))
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

