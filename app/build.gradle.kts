import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import im.angry.openeuicc.build.MagiskModuleDirTask
import im.angry.openeuicc.build.MySigningPlugin
import im.angry.openeuicc.build.MyVersioningPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    compileSdk = 37

    defaultConfig {
        applicationId = "im.angry.openeuicc"
        minSdk = 30
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
        getByName("test").java.srcDir("../app-common/src/test/java/im/angry/openeuicc/testutil")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    compileOnly(project(":libs:hidden-apis-stub"))
    implementation(project(":libs:hidden-apis-shim"))
    implementation(project(":libs:lpac-jni"))
    implementation(project(":app-common"))
    // Lets JUnit load test classes that mention EuiccService outside the Robolectric sandbox;
    // inside it, the real classes from android-all take precedence
    testImplementation(project(":libs:hidden-apis-stub"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17-beta-4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

val modulePropsTemplate = mutableMapOf(
    "id" to android.defaultConfig.applicationId!!,
    "name" to "OpenEUICC",
    "version" to android.defaultConfig.versionName!!,
    "versionCode" to "${android.defaultConfig.versionCode}",
    "author" to "OpenEUICC authors",
    "description" to "OpenEUICC is an open-source app that provides system-level eSIM integration."
)

val moduleCustomizeScript = project.file("magisk/customize.sh").readText()
    .replace("{APK_NAME}", "OpenEUICC")
    .replace("{PKG_NAME}", android.defaultConfig.applicationId!!)

val moduleUninstallScript = project.file("magisk/uninstall.sh").readText()
    .replace("{PKG_NAME}", android.defaultConfig.applicationId!!)

tasks.register<MagiskModuleDirTask>("assembleDebugMagiskModuleDir") {
    variant = "debug"
    appName = "OpenEUICC"
    permsFile = project.rootProject.file("privapp_whitelist_im.angry.openeuicc.xml")
    moduleInstaller = project.file("magisk/module_installer.sh")
    moduleCustomizeScriptText = moduleCustomizeScript
    moduleUninstallScriptText = moduleUninstallScript
    moduleProp = modulePropsTemplate.let {
        it["description"] = "(debug build) ${it["description"]}"
        it["versionCode"] = (android.applicationVariants
            .find { v -> v.name == "debug" }!!
            .outputs
            .first() as ApkVariantOutputImpl)
            .versionCodeOverride.toString()
        it["updateJson"] = "https://openeuicc.com/magisk/magisk-debug.json"
        it
    }
    dependsOn("assembleDebug")
}

tasks.register<Zip>("assembleDebugMagiskModule") {
    dependsOn("assembleDebugMagiskModuleDir")
    from((tasks.getByName("assembleDebugMagiskModuleDir") as MagiskModuleDirTask).outputDir)
    archiveFileName = "magisk-debug.zip"
    destinationDirectory = project.layout.buildDirectory.dir("magisk")
    entryCompression = ZipEntryCompression.STORED
}

tasks.register<MagiskModuleDirTask>("assembleReleaseMagiskModuleDir") {
    variant = "release"
    appName = "OpenEUICC"
    permsFile = project.rootProject.file("privapp_whitelist_im.angry.openeuicc.xml")
    moduleInstaller = project.file("magisk/module_installer.sh")
    moduleCustomizeScriptText = moduleCustomizeScript
    moduleUninstallScriptText = moduleUninstallScript
    moduleProp = modulePropsTemplate
    dependsOn("assembleRelease")
}

tasks.register<Zip>("assembleReleaseMagiskModule") {
    dependsOn("assembleReleaseMagiskModuleDir")
    from((tasks.getByName("assembleReleaseMagiskModuleDir") as MagiskModuleDirTask).outputDir)
    archiveFileName = "magisk-release.zip"
    destinationDirectory = project.layout.buildDirectory.dir("magisk")
    entryCompression = ZipEntryCompression.STORED
}
