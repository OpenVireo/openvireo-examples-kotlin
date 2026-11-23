/**
 * Live events: react when another device changes a vault, or when access changes.
 *
 * Guide: https://developer.openvireo.com/sdk/guides/events/
 *
 * Running it signs in, opens the first vault and prints events until the session ends (Ctrl-C to
 * stop). Edit something in another OpenVireo app to see one.
 *
 *     ./gradlew run --args="events"
 */
package examples.events

import com.openvireo.core.*
import examples.makeClient
import examples.signIn

fun render(vaults: List<VaultInfo>) = println("Vaults now: ${vaults.joinToString { it.name }}")

suspend fun watch(account: Account, vault: Vault) {
    val stream = account.subscribe(CancelToken())
    try {
        while (true) {
            when (val event = stream.next(CancelToken()) ?: break) {
                is AccountEvent.VaultChanged -> {   // another device uploaded (or deleted) a vault this account can open
                    println("Vault changed: ${event.vaultId}")
                    if (event.vaultId == vault.id() && event.etag != vault.etag()) vault.sync(SyncOptions(localChanged = false), CancelToken())
                }
                is AccountEvent.VaultsChanged -> render(event.vaults)   // access changed
                is AccountEvent.SessionEnded -> break
                else -> {}
            }
        }
    } finally {
        stream.closeStream()   // a pending next() then returns null
    }
}

suspend fun run(args: List<String>) {
    val account = signIn(makeClient())
    try {
        val vaults = account.vaults(CancelToken())
        if (vaults.isEmpty()) return println("No vaults to watch.")
        println("Watching for changes (Ctrl-C to stop)...")
        watch(account, account.openVault(vaults[0].id, CancelToken()))
    } finally {
        account.signOut(false, CancelToken())
    }
}
