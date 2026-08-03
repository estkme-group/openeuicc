package im.angry.openeuicc.build

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.ByteArrayOutputStream

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


class MyVersioningPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configure<BaseAppModuleExtension> {
            defaultConfig {
                versionCode = target.gitVersionCode
                // format: <tag>[-<commits>-g<hash>][-dirty][-suffix]
                versionName = target.getGitVersionName()
            }

            applicationVariants.all {
                if (name == "debug") {
                    val versionCode = (System.currentTimeMillis() / 1000).toInt()
                    // format: <tag>-<commits>-g<hash>[-dirty][-suffix]
                    val versionName = target.getGitVersionName("--long")
                    val versionNameSuffix = mergedFlavor.versionNameSuffix
                    outputs.forEach {
                        with(it as ApkVariantOutputImpl) {
                            versionCodeOverride = versionCode
                            versionNameOverride = versionName + versionNameSuffix
                        }
                    }
                }
            }
        }
    }
}
