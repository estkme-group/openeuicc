package im.angry.openeuicc.build

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.invoke
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

val Project.keystoreProperties: Properties?
    get() {
        try {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            return Properties().apply { load(FileInputStream(keystorePropertiesFile)) }
        } catch (e: FileNotFoundException) {
            return null
        }
    }

interface KeystorePropertiesExtension {
    var storeFileField: String?
    var storePasswordField: String?
    var keyAliasField: String?
    var keyPasswordField: String?
}

fun Project.signingKeystoreProperties(_configure: Action<KeystorePropertiesExtension>) {
    extensions.create<KeystorePropertiesExtension>("keystoreProperties")
    configure<KeystorePropertiesExtension> {
        _configure(this)
    }
}

class MySigningPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val ext = target.extensions.findByType(KeystorePropertiesExtension::class.java)
        target.configure<BaseAppModuleExtension> {
            target.keystoreProperties?.let { keystore ->
                signingConfigs {
                    create("config") {
                        storeFile = target.file(keystore[ext?.storeFileField ?: "storeFile"]!!)
                        storePassword = keystore[ext?.storePasswordField ?: "storePassword"] as String?
                        keyAlias = keystore[ext?.keyAliasField ?: "keyAlias"] as String?
                        keyPassword = keystore[ext?.keyPasswordField ?: "keyPassword"] as String?
                    }
                }

                buildTypes {
                    debug {
                        signingConfig = signingConfigs.getByName("config")
                    }
                    release {
                        signingConfig = signingConfigs.getByName("config")
                    }
                }
            }
        }
    }
}
