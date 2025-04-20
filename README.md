# Kotlin Monorepo and Monolith Example

An example showcasing a monorepo and a monolith in Kotlin. 

## Components

- [Docs](./docs/README.md)
- [Libraries](./libs/README.md)
- [Services](./services/README.md)
- [Tools](./tools/README.md)
- [Resources](./resources/README.md)
- [Examples](./example/README.md)
- [Exercises](./exercise/README.md)

## Requirements

1. Java 23 (neither below nor above).

## How to

### Build the project (incrementally)

```bash
just build

```

### Rebuild the project (without caches)

```bash
just rebuild

```

### Upgrade the Gradle wrapper to the latest available version

```bash
just updateGradle

```

### Update all dependencies if more recent versions exist, and remove unused ones (it will update `gradle/libs.versions.toml`)

```bash
just updateDependencies

```

### Publish the libraries to the local Maven repository

```bash
just publishLibraries

```

## Outstanding quirks and gotchas

- Check [the list of quirks and gotchas](docs/QUIRKS.md) to learn about things you should watch out for.