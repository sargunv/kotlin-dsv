# Kotlin DSV

A Kotlin Multiplatform library for working with delimiter-separated values (CSV, TSV, and custom
formats). It is published to Maven Central as `dev.sargunv.kotlin-dsv:kotlin-dsv`.

## Project map

- `kotlin-dsv` — the published library
- `benchmark` — kotlinx-benchmark suite (JVM, JS, Linux, macOS, Windows)
- `docs` — MkDocs documentation site
- `kotlin-dsv/src/fsTest` — filesystem-backed tests that use Git submodule fixtures

## Pull requests

When you open a pull request, adhere to the
[PULL_REQUEST_TEMPLATE.md](./.github/PULL_REQUEST_TEMPLATE.md) and open it in draft mode. The user
is responsible for additional details and marking ready for review.

## Dev tool commands

Tooling is managed by [mise](https://mise.jdx.dev). Install mise, then run `mise install` to install
the pinned tools (Java, dprint, hk, pkl, ktfmt, Python, actionlint, shellcheck) and set up the git
hooks. Clone with `--recurse-submodules` (or run `git submodule update --init --recursive`) so the
filesystem test fixtures are present.

```bash
# Install/refresh all tools and git hooks
mise install

# List available tasks
mise tasks --all

# Compile all platforms and run all checks (Detekt, ABI, formatting)
mise run build

# Run all tests (JVM, JS, WASM, native)
mise run test

# Test specific platforms
mise run test:jvm
mise run test:jsnode
mise run test:wasmjsnode
mise run test:native

# Lint and format checks / auto-fix
mise run check
mise run fix

# Run a single test
mise exec -- ./gradlew :kotlin-dsv:jvmTest --tests "*SomeTest*"

# Serve the documentation site locally
mise run docs
```

Formatters and linters run automatically on pre-commit via [hk](https://hk.jdx.dev); you usually
don't need to run them manually. The environment is managed by mise, so run any tool that isn't
already a mise task with `mise exec -- <command>`.
