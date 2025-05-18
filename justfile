#!/usr/bin/env just --justfile

initSubmodule submodule:
    git submodule update --init --recursive {{submodule}}

resetAll:
    git fetch origin && git reset --hard origin/main && git clean -f -d

push:
    git add . && git commit -m "WIP" && git push --recurse-submodules=on-demand origin main

pull:
    git fetch origin && git pull && git submodule update --recursive --remote

build:
    ./gradlew build jibDockerBuild containerBasedServiceTest

rebuild:
    ./gradlew --refresh-dependencies --rerun-tasks build jibDockerBuild containerBasedServiceTest

updateDependencies:
    ./gradlew versionCatalogUpdate

updateGradle:
    ./gradlew wrapper --gradle-version latest --distribution-type all

updateAll:
    just updateDependencies && just updateGradle

publishLibraries:
    ./gradlew build publishToMavenLocal

updateWorkspace:
    cd gradle-plugins
    just updateAll
    just build
    cd ..
    cd acme-schema-catalogue
    just updateAll
    just build
    cd ..
    cd swissknife
    just updateAll
    just build
    cd ..
    cd pillar
    just updateAll
    just build
    cd ..
    cd tools
    just updateAll
    just build
    cd ..
    cd examples
    just updateAll
    just build
    cd ..
    cd services/modulith-example
    just updateAll
    just build
    cd ..
    cd ..
    just updateAll
    just build