/**
 * Account recovery: resetting with a recovery link, and restoring the old vaults afterwards.
 *
 * Guide: https://developer.openvireo.com/sdk/guides/recovery/
 *
 *     ./gradlew run --args="recovery info <token>"   # what a reset would do; works signed out
 *     ./gradlew run --args="recovery status"         # after a reset: is there an archive to restore?
 *
 * reset() and restore() change the account, so they only run when you call them yourself.
 */
package examples.recovery

import com.openvireo.core.*
import examples.issuer
import examples.makeClient
import examples.signIn
import examples.usage

// Resetting
suspend fun info(account: Account, token: ByteArray) {
    val info = account.recoverInfo(token, CancelToken())   // works signed out
    println(info)   // email, notBefore, expiresAt, archiveDays, personalVaults, sharedGrants, soleAdminOrgs
}

suspend fun reset(account: Account, token: ByteArray, newPassword: ByteArray) {
    val created = account.recover(token, newPassword, null, true, CancelToken())   // signed in and unlocked afterwards
    println("Your new Secret Key (keep it safe): ${created.secretKey.decodeToString()}")
}

// Restoring the old vaults
suspend fun restore(account: Account, oldPassword: ByteArray, oldSecretKey: ByteArray) {
    val status = account.recoveryStatus(CancelToken())
    if (status.archived && !status.locked) {
        account.restoreFromArchive(oldPassword, oldSecretKey, CancelToken())   // vault list reloaded
    }
}

suspend fun run(args: List<String>) {
    val client = makeClient()
    when {
        args.size == 2 && args[0] == "info" -> info(client.connect(issuer, CancelToken()), args[1].toByteArray())
        args == listOf("status") -> {
            val account = signIn(client)
            println(account.recoveryStatus(CancelToken()))
            account.signOut(false, CancelToken())
        }
        else -> usage("Usage: recovery info <token> | recovery status")
    }
}
