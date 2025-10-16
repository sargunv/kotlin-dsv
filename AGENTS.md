# Agent Guide for kotlin-dsv

## Commands

- Build: `./gradlew build` or `just build`
- Lint: `./gradlew detekt` or `just detekt`
- Format: `just format` (runs pre-commit hooks)
- Test (all): `./gradlew allTests` or `just test`
- Test (JVM): `./gradlew jvmTest` or `just test-jvm`
- Test (single file): `./gradlew jvmTest --tests ClassName` (e.g.,
  `./gradlew jvmTest --tests EncoderDecoderTest`)
- Coverage: `./gradlew :koverHtmlReport` or `just coverage`
- Docs: `./gradlew :dokkaGenerateHtml` or `just build-dokka`

## Architecture

- Kotlin Multiplatform library for delimiter-separated values (CSV/TSV)
- Main module: `kotlin-dsv/` - Core library with DSV parsing, writing,
  encoding/decoding
- Subprojects: `benchmark/` (performance benchmarks)
- Uses kotlinx.serialization for type-safe serialization and kotlinx.io for
  streaming
- Package: `dev.sargunv.kotlindsv`

## Code Style

- Indent: 2 spaces for Kotlin, 4 spaces for other files
- All public APIs must be documented (detekt enforces
  UndocumentedPublicClass/Function/Property)
- Use kotlinx.serialization annotations (e.g., `@Serializable`)
- Error handling: throw `DsvParseException` for parsing errors
- Naming: use `DsvNamingStrategy` for column name transformations
- **IMPORTANT**: Always run the formatter before committing any changes using
  `just format` or `pre-commit run --all-files --hook-stage manual`. This
  formats code and dumps the ABI.

## Commit Guidelines

Never make a commit unless explicitly asked to do so. Such permission only
extends to that one commit, not to future commits in that session.

**Before committing, ALWAYS run the formatter:**

```bash
just format
```

This will format all code files and dump the ABI. Do not skip this step as it is
required for all commits.

When making commits, always include a signoff in the commit message following
this format:

```
🤖 Generated with [Agent Name](https://agent-url)

Co-Authored-By: Agent Name <example@agent-domain>
```

Examples:

- Claude: `🤖 Generated with [Claude Code](https://claude.com/claude-code)` and
  `Co-Authored-By: Claude <noreply@anthropic.com>`
- OpenCode: `🤖 Generated with [OpenCode](https://opencode.ai)` and
  `Co-Authored-By: OpenCode <noreply@opencode.ai>`
- Amp: `🤖 Generated with [Amp](https://ampcode.com)` and
  `Co-Authored-By: Amp <amp@ampcode.com>`

Each coding agent should use their own Author and URL but maintain the same
format.
