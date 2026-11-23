/**
 * A self-hosted sync server: pairing a device, syncing a .kdbx file, and the other calls.
 *
 * Guide: https://developer.openvireo.com/sdk/guides/self-hosted-sync/
 *
 *     ./gradlew run --args="self-hosted-sync https://sync.example.org ABCD-EFGH personal Passwords.kdbx"
 *
 * Pairs with the server, syncs the file (password from KDBX_PASSWORD; the merged file is written back
 * to the same path) and shows the other calls. The default host keeps the pairing in memory; give it a
 * persistent SecureStore and restoreSelfHosted() finds it again next run.
 */
package examples.selfhosted

import com.openvireo.core.*
import examples.env
import examples.makeClient
import examples.usage
import java.nio.file.Files
import java.nio.file.Path

// Pairing
suspend fun pair(client: OvClient, url: String, code: String): SelfHosted =
    SelfHosted.pair(client, url, code, "Laptop", CancelToken())

suspend fun restore(client: OvClient, url: String): SelfHosted? =
    restoreSelfHosted(client, url)   // next time, with the same persistent store; null when not paired

// Syncing a database
suspend fun syncFile(server: SelfHosted, databaseId: String, path: Path, password: ByteArray) {
    val creds = KdbxCredentials(password = password, keyFile = null)
    val result = server.syncDatabase(databaseId, Files.readAllBytes(path), creds, CancelToken())
    // result.kdbx is the merged file: save it locally and reopen it (result.report says what changed)
    Files.write(path, result.kdbx)
    println(result.report)
}

// Other calls
suspend fun otherCalls(server: SelfHosted) {
    println(server.health(CancelToken()))   // '{"status":"ok"}'; not_sync_server for another site
    for (db in server.listDatabases(CancelToken())) println("${db.id} ${db.revision} ${db.size} ${db.updatedAt}")
    val first = server.download("personal", CancelToken())   // the .kdbx as uploaded, or null
    println("personal: ${first?.size} bytes")
    val code = server.createPairingCode(CancelToken()).code   // for the next device
    println("pairing code for the next device: $code")
    val stream = server.events(CancelToken())   // next() gives AccountEvent.DatabaseChanged
    stream.closeStream()
}

suspend fun run(args: List<String>) {
    if (args.size != 4) usage("Usage: self-hosted-sync <server url> <pairing code> <database id> <file.kdbx>")
    val (url, code, databaseId, file) = args
    val client = makeClient("Laptop")
    val server = restore(client, url) ?: pair(client, url, code)
    syncFile(server, databaseId, Path.of(file), env("KDBX_PASSWORD").toByteArray())
    otherCalls(server)
}
