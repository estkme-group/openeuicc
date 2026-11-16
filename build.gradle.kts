// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // The following Android-related plugins are already depended upon by buildSrc, hence unnecessary.
    // id("com.android.application") version "8.1.2" apply false
    // id("com.android.library") version "8.1.2" apply false
    //id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    //id("org.jetbrains.kotlin.multiplatform") version "2.4.10" apply false
}

tasks.register<Delete>("clean") {
    description = "Cleans the build directory of the root project."
    group = "build"
    delete = setOf(rootProject.layout.buildDirectory)
}

// Print per-test results (which tests ran, passed, failed, were skipped)
// instead of Gradle's default summary-only output.
subprojects {
    tasks.withType<Test>().configureEach {
        testLogging {
            events("passed", "skipped", "failed")
            // Uncomment to also capture each test's stdout/stderr:
            // showStandardStreams = true
        }
    }
}
