package im.angry.openeuicc.build

import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class MagiskModuleDirTask : DefaultTask() {
    @get:Input
    abstract val variant : Property<String>

    @get:Input
    abstract val appName : Property<String>

    @get:InputFile
    abstract val permsFile : Property<File>

    @get:InputFile
    abstract val moduleInstaller : Property<File>

    @get:Input
    abstract val moduleCustomizeScriptText : Property<String>

    @get:Input
    abstract val moduleUninstallScriptText : Property<String>

    @get:Input
    abstract val moduleProp : MapProperty<String, String>

    @InputDirectory
    val inputDir = variant.map { project.layout.buildDirectory.dir("outputs/apk/${it}") }

    @OutputDirectory
    val outputDir = variant.map { project.layout.buildDirectory.dir("magisk/${it}") }

    @TaskAction
    fun build() {
        val dir = outputDir.get().get()
        project.mkdir(dir)
        val systemExtDir = dir.dir("system/system_ext")
        val permDir = dir.dir("system/system_ext/etc/permissions")
        val appDir = systemExtDir.dir("priv-app/${appName.get()}")
        val metaInfDir = dir.dir("META-INF/com/google/android")
        project.mkdir(systemExtDir)
        project.mkdir(metaInfDir)
        project.mkdir(appDir)
        project.mkdir(permDir)
        project.copy {
            into(appDir)
            from(inputDir) {
                include("app-${variant.get()}.apk")
                rename("app-${variant.get()}.apk", "${appName.get()}.apk")
            }
        }
        project.copy {
            from(permsFile)
            into(permDir)
        }
        project.copy {
            from(moduleInstaller)
            into(metaInfDir)
            rename(".*", "update-binary")
        }
        dir.file("customize.sh").asFile.writeText(moduleCustomizeScriptText.get())
        dir.file("uninstall.sh").asFile.writeText(moduleUninstallScriptText.get())
        metaInfDir.file("updater-script").asFile.writeText("# MAGISK")
        dir.file("module.prop").asFile.writeText(moduleProp.get().map { (k, v) -> "$k=$v" }.joinToString("\n"))
    }
}