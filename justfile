#!/usr/bin/env just --justfile

resetAll:
    git fetch origin && git reset --hard origin/main && git clean -f -d

push:
    git add . && git commit -m "WIP" && git push origin main

pull:
    git pull

build:
    ./gradlew build

rebuild:
    ./gradlew --refresh-dependencies --rerun-tasks clean build

updateDependencies:
    ./gradlew versionCatalogUpdate

updateGradle:
    ./gradlew wrapper --gradle-version latest --distribution-type all

updateAll:
    just updateDependencies && just updateGradle



@update-workspace:
    just update-gradle-plugins
    just update-acme-schema-catalogue
    just update-swissknife
    just update-pillar
    just update-tools
    just update-facts
    just update-modulith-example
    just update-element-service-example

@push-workspace:
    just push-gradle-plugins
    just push-acme-schema-catalogue
    just push-swissknife
    just push-pillar
    just push-tools
    just push-facts
    just push-modulith-example
    just push-element-service-example

[working-directory: 'gradle-plugins']
@update-gradle-plugins:
    pwd
    just updateAll
    just build
    just publish

[working-directory: 'acme-schema-catalogue']
@update-acme-schema-catalogue:
    pwd
    just updateAll
    just build
    just publish

[working-directory: 'swissknife']
@update-swissknife:
    pwd
    just updateAll
    just build
    just publish

[working-directory: 'pillar']
@update-pillar:
    pwd
    just updateAll
    just build
    just publish

[working-directory: 'tools']
@update-tools:
    pwd
    just updateAll
    just build

[working-directory: 'examples']
@update-examples:
    pwd
    just updateAll
    just build

[working-directory: 'facts']
@update-facts:
    pwd
    just updateAll
    just build

[working-directory: 'modulith-example']
@update-modulith-example:
    pwd
    just updateAll
    just build

[working-directory: 'element-service-example']
@update-element-service-example:
    pwd
    just updateAll
    just build

[working-directory: 'gradle-plugins']
@push-gradle-plugins:
    pwd
    just push

[working-directory: 'acme-schema-catalogue']
@push-acme-schema-catalogue:
    pwd
    just push

[working-directory: 'swissknife']
@push-swissknife:
    pwd
    just push

[working-directory: 'pillar']
@push-pillar:
    pwd
    just push

[working-directory: 'tools']
@push-tools:
    pwd
    just push

[working-directory: 'examples']
@push-examples:
    pwd
    just push

[working-directory: 'facts']
@push-facts:
    pwd
    just push

[working-directory: 'modulith-example']
@push-modulith-example:
    pwd
    just push

[working-directory: 'element-service-example']
@push-element-service-example:
    pwd
    just push
