/**
 * Share links: share one item, or some text, through a link that needs no account; list and revoke
 * links; open one.
 *
 * Guide: https://developer.openvireo.com/sdk/guides/share-links/
 *
 *     ./gradlew run --args="share-links list"
 *     ./gradlew run --args="share-links text 'The alarm code is 4321'"
 *     ./gradlew run --args="share-links open https://share.openvireo.com/<id>#<key>"
 */
package examples.sharelinks

import com.openvireo.core.*
import examples.makeClient
import examples.prompt
import examples.signIn
import examples.usage

// Making a link
suspend fun shareEntry(account: Account, vault: Vault, entryId: String): String {
    val entry = vault.entry(entryId)
    val choices = shareableFields(entry)   // what can be shared, in display order
    println("can share: ${choices.map { it.name }}")

    val payload = itemSharePayload(entry, defaultShareFields(), "Sam")   // username, password, website
    val link = account.createShare(payload, ShareOptions(
        expiresIn = 7u * 86400u,   // seconds: 1 hour to 30 days (free plans: 7 days)
        maxOpens = 1u,             // null: until it expires
        passcode = null,
        vaultId = vault.id(),
        entryId = entryId,
    ), CancelToken())
    return link.url.decodeToString()   // https://share.openvireo.com/<id>#<key>
}

suspend fun shareText(account: Account, text: String): String {
    val link = account.createShare(
        textSharePayload(text.toByteArray(), "From the examples", null),
        ShareOptions(expiresIn = 3600u, maxOpens = null, passcode = "tiger lily 42".toByteArray(), vaultId = null, entryId = null),
        CancelToken(),
    )
    return link.url.decodeToString()
}

// Listing and revoking
suspend fun listShares(account: Account) {
    for (s in account.listShares(CancelToken())) {
        // s.reference has vault, entry, title, readable only by this account
        println("${s.id} ${s.reference?.title} ${s.status} ${s.opens}/${s.maxOpens} ${s.expiresAt} ${s.endReason}")
    }
}

suspend fun revoke(account: Account, shareId: String) = account.revokeShare(shareId, CancelToken())

suspend fun orgPolicy(account: Account, orgId: String) =
    account.setOrgShareLinks(orgId, ShareLinkPolicy.MANAGERS, CancelToken())   // ON (default), MANAGERS or OFF

// Opening a link
suspend fun openLink(client: OvClient, api: String, link: String) {
    val info = client.shareInfo(api, parseShareLink(link).id, CancelToken())   // doesn't count as an open
    // Show a "Reveal" button and open only when it's pressed: link previews then can't use up a one-time link.
    prompt("Opens left: ${info.opensLeft}. Press Enter to reveal... ")
    val passcode = if (info.kdf != null) prompt("Passcode: ").toByteArray() else null
    val opened = client.openShare(api, link.toByteArray(), passcode, info, CancelToken())

    for (f in opened.payload.fields) {
        val shown = if (f.k == "totp") totpCode(parseTotpUri(f.value), System.currentTimeMillis()).code else f.value
        // render as plain text; hide f.protected values until shown
        println("${f.label}: $shown")
    }
}

suspend fun run(args: List<String>) {
    val client = makeClient()
    val account = signIn(client)
    try {
        when {
            args == listOf("list") -> listShares(account)
            args.size == 2 && args[0] == "text" -> println("${shareText(account, args[1])}\npasscode: tiger lily 42")
            args.size == 2 && args[0] == "open" -> openLink(client, account.serverConfig().api, args[1])   // or the share host's API
            else -> usage("Usage: share-links list | text <text> | open <link>")
        }
    } finally {
        account.signOut(false, CancelToken())
    }
}
