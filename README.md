# OpenVireo examples: Kotlin

Runnable examples for the [OpenVireo](https://openvireo.com) Kotlin binding (`com.openvireo.core`), one
file per guide on [developer.openvireo.com](https://developer.openvireo.com). Each file holds the code from
its guide's **Kotlin** tab, wrapped in a program you can run.

Examples in other languages: [TypeScript](https://github.com/openvireo/openvireo-examples-typescript) · [Rust](https://github.com/openvireo/openvireo-examples-rust) · [Python](https://github.com/openvireo/openvireo-examples-python) · [Java](https://github.com/openvireo/openvireo-examples-java) · [Swift](https://github.com/openvireo/openvireo-examples-swift) · [C++](https://github.com/openvireo/openvireo-examples-cpp) · [ov CLI](https://github.com/openvireo/openvireo-examples-cli)

## Run them

You need JDK 21. The Gradle wrapper fetches everything else. The build takes the core's jar
(`openvireo-core-jvm` 1.2.2, natives for macOS, Linux and Windows inside) from Maven Central, so it needs
no token.

```sh
git clone https://github.com/openvireo/openvireo-examples-kotlin
cd openvireo-examples-kotlin
./gradlew run --args="check"
```

Set your account in the environment. `OV_SECRET_KEY` is only needed the first time on a device;
`OV_ISSUER` picks an account server other than `https://accounts.openvireo.com`.

```sh
export OV_EMAIL=you@example.com OV_PASSWORD='...' OV_SECRET_KEY='A3-...'
```

```sh
./gradlew -q run --args="first-program ov://Personal/GitHub/username"
./gradlew -q run --args="vaults-and-sharing"
```

In your own project, depend on `com.openvireo:openvireo-core-jvm` from Maven Central (or
`openvireo-core-android` for Android): see [Kotlin and Java](https://developer.openvireo.com/bindings/kotlin-java/).

## The examples

| Example | What it does | Guide |
|---|---|---|
| [`first-program`](src/main/kotlin/examples/firstprogram/FirstProgram.kt) | Sign in, list the vaults, read one field, sign out | [A first program](https://developer.openvireo.com/sdk/#a-first-program) |
| [`hosted-account`](src/main/kotlin/examples/hostedaccount/HostedAccount.kt) | Connect, sign up, sign in with two-step codes, lock, devices, account deletion | [Hosted accounts](https://developer.openvireo.com/sdk/guides/hosted-account/) |
| [`vaults-and-sharing`](src/main/kotlin/examples/vaults/VaultsAndSharing.kt) | List, open, read, write and edit vaults; share, revoke, organisations | [Vaults and sharing](https://developer.openvireo.com/sdk/guides/vaults-and-sharing/) |
| [`events`](src/main/kotlin/examples/events/Events.kt) | Follow live changes from other devices | [Events](https://developer.openvireo.com/sdk/guides/events/) |
| [`offline`](src/main/kotlin/examples/offline/Offline.kt) | Edit offline, merge when back online | [Offline](https://developer.openvireo.com/sdk/guides/offline/) |
| [`move-to-account`](src/main/kotlin/examples/movetoaccount/MoveToAccount.kt) | Import a `.kdbx` file as a new vault or merge it into one | [Moving to an account](https://developer.openvireo.com/sdk/guides/move-to-account/) |
| [`recovery`](src/main/kotlin/examples/recovery/Recovery.kt) | Inspect a recovery link, reset, restore the old vaults | [Recovery](https://developer.openvireo.com/sdk/guides/recovery/) |
| [`self-hosted-sync`](src/main/kotlin/examples/selfhosted/SelfHostedSync.kt) | Pair with a self-hosted sync server and sync a `.kdbx` file | [Self-hosted sync](https://developer.openvireo.com/sdk/guides/self-hosted-sync/) |
| [`service-accounts`](src/main/kotlin/examples/serviceaccounts/ServiceAccounts.kt) | Create a service account; read, inject and write with its token | [Service accounts](https://developer.openvireo.com/sdk/guides/service-accounts/) |
| [`share-links`](src/main/kotlin/examples/sharelinks/ShareLinks.kt) | Share an item or text by link, list and revoke links, open one | [Share links](https://developer.openvireo.com/sdk/guides/share-links/) |

Each file has one function per section of its guide. Running an example does something safe and
mostly read-only; the functions that change an account for good (deleting it, revoking access,
rotating keys) are there to read and copy, and the programs don't call them. Try the rest on a test
account. `check` reaches the account server without signing in, to test the setup.

## Checks

CI compiles the examples and runs `check` on Linux and macOS, on every push and weekly:

```sh
./gradlew build
```

## License

MIT. Copy the code into your own programs.
