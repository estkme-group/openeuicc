package im.angry.openeuicc.build

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression
import java.io.File
import javax.inject.Inject

class MyMagiskPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val appName = project.rootProject.name
        val applicationExtension = project.extensions.getByType(ApplicationExtension::class.java)
        val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidComponents.onVariants { variant ->
            val buildType = variant.buildType ?: return@onVariants
            val variantName = variant.name
            val taskName = "assemble" + variantName.replaceFirstChar { it.uppercase() }

            val defaultConfig = applicationExtension.defaultConfig
            val applicationId = checkNotNull(defaultConfig.applicationId) {
                "applicationId MUST BE configured before creating Magisk module metadata"
            }
            val versionName = checkNotNull(defaultConfig.versionName) {
                "versionName MUST BE configured before creating Magisk module metadata"
            }
            val versionCode = project.objects
                .property(String::class.java)
                .convention(defaultConfig.versionCode?.toString() ?: "1")
            variant.outputs.singleOrNull()?.let { output ->
                versionCode.set(output.versionCode.map { it.toString() })
            }

            val moduleProperties = mapOf(
                "id" to applicationId,
                "name" to appName,
                "description" to "($buildType build) $appName is an open-source app that provides system-level eSIM integration.",
                "version" to versionName,
                "versionCode" to versionCode.get(),
                "author" to "$appName authors",
                "updateJson" to "https://openeuicc.com/magisk/magisk-$buildType.json"
            )

            val moduleCustomizeScript = project.file("magisk/customize.sh").readText()
                .replace("{APK_NAME}", appName)
                .replace("{PKG_NAME}", applicationId)

            val moduleUninstallScript = project.file("magisk/uninstall.sh").readText()
                .replace("{PKG_NAME}", applicationId)

            val moduleDirTask = project.tasks.register("${taskName}MagiskModuleDir", MagiskModuleDirTask::class.java) {
                this.variant.set(variantName)
                this.appName.set(appName)
                permsFile.set(project.rootProject.file("privapp_whitelist_im.angry.openeuicc.xml"))
                moduleInstaller.set(project.file("magisk/module_installer.sh"))
                moduleCustomizeScriptText.set(moduleCustomizeScript)
                moduleUninstallScriptText.set(moduleUninstallScript)
                moduleProp.putAll(moduleProperties)
                dependsOn(taskName)
            }

            project.tasks.register("${taskName}MagiskModule", Zip::class.java) {
                dependsOn(moduleDirTask)
                from(moduleDirTask.map { it.outputDir })
                archiveFileName.set("magisk-$variantName.zip")
                destinationDirectory.set(project.layout.buildDirectory.dir("magisk"))
                entryCompression = ZipEntryCompression.STORED
            }
        }
    }

}

private abstract class MagiskModuleDirTask : DefaultTask() {
    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Input
    abstract val variant: Property<String>

    @get:Input
    abstract val appName: Property<String>

    @get:InputFile
    abstract val permsFile: Property<File>

    @get:InputFile
    abstract val moduleInstaller: Property<File>

    @get:Input
    abstract val moduleCustomizeScriptText: Property<String>

    @get:Input
    abstract val moduleUninstallScriptText: Property<String>

    @get:Input
    abstract val moduleProp: MapProperty<String, String>

    @InputDirectory
    val inputDir = variant.flatMap { layout.buildDirectory.dir("outputs/apk/$it") }

    @OutputDirectory
    val outputDir = variant.flatMap { layout.buildDirectory.dir("magisk/$it") }

    @TaskAction
    fun build() {
        val dir = outputDir.get().asFile
        val systemExtDir = dir.resolve("system/system_ext")
        val permDir = dir.resolve("system/system_ext/etc/permissions")
        val appDir = systemExtDir.resolve("priv-app/${appName.get()}")
        val metaInfDir = dir.resolve("META-INF/com/google/android")

        permDir.mkdirs()
        appDir.mkdirs()
        metaInfDir.mkdirs()

        fs.copy {
            into(appDir)
            from(inputDir) {
                include("app-${variant.get()}.apk")
                rename("app-${variant.get()}.apk", "${appName.get()}.apk")
            }
        }
        fs.copy {
            from(permsFile.get())
            into(permDir)
        }
        fs.copy {
            from(moduleInstaller.get())
            into(metaInfDir)
            rename(".*", "update-binary")
        }
        dir.resolve("customize.sh").writeText(moduleCustomizeScriptText.get())
        dir.resolve("uninstall.sh").writeText(moduleUninstallScriptText.get())
        metaInfDir.resolve("updater-script").writeText("#MAGISK\n")
        dir.resolve("module.prop").writeText(moduleProp.get().map { (k, v) -> "$k=$v" }.joinToString("\n"))
    }
}
