# LiquidJava - Extending Java with Liquid Types
[![VS Code Extension](https://img.shields.io/visual-studio-marketplace/v/AlcidesFonseca.liquid-java?label=VS%20Code&logo=visual-studio-code)](https://marketplace.visualstudio.com/items?itemName=AlcidesFonseca.liquid-java)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.liquid-java/liquidjava-api)](https://central.sonatype.com/artifact/io.github.liquid-java/liquidjava-api)


![LiquidJava Banner](docs/design/figs/banner.gif)

## Welcome to LiquidJava!

LiquidJava is an additional type checker for Java, based on **liquid types** and **typestates**, which provides additional safety guarantees to Java programs through **refinements** at compile time.

### Refinements

To refine a variable, field, parameter or return value, use the `@Refinement` annotation with a predicate as an argument. The predicate must be a boolean expression that uses the name of the variable being refined (or `_`) to refer to its value.

```java
@Refinement("a > 0")
int a = 3;
a = -8; // Refinement Error


@Refinement("_ >= low && _ <= high")
public static int midpoint(
    @Refinement("_ <= high") int low,
    int high
) {
    return low + (high - low) / 2;
}

midpoint(5, 10);
midpoint(10, 5); // Refinement Error
```

### State Refinements

LiquidJava also supports object state modeling via typestates, which allows enforcing protocols on objects using the `@StateSet` and `@StateRefinement` annotations. The `@StateSet` annotation defines the possible states of an object, while the `@StateRefinement` annotation specifies the allowed transitions between states for each method. The `@ExternalRefinementsFor` annotation is used to specify an external class for which the refinements are being defined.

```java
@ExternalRefinementsFor("java.net.Socket")
@StateSet({"unconnected", "bound", "connected", "closed"})
public interface SocketRefinements {
    @StateRefinement(to="unconnected(this)")
    public void Socket();

    @StateRefinement(from="unconnected(this)", to="bound(this)")
    public void bind(SocketAddress add);

    @StateRefinement(from="bound(this)", to="connected(this)")
    public void connect(SocketAddress add);

    @StateRefinement(from="connected(this)")
    public void sendUrgentData(int n);

    @StateRefinement(from="!closed(this)", to="closed(this)")
    public void close();
}

Socket socket = new Socket();
socket.bind(new InetSocketAddress("localhost", 8080));
socket.sendUrgentData(1); // State Refinement Error
socket.close();
```

### Ghosts

Finally, LiquidJava also provides ghost variables that are used to track additional information about the program state with the `@Ghost` annotation. These are also updated through the `@StateRefinement` annotation.

```java
@ExternalRefinementsFor("java.util.Stack")
@Ghost("int size")
public interface StackRefinements<E> {
    @StateRefinement(to="size(this) == 0")
    public void Stack();

    @StateRefinement(to="size(this) == size(old(this)) + 1")
    public E push(E elem);

    @StateRefinement(from="size(this) > 0", to="size(this) == size(old(this)) - 1")
    public E pop();

    @StateRefinement(from="size(this) > 0")
    public E peek();
}

Stack<Integer> stack = new Stack<>();
stack.push(1);
stack.pop();
stack.pop(); // State Refinement Error
```

## Getting Started

### VS Code Extension

The easiest way to use LiquidJava is through its [VS Code extension](https://marketplace.visualstudio.com/items?itemName=AlcidesFonseca.liquid-java), which uses the LiquidJava verifier directly inside VS Code, with real-time error diagnostics and syntax highlighting for refinements.

### Command Line

For development, you may use the LiquidJava verifier from the command line.

#### Prerequisites

Before setting up LiquidJava, ensure you have the following installed:

- Java 20+ - JDK for compiling and running Java programs
- Maven 3.6+ - For building and dependency management

Additionally, you'll need the following dependency, which includes the LiquidJava API annotations:

#### Maven
```xml
<dependency>
    <groupId>io.github.liquid-java</groupId>
    <artifactId>liquidjava-api</artifactId>
    <version>0.0.7</version>
</dependency>
```

#### Gradle
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.liquid-java:liquidjava-api:0.0.7'
}
```

#### Setup

1. Clone the repository: `git clone https://github.com/liquid-java/liquidjava.git`
2. Build the project `mvn clean install`
3. Run tests to verify installation: `mvn test`
4. If importing into an IDE, import the project as a Maven project using the root `pom.xml`

#### Run Verification

To run LiquidJava, use the Maven command below, replacing `/path/to/your/project` with the path to the Java file or directory you want to verify.

```bash
mvn exec:java -pl liquidjava-verifier -Dexec.mainClass="liquidjava.api.CommandLineLauncher" -Dexec.args="/path/to/your/project"
```

If you're on Linux/macOS, you can use the `liquidjava` script (from the repository root) to simplify the process.
The script recompiles the verifier only when local sources or Maven files have changed.

The LiquidJava verifier can be run from the command line with the following options:

| Option | Description |
| --- | --- |
| `<...paths>` | Paths (files or directories) to be verified by LiquidJava |
| `-h`, `--help` | Show the help message with available options |
| `-v`, `--version` | Show the current version of the verifier |
| `-d`, `--debug` | Enable debug logging and skip expression simplification for troubleshooting |
| `-lsp`, `--language-server` | Enable language server mode for editor support |

**Test a correct case**:
```bash
./liquidjava liquidjava-example/src/main/java/testSuite/CorrectSimpleAssignment.java
```

This should output: `Correct! Passed Verification`.

**Test an error case**:
```bash
./liquidjava liquidjava-example/src/main/java/testSuite/ErrorSimpleAssignment.java
```

This should output an error message describing the refinement violation.

#### Testing

Run `mvn test` to run all the tests in LiquidJava.

The starter test file is `TestExamples.java`, which runs the test suite under the `testSuite` directory in `liquidjava-example`.

The test suite considers test cases:
1. Files that start with `Correct` or `Error` (e.g., `CorrectRecursion.java`)
2. Directories that contain the word `correct` or `error` (e.g., `arraylist_correct`)

Therefore, the files and folders that do not follow this pattern are ignored.

For failing test cases, the expected error must be specified as follows:
1. In singular test files, the expected error (title) should be written in the first line of the file as a comment
2. In test directories, a `.expected` file should be included in that directory with the expected error (title)

## Project Structure

* **docs**: Contains documents used for the design of the language. This folder includes a [README](./docs/design/README.md) with the link to the full artifact used in the design process. It also contains initial documents used to prepare the design of the refinements language during its evaluation
* **liquidjava-api**: Includes the annotations that can be introduced in the Java programs to add the refinements
* **liquidjava-example**: Includes some examples and the test suite used for testing the verifier
* **liquidjava-verifier**: Includes the implementation of the verifier. Its main packages are:
  * `api`: Includes the `CommandLineLauncher`, which verifies one or more specified files or directories
  * `diagnostics`: Reports verification errors and warnings
  * `processor`: Handles the type checking
  * `rj_language`: Parses refinement strings and contains the Refinements Language (RJ) AST
  * `smt`: Translates verification conditions to the SMT solver and processes its results
  * `utils`: Provides shared utility classes

## References

You can find out more about LiquidJava in the following resources:

* [LiquidJava Website](https://liquid-java.github.io)
* [VS Code Extension (Marketplace)](https://marketplace.visualstudio.com/items?itemName=AlcidesFonseca.liquid-java)
* [VS Code Extension (Source Code)](https://github.com/liquid-java/vscode-liquidjava)
* [LiquidJava Examples](https://github.com/liquid-java/liquidjava-examples)
* [LiquidJava External Libraries Examples](https://github.com/liquid-java/liquid-java-external-libs)
* [LiquidJava MCP](https://github.com/liquid-java/liquidjava-mcp)
<!-- * [Formalization of LiquidJava](https://github.com/liquid-java/liquidjava-formalization) - not opensource yet -->