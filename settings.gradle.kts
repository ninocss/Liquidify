pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral(); maven("https://maven.aliucord.com/releases") } }
rootProject.name = "SquareGlassTidal"
include(":extension")
include(":host-stubs")
