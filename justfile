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
    ./gradlew build

rebuild:
    ./gradlew --refresh-dependencies --rerun-tasks build

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
    git checkout main
    git pull origin/main
    just updateAll
    just build
    cd ..
    cd acme-schema-catalogue
    git checkout main
    git pull origin/main
    just updateAll
    just build
    cd ..
    cd swissknife
    git checkout main
    git pull origin/main
    just updateAll
    just build
    cd ..
    cd pillar
    git checkout main
    git pull origin/main
    just updateAll
    just build
    cd ..
    cd tools
    git checkout main
    git pull origin/main
    just updateAll
    just build
    cd ..
    cd examples
    git checkout main
    git pull origin/main
    just updateAll
    just build
    cd ..
    cd services/modulith-example
    git checkout main
    git pull origin/main
    just updateAll
    just build
    cd ..
    cd ..
    just updateAll
    just build