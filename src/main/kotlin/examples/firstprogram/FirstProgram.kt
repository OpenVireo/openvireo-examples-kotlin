/**
 * A first program: sign in, list the vaults, read one field and sign out.
 *
 * Guide: https://developer.openvireo.com/sdk/#a-first-program
 *
 *     ./gradlew run --args="first-program ov://Personal/GitHub/username"
 */
package examples.firstprogram

import com.openvireo.core.*
import com.openvireo.core.hosts.defaultHost
import examples.env
import examples.issuer

suspend fun firstProgram(email: String, password: ByteArray, secretKey: ByteArray, reference: String?) {
    val client = OvClient(
        ClientConfig(DeviceInfo(id = null, name = "My script", platform = Platform.CLI), offline = false, server = null),
        defaultHost(),
    )
    val account = client.connect(issuer, CancelToken())
    // Returns LoginStep.NeedsTotp when two-step sign-in is on: see hosted-account
    account.login(LoginInput(email, password, secretKey = secretKey, totp = null, trustDevice = true), CancelToken())

    val vaults = account.vaults(CancelToken())
    for (info in vaults) println("${info.name} ${info.role}")
    if (vaults.isNotEmpty() && reference != null) {
        val vault = account.openVault(vaults.first().id, CancelToken())
        println(vault.readField(reference).decodeToString())
    }
    account.signOut(false, CancelToken())
}

suspend fun run(args: List<String>) =
    firstProgram(env("OV_EMAIL"), env("OV_PASSWORD").toByteArray(), env("OV_SECRET_KEY").toByteArray(), args.firstOrNull())
