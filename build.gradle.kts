import sollecitom.plugins.conventions.task.dependency.version.MinimumDependencyVersion
import sollecitom.plugins.conventions.task.dependency.version.MinimumDependencyVersionConventions
import sollecitom.plugins.RepositoryConfiguration
import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure
import sollecitom.plugins.Plugins
import java.nio.file.Path
import java.nio.file.Paths

buildscript {
    repositories {
        mavenLocal()
    }

    dependencies {
        classpath(libs.sollecitom.gradle.plugins.base)
        classpath(libs.sollecitom.gradle.plugins.kotlin.jvm)
    }
}

plugins {
    `java-library`
    idea
    alias(libs.plugins.com.palantir.git.version)
}

repositories {
    RepositoryConfiguration.BuildScript.apply(this)
}

apply(plugin = "sollecitom.dependency-update-conventions")
apply(plugin = "sollecitom.aggregate-test-metrics-conventions")

val parentProject = this
val projectGroup: String by project
val currentVersion: String by project
val versionDetails: Closure<VersionDetails> by extra
val gitVersion = versionDetails()
val libsFolder: Path = rootProject.projectDir.path.let { Paths.get(it) }.resolve("libs")
val servicesFolder: Path = rootProject.projectDir.path.let { Paths.get(it) }.resolve("services")
val toolsFolder: Path = rootProject.projectDir.path.let { Paths.get(it) }.resolve("tools")
val resourceFolder: Path = rootProject.projectDir.path.let { Paths.get(it) }.resolve("resources")

fun Project.isLibrary() = projectDir.path.let { Paths.get(it) }.startsWith(libsFolder)

allprojects {

    project.extra["gitVersion"] = gitVersion

    group = projectGroup
    version = currentVersion

    repositories { RepositoryConfiguration.Modules.apply(this, project) }

    apply<IdeaPlugin>()
    idea { module { inheritOutputDirs = true } }

    apply(plugin = "sollecitom.kotlin-conventions")
    apply(plugin = "sollecitom.test-conventions")

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    if (isLibrary()) {
        apply(plugin = "sollecitom.maven-publish-conventions")
    }

    java(Plugins.JavaPlugin::configure)

    apply(plugin = "sollecitom.minimum-dependency-version-conventions")
    configure<MinimumDependencyVersionConventions.Extension> {
        val apacheCommonsCompress = MinimumDependencyVersion(group = "org.apache.commons", name = "commons-compress", minimumVersion = "1.26.0")
        knownVulnerableDependencies.set(
            listOf(
                apacheCommonsCompress
            )
        )
    }

    subprojects {
        apply {
            plugin("org.jetbrains.kotlin.jvm")
            plugin<JavaLibraryPlugin>()
        }
    }
}

val containerBasedServiceTest: Task = tasks.register("containerBasedServiceTest").get()