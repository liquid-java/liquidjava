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
- `ast/` — AST of the Refinements Language (RJ).
- `rj_language/` — parser from refinement strings to RJ AST.
- `smt/` — Z3 translation (`TranslatorToZ3`, `ExpressionToZ3Visitor`, `SMTEvaluator`, `Counterexample`).
- `errors/`, `utils/`, `diagnostics/`.

## Commands

Build / install everything:
```bash
mvn clean install
```

Run the test suite (verifier module, runs whole `testSuite/` dir):
```bash
mvn test
```

Run a single test method (JUnit 4/5 mix — both work via Surefire):
```bash
mvn -pl liquidjava-verifier -Dtest=TestExamples test
mvn -pl liquidjava-verifier -Dtest=TestExamples#testMultiplePaths test
```

Verify a specific file/directory from CLI (uses the `liquidjava` script in repo root, macOS/Linux):
```bash
./liquidjava liquidjava-example/src/main/java/testSuite/CorrectSimpleAssignment.java
```
Equivalent raw form:
```bash
mvn exec:java -pl liquidjava-verifier \
  -Dexec.mainClass="liquidjava.api.CommandLineLauncher" \
  -Dexec.args="/path/to/file_or_dir"
```

Code formatting runs automatically in the `validate` phase via `formatter-maven-plugin` (configured for Java 20 in `liquidjava-verifier/pom.xml`); no separate lint command.

## Test Suite Conventions

Tests are discovered by `TestExamples#testPath` (parameterized) under `liquidjava-example/src/main/java/testSuite/`:

- Single-file cases: filename starts with `Correct…` or `Error…`.
- Directory cases: directory name contains the substring `correct` or `error`.
- Anything else is **ignored** (so helper sources can live alongside).
- Expected errors for a failing case are declared with inline `// <Error Title>` comments on **the line where each error should be reported** (regex `//\s*(.*?\bError\b)`, case-insensitive — see `TestUtils#getExpectedErrorsFromFile`). Both the title and the line number must match, and the count of comments must equal the count of reported errors. Directory cases work the same way: the scanner walks every file in the directory; there are no `.expected` files.

When adding new test cases, place them under `liquidjava-example/src/main/java/testSuite/` following the naming rules above — that is the only way they get picked up.

## Architecture Notes That Span Files

- **Two-pass typechecking.** `MethodsFirstChecker` collects method signatures and refinement contracts before `RefinementTypeChecker` walks bodies, so forward references and recursion resolve. Edits to one usually need a matching change in the other.
- **Refinement string → AST → Z3.** A `@Refinement("a > 0")` string flows: `rj_language` parser → `ast` nodes → `smt/TranslatorToZ3` / `ExpressionToZ3Visitor`. New predicate forms generally require touching all three.
- **External refinements.** `ExternalRefinementTypeChecker` plus `*Refinements.java` companion files specify contracts for third-party APIs without modifying their sources. The `co-specifying-liquidjava` skill covers this workflow.
- **Typestate** lives in `processor/refinement_checker/object_checkers/` and uses `@StateRefinement` / `@StateSet` from the API. Ghost-state predicates flow through the same SMT pipeline as value refinements.
- **Z3 dependency.** The verifier shells out to Z3 via JNI bindings; failures often surface as `SMTResult` errors or counterexamples, not Java exceptions.
