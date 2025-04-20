#!/usr/bin/env just --justfile

build:
    ./gradlew build
#    ./gradlew build jibDockerBuild containerBasedServiceTest

rebuild:
    ./gradlew --refresh-dependencies --rerun-tasks build
#    ./gradlew --refresh-dependencies --rerun-tasks build jibDockerBuild containerBasedServiceTest

updateDependencies:
    ./gradlew versionCatalogUpdate

updateGradle:
    ./gradlew wrapper --gradle-version latest --distribution-type all

updateAll:
    just updateDependencies && just updateGradle

publishLibraries:
    ./gradlew build publishToMavenLocal