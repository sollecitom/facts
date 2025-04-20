@file:Suppress("UnstableApiUsage")

rootProject.name = "playground"

includeBuild(".")
includeBuild("gradle-plugins")

fun resource(vararg pathSegments: String) = module(rootFolder = "resources", pathSegments = pathSegments)

fun library(vararg pathSegments: String) = module(rootFolder = "libs", pathSegments = pathSegments)

fun service(vararg pathSegments: String) = module(rootFolder = "services", pathSegments = pathSegments)

fun example(vararg pathSegments: String) = module(rootFolder = "example", pathSegments = pathSegments, excludeRootFolderFromGroupName = false)

fun exercise(vararg pathSegments: String) = module(rootFolder = "exercise", pathSegments = pathSegments, excludeRootFolderFromGroupName = false)

fun tool(vararg pathSegments: String) = module(rootFolder = "tools", pathSegments = pathSegments)

fun module(rootFolder: String, vararg pathSegments: String, excludeRootFolderFromGroupName: Boolean = true) {

    val projectName = pathSegments.last()
    val path = listOf(rootFolder) + pathSegments.dropLast(1)
    val group = if (excludeRootFolderFromGroupName) path.minus(rootFolder).joinToString(separator = "-") else path.joinToString(separator = "-")
    val directory = path.joinToString(separator = "/", prefix = "./")
    val fullProjectName = "${if (group.isEmpty()) "" else "$group-"}$projectName"

    include(fullProjectName)
    project(":$fullProjectName").projectDir = mkdir("$directory/$projectName")
}

fun subProject(name: String) {

    apply("$name/settings.gradle.kts")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")