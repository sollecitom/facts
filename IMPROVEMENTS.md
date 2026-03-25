# facts

## Overview
Ambitious exploration of a facts-based backend framework. Central premise: building effective backend applications using immutable "facts" as fundamental data units instead of traditional events. Early-stage proof of concept.

## Scorecard

| Dimension | Rating | Notes |
|-----------|--------|-------|
| Build system | A | Gradle 9.4.0, consistent conventions |
| Code quality | N/A | Almost no implementation code yet |
| Test coverage | D | 3 test files, 0 production files |
| Documentation | A- | Exceptional README with vision, goals, and rationale |
| Dependency freshness | A | All current |
| Modularity | B+ | 4 modules, well-planned folder structure |
| Maintainability | B | Small but pre-alpha |

## Structure
- 4 modules: `client/kotlin/api`, `client/kotlin/test-specification`, `client/kotlin/in-memory/implementation`, `client/kotlin/in-memory/tests`
- 1 exercise: `event-driven-tic-tac-toe`
- Planned folders: `libs/`, `services/`, `tools/`, `resources/`, `modules/`, `exercises/`

## Issues
- Pre-alpha: very little actual code despite ambitious 10-goal vision
- Scope creep risk: claims multi-language support (JVM, Python, C++, JS, Go, Rust) but only Kotlin exists
- No phased roadmap — unclear what comes first
- No working implementation of the core facts abstraction

## Potential Improvements
1. Reduce scope — implement one killer feature (facts storage/retrieval) before expanding
2. Create a phased roadmap (MVP → Phase 1 → Phase 2)
3. Implement the core facts-processor abstraction with actual code
4. Complete the tic-tac-toe exercise as a working tutorial
5. Start single-language (Kotlin) — multi-language support is premature
6. Extract successful patterns to swissknife when mature
