# Contributing to LiquidJava

Thanks for your interest in contributing! This guide covers the essentials.

## Requirements

- Java 20+
- Maven 3.6+

## Build and Run

To build the project, run:

```bash
mvn clean install
```

Code formatting runs automatically via `formatter-maven-plugin` during the `validate` phase.

To verify a single file from the CLI, run:

```bash
./liquidjava path/to/File.java
```

This script recompiles `liquidjava-api` and `liquidjava-verifier` when local sources or Maven files have changed.

## Testing

To run all tests, run:

```bash
mvn test
```

To run specific tests, run:

```bash
mvn -pl liquidjava-verifier -Dtest=ExpressionFormatterTest test
mvn -pl liquidjava-verifier -Dtest=RefinementsParserTest test
mvn -pl liquidjava-verifier -Dtest=VCSimplificationTest test
```

## Release

Releases are published through GitHub Actions when release tags are pushed to
`main`:

- `v*` tags publish `liquidjava-verifier` through the `publish-verifier`
  workflow.
- `api-v*` tags publish `liquidjava-api` through the `publish-api` workflow.

Use `release.sh` from the repository root to publish a release.
The script automatically verifies the build and tests, bumps the module version, commits the version change, creates the matching tag, and pushes them to `main`.

```bash
./release.sh verifier # publishes liquidjava-verifier
./release.sh api      # publishes liquidjava-api
```

You can also pass the release version explicitly:

```bash
./release.sh verifier 1.2.3
./release.sh api 1.2.3
```

Run releases from a clean `main` branch. Once the tag is pushed, the matching
GitHub Actions workflow publishes the module to Maven Central.

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
