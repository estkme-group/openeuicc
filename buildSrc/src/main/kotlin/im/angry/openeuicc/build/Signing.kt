package im.angry.openeuicc.build

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import java.util.Properties

interface KeystorePropertiesExtension {
    var storeFileField: String?
    var storePasswordField: String?
    var keyAliasField: String?
    var keyPasswordField: String?
}

fun Project.signingKeystoreProperties(action: KeystorePropertiesExtension.() -> Unit) {
    extensions.create<KeystorePropertiesExtension>("keystoreProperties")
    configure<KeystorePropertiesExtension>(action)
}

class MySigningPlugin : Plugin<Project> {
    private val configName = "default"

    override fun apply(project: Project) {
        val ext = project.extensions.findByType(KeystorePropertiesExtension::class.java)
        project.configure<ApplicationExtension> {
            val properties = project.keystoreProperties ?: return@configure // Skip if not found

            signingConfigs {
                create(configName) {
                    storeFile = project.file(properties.getProperty(ext?.storeFileField ?: "storeFile"))
                    storePassword = properties.getProperty(ext?.storePasswordField ?: "storePassword")
                    keyAlias = properties.getProperty(ext?.keyAliasField ?: "keyAlias")
                    keyPassword = properties.getProperty(ext?.keyPasswordField ?: "keyPassword")
                }
            }

            buildTypes.configureEach {
                signingConfig = signingConfigs.getByName(configName)
            }
        }
    }
}

private val Project.keystoreProperties: Properties?
    get() {
        return try {
            Properties().apply {
                rootProject.file("keystore.properties").inputStream()
                    .use { load(it) }
                val storeFile = checkNotNull(getProperty("storeFile")) {
                    "storeFile not found in keystore.properties"
                }
                check(rootProject.file(storeFile).exists()) {
                    "Keystore file $storeFile does not exist"
                }
            }
        } catch (_: Exception) {
            null
        }
    }
