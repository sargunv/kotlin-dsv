# Contributing

## Development Environment

This project uses [mise](https://mise.jdx.dev/) for environment management.

1. Install mise: https://mise.jdx.dev/getting-started.html
2. Run `mise install` in the project root — this installs all required tools and sets up git hooks.

Alternatively, check `mise.toml` for the list of required tools and install them manually.

## IDE

Install the [dprint](https://plugins.jetbrains.com/plugin/18192-dprint) plugin for format-on-save
support.

## Formatting

A pre-commit hook (via [hk](https://hk.jdx.dev/)) auto-formats staged files before each commit.

- `mise run fix` — format all files
- `mise run check` — check formatting without modifying files

## Building and Running Tests

- `mise run build` — build and run all checks and tests
- `mise run test` — run all tests (JVM, JS Node, WASM JS Node, native)
- `mise run test:jvm` — run JVM tests only
