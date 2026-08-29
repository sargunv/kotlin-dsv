## Dev Setup

This project uses [mise](https://mise.jdx.dev/) to manage tools and tasks. Install mise, then from
the repo root install all required tools:

```sh
mise install
```

This installs the pinned versions of Java, dprint, hk, pkl, ktfmt, Python, actionlint, and
shellcheck, and runs `hk install --mise` to set up the pre-commit hook.

Clone with `--recurse-submodules`, or initialize them after the fact:

```sh
git submodule update --init --recursive
```

The filesystem tests under `kotlin-dsv/src/fsTest` read fixtures from those submodules.

## IDE Setup

Install the [dprint](https://plugins.jetbrains.com/plugin/18192-dprint) plugin for format-on-save
support. The project's `dprint.jsonc` configures all formatting rules.

## Formatting

Auto-fix formatting issues:

```sh
mise run fix
```

Check formatting without making changes:

```sh
mise run check
```

A pre-commit hook is managed by [hk](https://hk.jdx.dev/) and runs formatting checks automatically
before each commit. It is installed as part of `mise install` via the `postinstall` hook.

## Running Tests

Run all tests:

```sh
mise run test
```

Run tests for a specific platform:

```sh
mise run test:jvm          # JVM tests
mise run test:jsnode       # JS Node tests
mise run test:wasmjsnode   # WASM JS Node tests
mise run test:native       # Native tests for the current platform
```

Run a full build and check all targets:

```sh
mise run build
```

## Documentation

Serve the documentation site locally. This passes the versions derived from Git tags, which the site
prints as the coordinates to depend on:

```sh
mise run docs
```

## Releasing

Releases are tagged `vMAJOR.MINOR.PATCH`. Pushing that tag runs `.github/workflows/release.yml`,
which publishes to Maven Central and deploys the documentation site. Snapshots publish from
`.github/workflows/daily.yml` when `main` has moved. Run `mise run version` to see the versions a
checkout would publish.
