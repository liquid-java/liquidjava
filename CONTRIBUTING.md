# Contributing to LiquidJava

Thanks for your interest in contributing! This guide covers the essentials.

## Requirements

- Java 20+
- Maven 3.6+

## Build & test

```bash
mvn clean install   # build everything
mvn test            # run the test suite
```

Run a single test:

```bash
mvn -pl liquidjava-verifier -Dtest=TestExamples#testMultiplePaths test
```

Verify a single file from the CLI:

```bash
./liquidjava path/to/File.java
```

Code formatting runs automatically via `formatter-maven-plugin` during the `validate` phase.

## Adding test cases

Tests live under `liquidjava-example/src/main/java/testSuite/` and are auto-discovered:

- **Single file:** name it `Correct*.java` (should pass) or `Error*.java` (should fail).
- **Directory:** include `correct` or `error` in the directory name.

For failing cases (file or directory), mark each expected error with an inline `// <Error Title>` comment on the same line where the error should be reported, e.g.:

```java
int r = pos; // Refinement Error
```

The runner matches both the title and the line number, and the number of comments must equal the number of reported errors.


## Pull requests

1. Fork and create a branch from `main`.
2. Add tests for your change.
3. Run `mvn test` and make sure it passes.
4. Open a PR describing the change and linking any related issue.

## Reporting issues

Use the issue templates. Include a minimal reproducer when possible.

## Code of Conduct

By participating you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).
