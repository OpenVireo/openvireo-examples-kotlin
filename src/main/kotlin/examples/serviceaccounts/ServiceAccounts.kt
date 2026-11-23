/**
 * Service accounts: create one for CI, then read, inject and write secrets with its token.
 *
 * Guide: https://developer.openvireo.com/sdk/guides/service-accounts/
 *
 *     OPENVIREO_SERVICE_ACCOUNT_TOKEN=... ./gradlew run --args="service-accounts 'ov://Team Infra/CI/token'"
 *
 * create() needs an organisation owner or admin signed in, so run() doesn't call it. Set ROTATE to a
 * new value to also write it (needs the write role).
 */
package examples.serviceaccounts

import com.openvireo.core.*
import examples.env
import examples.makeClient
import examples.usage

// Creating one
suspend fun create(account: Account, orgId: String, vaultId: String) {
    val created = account.createServiceAccount(orgId, NewServiceAccount(
        name = "ci",
        vaults = listOf(VaultGrantRequest(vaultId, VaultRole.READ)),
        expiresAt = "2027-01-01T00:00:00Z",   // or null
        secretName = null,
    ), CancelToken())
    println(created.token.decodeToString())   // show it once; store it as a CI secret

    account.issueServiceAccountSecret(created.serviceAccount.id, "staging", null, CancelToken())
}

// Reading and writing
suspend fun run(args: List<String>) {
    val reference = args.singleOrNull() ?: usage("Usage: service-accounts <ov://Vault/Entry/field>")
    val client = makeClient("CI")
    val sa = ServiceAccount.fromToken(client, env("OPENVIREO_SERVICE_ACCOUNT_TOKEN").toByteArray(), CancelToken())
    try {
        val password = sa.read(reference, CancelToken())
        println("$reference: ${password.size} bytes")

        val injected = sa.inject("TOKEN=${reference.replace(" ", "%20")}\n".toByteArray(), CancelToken())   // mask injected.secrets
        println("injected ${injected.secrets.size} secret(s)")

        System.getenv("ROTATE")?.let { sa.write(reference, it.toByteArray(), false, CancelToken()) }   // needs the write role
    } finally {
        sa.close()   // wipes the keys from memory
    }
}
