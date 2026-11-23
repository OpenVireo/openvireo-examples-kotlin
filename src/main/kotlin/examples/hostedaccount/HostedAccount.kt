/**
 * Hosted accounts: connecting, signing up and in, locking, the account itself, devices and deletion.
 *
 * Guide: https://developer.openvireo.com/sdk/guides/hosted-account/
 *
 * Running it signs in, prints the account and its devices, locks and unlocks, and signs out. The
 * functions that change the account (signUp, changePassword, deleteAccount, ...) are here to read and
 * copy; run() doesn't call them.
 *
 *     ./gradlew run --args="hosted-account"
 */
package examples.hostedaccount

import com.openvireo.core.*
import com.openvireo.core.hosts.defaultHost
import examples.askForCode
import examples.env
import examples.issuer
import examples.prompt
import java.nio.file.Path

// Connecting
suspend fun connect(): Account {
    val client = OvClient(
        ClientConfig(DeviceInfo(id = null, name = "Backup tool", platform = Platform.CLI), offline = false, server = null),
        defaultHost(cacheDir = Path.of("ov-cache")),   // pass store = for a SecureStore that outlives the process
    )
    return client.connect(issuer, CancelToken())
}

// Signing up
suspend fun signUp(account: Account, email: String, password: ByteArray) {
    val created = account.register(RegisterInput(email, password, kdf = null, trustDevice = true), CancelToken())
    showEmergencyKit(created.secretKey)   // the only time the Secret Key is shown; the user must keep it
}

fun showEmergencyKit(secretKey: ByteArray) = println("Your Secret Key (keep it safe): ${secretKey.decodeToString()}")

// Signing in
suspend fun signIn(account: Account, email: String, password: ByteArray, secretKey: ByteArray?) {
    // secretKey: only needed the first time on this device (else null)
    var step = account.login(LoginInput(email, password, secretKey = secretKey, totp = null, trustDevice = true), CancelToken())
    while (step is LoginStep.NeedsTotp) {   // two-factor authentication is on
        step = account.submitTotp(step.challenge, askForCode(), CancelToken())
    }
}

// Unlocking and locking
suspend fun lockAndUnlock(account: Account, password: ByteArray) {
    account.lock()   // wipes the keys from memory; the session stays
    if (account.isSignedIn() && !account.isUnlocked()) account.unlock(password, null, CancelToken())
}

suspend fun signOut(account: Account, forgetSecretKey: Boolean) {
    // false ends the session on the server and forgets it here; true also forgets the trusted Secret Key
    account.signOut(forgetSecretKey, CancelToken())
}

// Staying unlocked across a restart
suspend fun stayUnlocked(account: Account, reloaded: Account) {
    val secret = account.unlockedKeysetSecret()   // keep it in the platform's secure store
    // after a restart, on a connected (signed-in, locked) account
    reloaded.resumeUnlocked(secret, CancelToken())
}

suspend fun watchForSignOut(account: Account) {
    val reason = account.signedOut(CancelToken())   // suspends: SESSION_EXPIRED, TOKEN_REUSED, UNAUTHORIZED, LOGOUT, ...
    println("Signed out: $reason")
}

// The account itself
fun showAccount(account: Account) {
    println("email: ${account.email()}")
    println("user id: ${account.userId()}")
    println("fingerprint: ${account.fingerprint()}")   // this account's public key fingerprint, for others to compare
}

suspend fun changePassword(account: Account, current: ByteArray, next: ByteArray) {
    account.changePassword(current, next, null, CancelToken())   // signs the other devices out
}

suspend fun changeEmail(account: Account, newEmail: String, password: ByteArray) {
    account.requestEmailChange(newEmail, CancelToken())   // mails a code to the new address
    val code = prompt("Code sent to $newEmail: ")
    account.changeEmail(newEmail, code, password, CancelToken())
}

suspend fun accountWebsiteLink(account: Account): String =
    account.webSessionUrl("/settings", CancelToken()).decodeToString()   // one-time link (60 s)

// Devices
suspend fun listDevices(account: Account) {
    for (d in account.devices(CancelToken())) {   // name, platform, lastSeenAt, current, ...
        println("${d.name} ${d.platform} ${d.lastSeenAt}${if (d.current) " (this device)" else ""}")
    }
}

suspend fun revokeDevice(account: Account, name: String) {
    val devices = account.devices(CancelToken())
    devices.find { !it.current && it.name == name }?.let { account.revokeDevice(it.id, CancelToken()) }
}

// Deleting the account
suspend fun deleteAccount(account: Account, password: ByteArray, typedEmail: String, heir: String?) {
    val preview = account.deletionPreview(CancelToken())
    println("would be erased on ${preview.eraseOn}")
    println("vaults: ${preview.vaults.map { it.name to it.sharedWith }}")   // shared vaults can be given away first
    println("orgs: ${preview.orgs.map { it.name to it.outcome }}")         // leave, delete or blocked
    if (heir != null) {
        preview.vaults.firstOrNull()?.takeIf { it.sharedWith.isNotEmpty() }?.let {
            account.transferVault(it.id, heir, CancelToken())
        }
    }

    val scheduledFor = account.deleteAccount(password, typedEmail, CancelToken())
    println("scheduled for $scheduledFor")
    // This device is signed out and its offline cache is gone. The Secret Key stays, so after signing in again:
    if (account.deletionScheduledFor() != null) account.restoreAccount(password, CancelToken())
}

suspend fun run(args: List<String>) {
    val password = env("OV_PASSWORD").toByteArray()
    val account = connect()
    signIn(account, env("OV_EMAIL"), password, env("OV_SECRET_KEY").toByteArray())
    showAccount(account)
    listDevices(account)
    lockAndUnlock(account, password)
    signOut(account, false)
}
