/**
 * Offline: edit from the cached copy while the servers can't be reached, and merge the edits in when
 * back online.
 *
 * Guide: https://developer.openvireo.com/sdk/guides/offline/
 *
 * Offline needs a persistent cache (ov-cache/ here). To unlock offline after a restart it also needs a
 * SecureStore that outlives the process; the default host's store is in memory, so this program signs
 * in (online) and then edits, keeping the edit if the network drops.
 *
 *     ./gradlew run --args="offline 'ov://Home/Wifi/password' 'new value'"
 */
package examples.offline

import com.openvireo.core.*
import com.openvireo.core.hosts.defaultHost
import examples.issuer
import examples.signIn
import examples.usage
import java.nio.file.Path

suspend fun run(args: List<String>) {
    if (args.size != 2) usage("Usage: offline <ov://Vault/Entry/field> <value>")
    val (reference, value) = args
    val client = OvClient(
        ClientConfig(DeviceInfo(id = null, name = "Laptop", platform = Platform.CLI), offline = true, server = null),
        defaultHost(cacheDir = Path.of("ov-cache")),   // offline needs a persistent cache
    )
    val account = signIn(client)   // with a persistent store: client.connect(issuer) then account.unlock(password, null)
    val vaultName = reference.removePrefix("ov://").substringBefore('/')
    val info = account.vaults(CancelToken()).first { it.name == vaultName }
    val vault = account.openVault(info.id, CancelToken())   // offline: the local copy
    println("offline: ${account.isOffline()} (server: $issuer)")   // true while the servers can't be reached

    try {
        vault.writeField(reference, value.toByteArray(), false, CancelToken())
    } catch (e: OvException) {
        if (e.code != OvErrorCode.NETWORK_ERROR) throw e   // offline: the edit is kept and merges in later
        println("Saved locally; it uploads when you're back online.")
    }

    if (account.isOffline()) {
        val result = account.reconnect(CancelToken())   // back online; result.dropped: revoked vaults
        println("reconnected; dropped: ${result.dropped}")
        vault.sync(SyncOptions(localChanged = true), CancelToken())   // uploads the kept edit
    }
    account.signOut(false, CancelToken())
}
