rootProject.name = "openvireo-examples-kotlin"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // The core's jar, straight from its GitHub release, so the examples build without a token.
        // In your own project use GitHub Packages (com.openvireo:openvireo-core-jvm): see the README.
        exclusiveContent {
            forRepository {
                ivy {
                    name = "openvireo-core-releases"
                    url = uri("https://github.com/openvireo/openvireo-core/releases/download")
                    patternLayout { artifact("v[revision]/[module]-[revision].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeGroup("com.openvireo") }
        }
    }
}
