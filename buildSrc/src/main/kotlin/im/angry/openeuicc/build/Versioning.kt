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
            val stdout = ByteArrayOutputStream()
            exec {
                commandLine("git", "rev-list", "--first-parent", "--count", "HEAD")
                standardOutput = stdout
            }
            stdout.toString("utf-8").trim('\n').toInt()
        } catch (_: Exception) {
            0
        }

fun Project.getGitVersionName(vararg args: String): String =
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "describe", "--always", "--tags", "--dirty", *args)
            standardOutput = stdout
        }
        stdout.toString("utf-8").trim('\n').removePrefix("unpriv-")
    } catch (_: Exception) {
        "Unknown"
    }


class MyVersioningPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configure<BaseAppModuleExtension> {
            defaultConfig {
                versionCode = target.gitVersionCode
                versionName = target.getGitVersionName()
            }

            applicationVariants.all {
                if (name == "debug") {
                    outputs.forEach {
                        with(it as ApkVariantOutputImpl) {
                            versionCodeOverride = (System.currentTimeMillis() / 1000).toInt()
                            // always keep the format: <tag>-<commits>-g<hash>[-dirty]
                            versionNameOverride = target.getGitVersionName("--long")
                        }
                    }
                }
            }
        }
    }
}
