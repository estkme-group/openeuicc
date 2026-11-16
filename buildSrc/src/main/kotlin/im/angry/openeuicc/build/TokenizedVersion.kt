package im.angry.openeuicc.build

import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import groovy.json.StringEscapeUtils
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
        val versionName = applicationExt.defaultConfig.versionName.orEmpty()
        val versionNameSuffix = applicationExt.defaultConfig.versionNameSuffix.orEmpty()
        val versionCode = checkNotNull(applicationExt.defaultConfig.versionCode) { "Version code is required." }
        applicationExt.buildTypes.all {
            val payload = buildPayload {
                writeUTF(name) // flavor name
                writeUTF(versionName + versionNameSuffix)
                writeLong(versionCode.toLong()) // version code
            }
            val message = buildMessage(signingConfig?.getKey(), payload)
            val token = Base64.getUrlEncoder().withoutPadding().encodeToString(message)
            buildConfigField("String", "TOKENIZED_VERSION", "\"$token\"")
        }
    }
}

private fun buildPayload(builderAction: DataOutputStream.() -> Unit) = ByteArrayOutputStream().use { out ->
    DataOutputStream(out).use(block = builderAction)
    out.toByteArray()
}

private fun buildMessage(key: Key?, payload: ByteArray): ByteArray {
    if (key == null) {
        return buildPayload {
            writeByte(0)
            writeBlock(payload)
        }
    }
    val signature = Signature.getInstance("SHA256with${key.algorithm}").let {
        it.initSign(checkNotNull(key as? PrivateKey) { "The key not is private key" })
        it.update(payload)
        it.sign()
    }
    return buildPayload {
        writeByte(1)
        writeBlock(payload)
        writeBlock(signature)
    }
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
