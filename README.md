## TaintTyper: Fast Type-Based Taint Checking for Java 

TaintTyper is a fast and practical static analysis tool for detecting taint-related security vulnerabilities in Java programs. It uses a type-based approach to track the flow of untrusted or sensitive data through code, identifying potential security issues such as injection attacks or data leaks. TaintTyper performs local, type-checking-based analysis to ensure that tainted data does not reach sensitive operations without proper sanitization.

**TaintTyper** is fast. It avoids whole-program dataflow analysis and instead leverages a pluggable type system to perform scalable, modular checks. In our benchmarks, TaintTyper achieves up to **23×** speedup over traditional taint analyses, while maintaining high precision and recall.

Designed for practicality and scalability, TaintTyper integrates easily into real-world codebases and continuous integration (CI) pipelines with minimal setup.


## Installation
Clone and publish TaintTyper locally:
```
git clone https://github.com/your-org/TaintTyper.git
cd TaintTyper
./gradlew publishToMavenLocal
```
This publishes `TaintTyper` to your local Maven repository.

Configure your project to use `TaintTyper` via the `Checker Framework` plugin:
In your `build.gradle` file, apply the Checker Framework Gradle plugin and set up the dependencies:
```
plugins {
    id 'org.checkerframework' version '0.6.22'
}

checkerFramework {
    checkers = [
        'edu.ucr.cs.riple.taint.ucrtainting.TaintTyper',
    ]
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'edu.ucr.cs.riple.taint:tainttyper:0.1'
    checkerFramework 'edu.ucr.cs.riple.taint:tainttyper:0.1'
}
```

This setup will run `TaintTyper` as part of your project’s Java compilation.

### Annotations

TaintTyper uses a small set of type qualifiers to track and restrict the flow of tainted data in your code:

- `@Tainted`: Indicates that a variable **may be influenced by untrusted input**. This is the __default__ type.
  
- `@Untainted`: Indicates that a value is cannot be tainted.

- `@PolyTainted`: A polymorphic qualifier that allows a method to preserve the taint status of its inputs in its output. If the input is tainted, the output is also considered tainted; if the input is untainted, the output is untainted as well.

#### Subtyping

`@Untainted` is a **subtype** of `@Tainted`. This means:

- You **can use** an `@Untainted` value wherever a `@Tainted` one is expected.
- You **cannot assign** a `@Tainted` value to a variable or parameter annotated as `@Untainted`, doing so will result in an type error.

This subtyping rule enforces that tainted values cannot flow into places that require untainted data, helping prevent security vulnerabilities.


### Example

`TaintTyper` tracks the flow of untrusted data and reports an error when tainted values may reach sensitive operations.

Consider the following code:

```java
Map<String, String> paths = ...;

String parentPath(String path) {
    return path.substring(0, path.lastIndexOf("/"));
}

void exec(String name) {
    String path = paths.get(name);
    String parent = parentPath(path);
    sink(Paths.get(parent).toAbsolutePath().toFile());
}

void sink(@Untainted String t) { ... }
```
In this example, `paths.get(name)` retrieves data from an untrusted map and ultimately, `sink(Paths.get(parent).toAbsolutePath().toFile())` is called with a tainted value, violating its `@Untainted` required type.

`TaintTyper` reports an error at the call to `sink(...)`, flagging a potential security vulnerability where untrusted input may flow into a sensitive operation.

To fix it, we have to encode that the map data is trusted and the checker will not report any error.
```java
Map<String, @Untainted String> paths = ...;

@PolyTaint String parentPath(@PolyTaint String path) {
    return path.substring(0, path.lastIndexOf("/"));
}

void exec(String name) {
    String path = paths.get(name);
    String parent = parentPath(path);
    sink(Paths.get(parent).toAbsolutePath().toFile());
}

void sink(@Untainted String t) { ... }
```

### Inference Support

`TaintTyper` supports automatic annotation inference to simplify adoption on existing codebases. The inference is built on top of [Annotator](https://github.com/ucr-riple/NullAwayAnnotator), an annotation inference engine. The inference engine analyzes the code and automatically infers the appropriate `@Untainted`, and `@PolyTainted` annotations.

In the example shown earlier, the inference tool can infer **all annotations** automatically.

To run `Annotator` on your codebase and infer annotations for `TaintTyper`:

1. Follow the setup instructions on the [Annotator](https://github.com/ucr-riple/NullAwayAnnotator).
2. Pass the flag to specify the TaintTyper checker: `-ch UCRTaint`
