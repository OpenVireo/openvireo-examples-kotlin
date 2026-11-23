package examples

import com.openvireo.core.*
import com.openvireo.core.hosts.defaultHost
import java.nio.file.Path
import kotlin.system.exitProcess

/** The account server: OV_ISSUER, or the hosted one. */
val issuer: String = System.getenv("OV_ISSUER") ?: "https://accounts.openvireo.com"

fun env(name: String): String = System.getenv(name)?.takeIf { it.isNotEmpty() }
    ?: run { System.err.println("Set $name to run this example."); exitProcess(1) }

fun usage(text: String): Nothing {
    System.err.println(text.trimIndent())
    exitProcess(2)
}

fun makeClient(name: String = "OpenVireo examples", offline: Boolean = false, cacheDir: Path? = null): OvClient =
    OvClient(
        ClientConfig(DeviceInfo(id = null, name = name, platform = Platform.CLI), offline = offline, server = null),
        defaultHost(cacheDir = cacheDir),
    )

fun askForCode(): String = prompt("Authenticator code: ")

fun prompt(text: String): String {
    print(text)
    return readlnOrNull()?.trim().orEmpty()
}

/** Connects and signs in from OV_EMAIL, OV_PASSWORD and OV_SECRET_KEY, asking for a TOTP code when needed. */
suspend fun signIn(client: OvClient): Account {
    val account = client.connect(issuer, CancelToken())
    var step = account.login(
        LoginInput(
            env("OV_EMAIL"), env("OV_PASSWORD").toByteArray(),
            secretKey = System.getenv("OV_SECRET_KEY")?.toByteArray(), totp = null, trustDevice = true,
        ),
        CancelToken(),
    )
    while (step is LoginStep.NeedsTotp) {
        step = account.submitTotp(step.challenge, askForCode(), CancelToken())
    }
    return account
}
