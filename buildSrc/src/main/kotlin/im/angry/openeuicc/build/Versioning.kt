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
        } catch (e: Exception) {
            0
        }

val Project.gitVersionName: String
    get() =
        try {
            val stdout = ByteArrayOutputStream()
            exec {
                commandLine("git", "describe", "--always", "--tags", "--dirty")
                standardOutput = stdout
            }
            stdout.toString("utf-8").trim('\n')
        } catch (e: Exception) {
            "Unknown"
        }

class MyVersioningPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        target.configure<BaseAppModuleExtension> {
            defaultConfig {
                versionCode = target.gitVersionCode
                versionName = target.gitVersionName
            }

            applicationVariants.all {
                if (name == "debug") {
                    outputs.forEach {
                        (it as ApkVariantOutputImpl).versionCodeOverride =
                            (System.currentTimeMillis() / 1000).toInt()
                    }
                }
            }
        }
    }
}