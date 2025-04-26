@file:Suppress("UnstableApiUsage")

includeBuild(".")
includeBuild("gradle-plugins")
includeProject("swissknife")
includeProject("acme-schema-catalogue")
includeProject("pillar")

rootProject.name = "playground"

fun resource(vararg pathSegments: String) = subProject(rootFolder = "resources", pathSegments = pathSegments)

fun library(vararg pathSegments: String) = subProject(rootFolder = "libs", pathSegments = pathSegments)

fun service(vararg pathSegments: String) = subProject(rootFolder = "services", pathSegments = pathSegments)

fun example(vararg pathSegments: String) = subProject(rootFolder = "example", pathSegments = pathSegments, excludeRootFolderFromGroupName = false)

fun exercise(vararg pathSegments: String) = subProject(rootFolder = "exercise", pathSegments = pathSegments, excludeRootFolderFromGroupName = false)

fun tool(vararg pathSegments: String) = subProject(rootFolder = "tools", pathSegments = pathSegments)

fun subProject(rootFolder: String, vararg pathSegments: String, excludeRootFolderFromGroupName: Boolean = true) {

    val projectName = pathSegments.last()
    val path = listOf(rootFolder) + pathSegments.dropLast(1)
    val group = if (excludeRootFolderFromGroupName) path.minus(rootFolder).joinToString(separator = "-") else path.joinToString(separator = "-")
    val directory = path.joinToString(separator = "/", prefix = "./")
    val fullProjectName = "${if (group.isEmpty()) "" else "$group-"}$projectName"

    include(fullProjectName)
    project(":$fullProjectName").projectDir = mkdir("$directory/$projectName")
}

fun includeProject(name: String) {

    apply("$name/settings.gradle.kts")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

example("libraries")