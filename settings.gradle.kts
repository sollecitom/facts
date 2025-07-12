@file:Suppress("UnstableApiUsage")

rootProject.name = "facts"

fun resource(vararg pathSegments: String) = subProject(rootFolder = "resources", pathSegments = pathSegments)

fun library(vararg pathSegments: String) = subProject(rootFolder = "libs", pathSegments = pathSegments)

fun service(vararg pathSegments: String) = subProject(rootFolder = "services", pathSegments = pathSegments)

fun example(vararg pathSegments: String) = subProject(rootFolder = "example", pathSegments = pathSegments, excludeRootFolderFromGroupName = false)

fun module(vararg pathSegments: String) = subProject(rootFolder = "modules", pathSegments = pathSegments, excludeRootFolderFromGroupName = false)

fun exercise(vararg pathSegments: String) = subProject(rootFolder = "exercises", pathSegments = pathSegments, excludeRootFolderFromGroupName = false)

fun subProject(rootFolder: String, vararg pathSegments: String, excludeRootFolderFromGroupName: Boolean = true) {

    val projectName = pathSegments.last()
    val path = listOf(rootFolder) + pathSegments.dropLast(1)
    val group = if (excludeRootFolderFromGroupName) path.minus(rootFolder).joinToString(separator = "-") else path.joinToString(separator = "-", prefix = "${rootProject.name}-")
    val directory = path.joinToString(separator = "/", prefix = "./")
    val fullProjectName = "${if (group.isEmpty()) "" else "$group-"}$projectName"

    include(fullProjectName)
    project(":$fullProjectName").projectDir = mkdir("$directory/$projectName")
}

fun includeProject(firstSegment: String, vararg otherSegments: String) {

    val segments = listOf(firstSegment) + otherSegments
    val projectName = segments.last()
    val path = segments.dropLast(1)
    val prefix = path.takeUnless(List<String>::isEmpty)?.joinToString(separator = "/", postfix = "/") ?: ""
    apply("$prefix$projectName/settings.gradle.kts")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

exercise("event-driven-tic-tac-toe")

module("client", "kotlin", "api")
module("client", "kotlin", "test-specification")
module("client", "kotlin", "in-memory", "implementation")
module("client", "kotlin", "in-memory", "tests")