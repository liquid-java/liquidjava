# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LiquidJava is an additional type checker for Java that adds **liquid types** (refinements) and **typestates** on top of standard Java. Users annotate Java code with `@Refinement`, `@StateRefinement`, `@StateSet` etc. (from `liquidjava-api`); the verifier parses the program with [Spoon](https://spoon.gforge.inria.fr/), translates refinement predicates to SMT, and discharges verification conditions with **Z3**.

Requires **Java 20+** and **Maven 3.6+** (the parent POM declares 1.8 source/target, but the verifier module overrides to 20).

## Module Layout

This is a Maven multi-module build (`pom.xml` is the umbrella):

- `liquidjava-api` — published annotations (`@Refinement`, `@RefinementAlias`, `@StateRefinement`, `@StateSet`, ghost functions). Stable artifact users depend on.
- `liquidjava-verifier` — the actual checker (Spoon processor + RJ AST + SMT translator). Published as `io.github.liquid-java:liquidjava-verifier`.
- `liquidjava-example` — sample programs **and the test suite** under `src/main/java/testSuite/`. The verifier's tests scan this directory.

Verifier package map (`liquidjava-verifier/src/main/java/liquidjava/`):
- `api/` — entrypoints; `CommandLineLauncher` is the CLI main.
- `processor/` — Spoon processors. `RefinementProcessor` orchestrates; `refinement_checker/` contains `RefinementTypeChecker`, `MethodsFirstChecker`, `ExternalRefinementTypeChecker`, plus `general_checkers/` and `object_checkers/` for typestate.
- `rj_language/` — the Refinements Language (RJ): `parsing/` (refinement strings → AST), `ast/`, `opt/` (expression simplification), `visitors/`.
- `smt/` — Z3 translation (`TranslatorToZ3`, `ExpressionToZ3Visitor`, `SMTEvaluator`, `Counterexample`).
- `diagnostics/` — error and warning reporting (`errors/`, `warnings/`).
- `utils/` — shared utilities and constants.

## Commands

Build / install everything:
```bash
mvn clean install
```

Run the test suite (verifier module, runs whole `testSuite/` dir); `./mvnw` works in place of `mvn`:
```bash
mvn test
```

Run a single test method (JUnit 4/5 mix — both work via Surefire):
```bash
mvn -pl liquidjava-verifier -Dtest=TestExamples test
mvn -pl liquidjava-verifier -Dtest=TestExamples#testMultiplePaths test
```

Verify a specific file/directory from CLI (uses the `liquidjava` script in repo root, macOS/Linux; it recompiles the verifier only when local sources or Maven files changed):
```bash
./liquidjava liquidjava-example/src/main/java/testSuite/CorrectSimpleAssignment.java
```
Equivalent raw form:
```bash
mvn exec:java -pl liquidjava-verifier \
  -Dexec.mainClass="liquidjava.api.CommandLineLauncher" \
  -Dexec.args="/path/to/file_or_dir"
```
CLI options: one or more paths, `-h`/`--help`, `-v`/`--version`, `-d`/`--debug` (debug logging, skips expression simplification), `-lsp`/`--language-server`.

Code formatting runs automatically in the `validate` phase via `formatter-maven-plugin` (configured for Java 20 in `liquidjava-verifier/pom.xml`); no separate lint command.

## Test Suite Conventions

Tests are discovered by `TestExamples#testPath` (parameterized) under `liquidjava-example/src/main/java/testSuite/`:

- Every `.java` file outside a leaf directory is a single-file test case.
- Every leaf directory (no subdirectories) is a single test case covering all its files.
- File and directory names do not matter (the `Correct…`/`Error…`/`…_correct`/`…_error` names are only a convention).
- Expected diagnostics are declared with inline `// Expect: <Title> Error` or `// Expect: Warning` comments on **the line where each diagnostic should be reported** (regex `//\s*Expect:\s*(.*?\b(Error|Warning)\b)`, case-insensitive — see `TestUtils#getExpectedDiagnosticsFromFile`). For errors both the title and the line must match; for warnings only the line. The number of expectations must equal the number of reported diagnostics, so a test with no expectations must produce none. Directory cases collect expectations from every file in the directory; there are no `.expected` files.

When adding new test cases, place them under `liquidjava-example/src/main/java/testSuite/` — that is the only way they get picked up.

## Architecture Notes That Span Files

- **Two-pass typechecking.** `MethodsFirstChecker` collects method signatures and refinement contracts before `RefinementTypeChecker` walks bodies, so forward references and recursion resolve. Edits to one usually need a matching change in the other.
- **Refinement string → AST → Z3.** A `@Refinement("a > 0")` string flows: `rj_language` parser → `ast` nodes → `smt/TranslatorToZ3` / `ExpressionToZ3Visitor`. New predicate forms generally require touching all three.
- **External refinements.** `ExternalRefinementTypeChecker` plus `*Refinements.java` companion files specify contracts for third-party APIs without modifying their sources.
- **Typestate** lives in `processor/refinement_checker/object_checkers/` and uses `@StateRefinement` / `@StateSet` from the API. Ghost-state predicates flow through the same SMT pipeline as value refinements.
- **Z3 dependency.** The verifier calls Z3 in-process through the Java bindings bundled by `z3-turnkey` (no separate Z3 install); failures often surface as `SMTResult` errors or counterexamples, not Java exceptions.
