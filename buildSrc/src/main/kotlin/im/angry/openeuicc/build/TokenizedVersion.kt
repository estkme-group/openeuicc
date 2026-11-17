package im.angry.openeuicc.build

import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.Key
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.util.Base64

class TokenizedVersionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val applicationExt = project.extensions.getByType(ApplicationExtension::class.java)
        val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            val buildType = checkNotNull(applicationExt.buildTypes.findByName(variant.buildType.orEmpty())) {
                "The variant ${variant.name} has no build type"
            }
            val output = checkNotNull(variant.outputs.firstOrNull()) {
                "The variant ${variant.name} has no outputs"
            }
            val versionCode = checkNotNull(output.versionCode.orNull) {
                "The variant ${variant.name} has no version code"
            }
            val versionName = checkNotNull(output.versionName.orNull) {
                "The variant ${variant.name} has no version name"
            }
            val payload = buildPayload {
                writeUTF(buildType.name)
                writeUTF(versionName)
                writeLong(versionCode.toLong())
            }
            val message = buildMessage(buildType.signingConfig?.getKey(), payload)
            val token = Base64.getUrlEncoder().withoutPadding().encodeToString(message)
            variant.buildConfigFields?.put(
                "TOKENIZED_VERSION",
                BuildConfigField("String", "\"$token\"", null),
            )
        }
    }
}

private fun buildPayload(builderAction: DataOutputStream.() -> Unit) = ByteArrayOutputStream().use { out ->
    DataOutputStream(out).use(block = builderAction)
    out.toByteArray()
}

private fun buildMessage(key: Key?, payload: ByteArray) = buildPayload {
    val privateKey = key as? PrivateKey
    // 0x00: unsigned, 0x01: signed
    writeByte(if (privateKey == null) 0 else 1)
    writeBlock(payload)
    if (privateKey == null) return@buildPayload
    writeBlock(Signature.getInstance("SHA256with${key.algorithm}").let {
        it.initSign(privateKey)
        it.update(payload)
        it.sign()
    })
}

private fun DataOutputStream.writeBlock(block: ByteArray) {
    writeShort(block.size)
    write(block)
}

private fun ApkSigningConfig.getKey(): Key {
    val storeFile = checkNotNull(storeFile) { "Signing config storeFile is required." }
    val storePassword = checkNotNull(storePassword) { "Signing config storePassword is required." }.toCharArray()
    val keyAlias = checkNotNull(keyAlias) { "Signing config keyAlias is required." }
    val keyPassword = checkNotNull(keyPassword) { "Signing config keyPassword is required." }.toCharArray()
    check(storeFile.exists()) { "Keystore file not found at: ${storeFile.absolutePath}" }
    return KeyStore.getInstance(/* type = */ KeyStore.getDefaultType())
        .also { storeFile.inputStream().use { stream -> it.load(stream, storePassword) } }
        .getKey(keyAlias, keyPassword)
}
