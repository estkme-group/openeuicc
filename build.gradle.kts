// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // The following Android-related plugins are already depended upon by buildSrc, hence unnecessary.
    // id("com.android.application") version "8.1.2" apply false
    // id("com.android.library") version "8.1.2" apply false
    //id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    //id("org.jetbrains.kotlin.multiplatform") version "2.4.10" apply false
}

tasks.register<Delete>("clean") {
    delete = setOf(rootProject.layout.buildDirectory)
}
