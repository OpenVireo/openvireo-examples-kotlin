/**
 * Vaults: listing and opening, reading and writing fields, editing entries, creating, sharing,
 * revoking and organisations.
 *
 * Guide: https://developer.openvireo.com/sdk/guides/vaults-and-sharing/
 *
 * Running it signs in, lists the vaults and the entries of the first one, and reads a field if you
 * pass an ov:// reference. Nothing is written unless you pass --write, which adds an "OpenVireo
 * example" entry to the first vault you can write to. The sharing and organisation functions are
 * here to read and copy; run() doesn't call them.
 *
 *     ./gradlew run --args="vaults-and-sharing [ov://Vault/Entry/field] [--write]"
 */
package examples.vaults

import com.openvireo.core.*
import examples.makeClient
import examples.prompt
import examples.signIn

// Listing and opening
suspend fun listAndOpen(account: Account): Vault? {
    val vaults = account.vaults(CancelToken())   // VaultInfo: id, name, role (READ, WRITE, MANAGE), orgId, ...
    for (info in vaults) println("${info.name}  ${info.role}  ${info.id}")
    if (vaults.isEmpty()) return null

    val vault = account.openVault(vaults[0].id, CancelToken())   // downloads and decrypts it
    println("Entries in ${vault.name()} at ${vault.etag()}")      // the server version this copy is based on
    for (e in vault.entries(false)) println("  ${e.title}")       // EntrySummary records; vault.entry(id) gives the full record
    return vault
}

// Reading and writing fields
suspend fun readAndWrite(vault: Vault, key: ByteArray) {
    println(vault.readField("ov://Team/GitHub/password").decodeToString())

    vault.writeField("ov://Team/GitHub/password", "new value".toByteArray(), false, CancelToken())   // edits and syncs
    vault.writeField("ov://Team/Apps/Stripe/api_key", key, true, CancelToken())   // creates the entry if needed
}

// Editing the database directly
suspend fun editEntries(vault: Vault) {
    vault.addEntry(vault.rootGroupId(), EntryInput(
        fields = listOf(
            FieldInput("Title", "OpenVireo example".toByteArray(), null),
            FieldInput("UserName", "app".toByteArray(), null),
            FieldInput("Password", "s3cret".toByteArray(), null),
        ),
        removeFields = emptyList(), tags = null, iconId = null, expiry = EntryExpiry.Keep,
    ))
    vault.setFields("ov://${vault.name()}/OpenVireo example", listOf(
        FieldInput("UserName", "svc".toByteArray(), null),
        FieldInput("token", "...".toByteArray(), true),
    ), CreateMode.CREATE_IF_MISSING)

    val result = vault.sync(SyncOptions(localChanged = true), CancelToken())
    println(result.outcome)     // UPLOADED, PULLED (only downloaded), CREATED or UNCHANGED
    println(result.conflicts)   // entries both sides changed
    println(result.changes)     // what the merge did, entry by entry
}

// Creating vaults
suspend fun createVaults(account: Account, orgId: String) {
    account.createVault("Personal", null, CancelToken())
    account.createVault("Team Infra", orgId, CancelToken())   // org owners and admins get manage automatically
}

// Sharing
suspend fun shareByEmail(account: Account, vaultId: String) {
    val grant = account.shareVault(vaultId, Grantee.Email("bob@example.com"), VaultRole.WRITE, CancelToken())
    confirmWithBob(grant.fingerprint)   // compare it with what Bob's app shows, out of band

    account.changeRole(vaultId, grant.principal, VaultRole.READ, CancelToken())   // changing the role
}

suspend fun shareWithVerifiedKeys(account: Account, vaultId: String) {
    val bob = account.lookUp("bob@example.com", CancelToken())
    check(verifyPublicKeys(bob.publicKeys)) { "Bob's keys don't verify" }
    confirmWithBob(bob.publicKeys.fingerprint)
    account.shareVaultWithKeys(vaultId, Grantee.Principal(bob.principal), VaultRole.WRITE, bob.publicKeys, CancelToken())
}

fun confirmWithBob(fingerprint: String) {
    check(prompt("Does Bob's app show $fingerprint? [y/N] ").lowercase() == "y") { "fingerprint not confirmed" }
}

// Revoking and rotating
suspend fun revokeAndRotate(account: Account, vaultId: String, bobPrincipal: String) {
    account.revoke(vaultId, listOf(bobPrincipal), CancelToken())   // removes access and rotates the vault key
    account.rotateVaultKey(vaultId, emptyList(), CancelToken())    // a new key without removing anyone
}

// Organisations
suspend fun organisations(account: Account, vaultId: String) {
    val org = account.createOrg("Acme", CancelToken())
    account.invite(org.id, "carol@example.com", OrgRole.MEMBER, CancelToken())
    // Carol gets an email, and her app calls account.acceptInvitation(invitationToken, CancelToken())

    val carol = account.orgMembers(org.id, CancelToken())
        .first { it.email == "carol@example.com" && it.status == MemberStatus.ACCEPTED }
    account.confirmMember(org.id, carol, listOf(VaultGrantRequest(vaultId, VaultRole.READ)), CancelToken())

    account.setMemberRole(org.id, carol, OrgRole.ADMIN, CancelToken())   // admins get manage on every org vault
    val report = account.removeMember(org.id, carol.userId, CancelToken())   // and rotates her vaults
    println("rotated: ${report.rotated}, failed: ${report.failed}")
}

suspend fun run(args: List<String>) {
    val account = signIn(makeClient())
    try {
        val vault = listAndOpen(account)
        val ref = args.firstOrNull { it.startsWith("ov://") }
        if (vault != null && ref != null) println("$ref = ${vault.readField(ref).decodeToString()}")
        if ("--write" in args) {
            account.vaults(CancelToken()).firstOrNull { it.role != VaultRole.READ }?.let {
                editEntries(account.openVault(it.id, CancelToken()))
            }
        }
    } finally {
        account.signOut(false, CancelToken())
    }
}
