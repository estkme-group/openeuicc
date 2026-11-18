package im.angry.openeuicc.build

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class MyVersioningPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.configure<ApplicationExtension> {
            defaultConfig {
                versionCode = project.gitVersionCode
                // format: <tag>[-<commits>-g<hash>][-dirty][-suffix]
                versionName = project.getGitVersionName()
            }
        }

        val debugVersionNameSuffix = project.extensions
            .findByType(ApplicationExtension::class.java)
            ?.buildTypes?.findByName("debug")
            ?.versionNameSuffix.orEmpty()

        val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
        androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) { variant ->
            // FIXME: versionCode is a signed 32-bit int. Using Unix seconds here reaches (overflows in 2038).
            val versionCode = System.currentTimeMillis() / 1000
            check(versionCode < Int.MAX_VALUE) {
                "versionCode is signed 32 bits number, but it is too big: $versionCode"
            }
            // format: <tag>-<commits>-g<hash>[-dirty][-suffix]
            val versionName = project.getGitVersionName("--long")
            variant.outputs.forEach { output ->
                output.versionCode.set(versionCode.toInt())
                output.versionName.set(versionName + debugVersionNameSuffix)
            }
        }
    }
}

val Project.gitVersionCode: Int
    get() =
        try {
            providers.exec {
                commandLine("git", "rev-list", "--first-parent", "--count", "HEAD")
            }.standardOutput.asText.get().trim('\n').toInt()
        } catch (_: Exception) {
            0
        }

fun Project.getGitVersionName(vararg args: String): String =
    try {
        providers.exec {
            commandLine("git", "describe", "--always", "--tags", "--dirty", *args)
        }.standardOutput.asText.get().trim('\n').removePrefix("unpriv-")
    } catch (_: Exception) {
        "Unknown"
    }
