import com.android.build.gradle.internal.api.ApkVariantOutputImpl
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
        it["versionCode"] = "${(android.applicationVariants.find { it.name == "debug" }!!.outputs.first() as ApkVariantOutputImpl).versionCodeOverride}"
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