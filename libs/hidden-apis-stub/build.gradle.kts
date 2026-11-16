import com.android.build.api.dsl.LibraryExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.library")
}

configure<LibraryExtension> {
    compileSdk = 37
    namespace = "im.angry.hidden.apis"
}

dependencies {
    implementation(libs.jetbrains.annotations)
}
