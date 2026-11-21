package im.angry.openeuicc.build

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import java.io.File
import java.io.Serializable
import javax.inject.Inject
import kotlin.text.replace

class MagiskModulePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val applicationExt = project.extensions.getByType(ApplicationExtension::class.java)
        val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
        // Magisk Module are generated only for debug builds
        // Release builds are intended for AOSP integration
        androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) { variant ->
            val assembleName = "assemble${variant.name.replaceFirstChar { it.uppercase() }}"
            val taskName = "${assembleName}MagiskModule"
            val dirName = "${taskName}Dir"
            val output = checkNotNull(variant.outputs.firstOrNull()) { "The variant ${variant.name} has no outputs" }
            val moduleConfiguration = MagiskModuleConfiguration(
                appName = project.rootProject.name,
                pkgName = checkNotNull(applicationExt.defaultConfig.applicationId),
                versionName = checkNotNull(output.versionName.orNull) { "The variant ${variant.name} has no versionName" },
                versionCode = checkNotNull(output.versionCode.orNull) { "The variant ${variant.name} has no versionCode" },
                buildType = checkNotNull(variant.buildType),
            )
            val moduleDirTask = project.tasks.register(dirName, MagiskModuleDirTask::class.java) {
                description = "Create Magisk module directory for ${variant.name} variant"
                dependsOn(assembleName)
                configuration.set(moduleConfiguration)
                apkDir.set(variant.artifacts.get(SingleArtifact.APK))
                artifactLoader = variant.artifacts.getBuiltArtifactsLoader()
                with(project.layout) {
                    outputDir.set(buildDirectory.dir("magisk/${variant.name}"))
                    magiskDir.set(projectDirectory.dir("magisk"))
                }
                with(project.rootProject.layout) {
                    whitelistFile.set(projectDirectory.file("privapp_whitelist_im.angry.openeuicc.xml"))
                }
            }
            project.tasks.register(taskName, Zip::class.java) {
                description = "Create Magisk module for ${variant.name} variant"
                dependsOn(moduleDirTask)
                from(moduleDirTask.flatMap { it.outputDir })
                archiveFileName.set("magisk-${variant.name}.zip")
                destinationDirectory.set(project.layout.buildDirectory.dir("magisk"))
                entryCompression = ZipEntryCompression.DEFLATED
            }
        }
    }
}

private data class MagiskModuleConfiguration(
    val appName: String,
    val pkgName: String,
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
) : Serializable

private abstract class MagiskModuleDirTask : DefaultTask() {
    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Input
    abstract val configuration: Property<MagiskModuleConfiguration>

    @get:InputFile
    abstract val whitelistFile: RegularFileProperty

    @get:InputDirectory
    abstract val magiskDir: DirectoryProperty

    @get:InputDirectory
    abstract val apkDir: DirectoryProperty

    @get:Internal
    abstract var artifactLoader: BuiltArtifactsLoader

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun build() {
        // FIXME: Migrate to new overlay system
        // See <https://topjohnwu.github.io/Magisk/guides.html#root-directory-overlay-system>
        val config = configuration.get()
        val rootDir = outputDir.get().asFile
        val permPath = rootDir.resolve("system/system_ext/etc/permissions/privapp_whitelist_${config.pkgName}.xml")
        val appPath = rootDir.resolve("system/system_ext/priv-app/${config.appName}/${config.appName}.apk")
        val metaDir = rootDir.resolve("META-INF/com/google/android")

        fs.delete {
            delete(rootDir)
        }
        fs.copy {
            val artifacts = artifactLoader.load(apkDir.get())
                ?: throw RuntimeException("Cannot load APK artifacts")
            from(artifacts.elements.first().outputFile) { rename { appPath.name } }
            into(appPath.parentFile)
        }
        fs.copy {
            from(whitelistFile)
            into(permPath.parentFile)
        }
        fs.copy {
            from(magiskDir) { include("update-binary", "updater-script") }
            into(metaDir)
        }
        for (name in listOf("module.prop", "customize.sh", "uninstall.sh")) {
            val formatted = formatText(magiskDir.file(name).get().asFile.readText())
            rootDir.resolve(name).writeText(formatted)
        }
    }

    private fun formatText(text: String) = with(configuration.get()) {
        text
            .replace("{APP_NAME}", appName)
            .replace("{PKG_NAME}", pkgName)
            .replace("{BUILD_TYPE}", buildType)
            .replace("{VERSION_NAME}", versionName)
            .replace("{VERSION_CODE}", versionCode.toString())
    }
}
