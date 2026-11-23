/**
 * Moving a .kdbx file into an account: as a new vault, or merged into a vault you already have.
 *
 * Guide: https://developer.openvireo.com/sdk/guides/move-to-account/
 *
 *     ./gradlew run --args="move-to-account Passwords.kdbx"            # a new vault named after the file
 *     ./gradlew run --args="move-to-account Passwords.kdbx Personal"   # merge into the vault "Personal"
 *
 * The file's password is read from KDBX_PASSWORD.
 */
package examples.movetoaccount

import com.openvireo.core.*
import examples.env
import examples.makeClient
import examples.signIn
import examples.usage
import java.nio.file.Files
import java.nio.file.Path

suspend fun importAsNewVault(account: Account, fileBytes: ByteArray, password: ByteArray, name: String) {
    val creds = KdbxCredentials(password = password, keyFile = null)
    val outcome = account.importKdbx(fileBytes, creds, ImportMode.NewVault(name = name, orgId = null), CancelToken())
    if (!outcome.uploaded) {
        // the vault exists but its first upload failed (outcome.uploadError): retry it later
        outcome.vault.sync(SyncOptions(localChanged = true), CancelToken())
    }
    println("Imported into ${outcome.vault.name()}")
}

suspend fun mergeInto(client: OvClient, vault: Vault, fileBytes: ByteArray, password: ByteArray) {
    val file = client.openLocal(fileBytes, KdbxCredentials(password = password, keyFile = null), CancelToken())
    val summary = vault.importFrom(file, LocalImportMode.Merge)   // added, updated, unchanged
    // or: LocalImportMode.Copies(groupId), with vault.importTargets() listing the groups to offer
    vault.sync(SyncOptions(localChanged = true), CancelToken())
    println(summary)
}

suspend fun run(args: List<String>) {
    if (args.size !in 1..2) usage("Usage: move-to-account <file.kdbx> [vault name]")
    val path = Path.of(args[0])
    val fileBytes = Files.readAllBytes(path)
    val password = env("KDBX_PASSWORD").toByteArray()
    val client = makeClient()
    val account = signIn(client)
    try {
        val into = args.getOrNull(1)
        if (into == null) {
            importAsNewVault(account, fileBytes, password, path.fileName.toString().removeSuffix(".kdbx"))
        } else {
            val info = account.vaults(CancelToken()).first { it.name == into }
            mergeInto(client, account.openVault(info.id, CancelToken()), fileBytes, password)
        }
    } finally {
        account.signOut(false, CancelToken())
    }
}
