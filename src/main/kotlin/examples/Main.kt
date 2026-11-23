package examples

import examples.firstprogram.run as firstprogram
import examples.hostedaccount.run as hostedaccount
import examples.vaults.run as vaults
import examples.events.run as events
import examples.offline.run as offline
import examples.movetoaccount.run as movetoaccount
import examples.recovery.run as recovery
import examples.selfhosted.run as selfhosted
import examples.serviceaccounts.run as serviceaccounts
import examples.sharelinks.run as sharelinks
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/** The examples, by the name you pass to `./gradlew run --args="<name> ..."`. */
private val registry: Map<String, suspend (List<String>) -> Unit> = mapOf(
    "first-program" to ::firstprogram,
    "hosted-account" to ::hostedaccount,
    "vaults-and-sharing" to ::vaults,
    "events" to ::events,
    "offline" to ::offline,
    "move-to-account" to ::movetoaccount,
    "recovery" to ::recovery,
    "self-hosted-sync" to ::selfhosted,
    "service-accounts" to ::serviceaccounts,
    "share-links" to ::sharelinks,
    "check" to ::check,
)

/** Loads the native library and reaches the account server, without signing in. */
private suspend fun check(args: List<String>) {
    val account = makeClient().connect(issuer, com.openvireo.core.CancelToken())
    println("Connected to $issuer (API ${account.serverConfig().api}); signed in: ${account.isSignedIn()}")
}

fun main(args: Array<String>) {
    val run = registry[args.firstOrNull()] ?: run {
        System.err.println("Usage: ./gradlew run --args=\"<example> [args]\"\nExamples: ${registry.keys.joinToString()}")
        exitProcess(2)
    }
    runBlocking { run(args.drop(1)) }
}
