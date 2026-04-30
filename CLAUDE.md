# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo at a glance

C++ Vietnamese tokenizer used in Cốc Cốc Search and Ads. Ships three binding surfaces: CLI tools (`tokenizer`, `vn_lang_tool`), a pure-Java Maven module (`java/`), and Cython Python bindings (`python/`). A Debian package is also provided. The C++ core is stable; active development lives in the pure-Java module.

Project memory with milestone history is under `~/.claude/projects/.../memory/` — read it at session start to catch up on the current milestone.

## Java module (active dev area)

All commands run from the `java/` directory unless noted.

```bash
cd java && mvn -B clean install          # full build including dicts jar
cd java && mvn -B verify                 # unit + integration tests (matches CI)

# Inside java/ — subset runs
mvn -pl coccoc-tokenizer-java test                            # unit tests only
mvn -pl coccoc-tokenizer-java test -Dtest="TokenizerLoadTest" # single class
mvn -pl coccoc-tokenizer-java test -Dtest="TokenizerLoadTest#testInstance" # single method

# CI-matching: fail loudly if classpath dicts are absent
REQUIRE_DICTS=1 mvn -B verify
```

## C++ / native (less common)

```bash
mkdir build && cd build
cmake ..                # base: tokenizer, vn_lang_tool, dict_compiler
cmake -DBUILD_PYTHON=1 ..    # + Cython Python binding
make install

dpkg-buildpackage       # Debian package
```

## Java module architecture

Maven multi-module layout (`java/`):

- **parent** (`pom.xml`) — manages compiler settings, plugin versions, JUnit 5.11.4.
- **`coccoc-tokenizer-java-dicts`** — resource-only jar; bundles `multiterm.bin`, `syllable.bin`, optional `bigram.bin` at classpath path `com/coccoc/dicts/`.
- **`coccoc-tokenizer-java`** — engine; depends on dicts only in test scope.

Public API lives in `com.coccoc`: `Tokenizer` (per-dict-path singleton, thread-safe), `Token`, `TokenizeOption` (`NORMAL` / `HOST` / `URL`).

Engine internals are under `com.coccoc.internal`: `bigram/`, `build/`, `io/`, `lang/`, `segment/`, `trie/`. Build utilities (dict compiler) are under `com.coccoc.tools/`.

Two dict-load paths:
- `Tokenizer.getInstance()` — reads bundled classpath resources from the dicts jar (recommended).
- `Tokenizer.getInstance("/path/to/dicts")` — reads `multiterm.bin`, `syllable.bin`, and optionally `bigram.bin` from a directory on disk.

`GoldenFileIT` cross-checks Java tokenization output against expected C++ output. It is an integration test (ends in `IT`); run via `mvn verify`, not `mvn test`.

## Testing conventions

- JUnit 5 (Surefire/Failsafe) with `--enable-native-access=ALL-UNNAMED` argLine.
- Unit test classes end in `Test` (Surefire). Integration test classes end in `IT` (Failsafe, requires `mvn verify`).
- Java 21 (Temurin), `maven.compiler.release=21`.

## Release / contribution

- Conventional Commits: `fix(java): ...`, `chore(java): ...`, `feat(java): ...`.
- CI runs `mvn -B verify` with `REQUIRE_DICTS=1` on PRs touching `java/**` or `.github/workflows/java-ci.yml`.
- PRs squash-merge into `master`. SemVer label is applied automatically from the commit type; auto-release fires on merge.
