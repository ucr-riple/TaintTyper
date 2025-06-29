## TaintTyper: Fast Type-Based Taint Checking for Java 

TaintTyper is a fast and practical static analysis tool for detecting taint-related security vulnerabilities in Java programs. It uses a type-based approach to track the flow of untrusted or sensitive data through code, identifying potential security issues such as injection attacks or data leaks. TaintTyper performs local, type-checking-based analysis to ensure that tainted data does not reach sensitive operations without proper sanitization.

**TaintTyper** is fast. It avoids whole-program dataflow analysis and instead leverages a pluggable type system to perform scalable, modular checks. In our benchmarks, TaintTyper achieves up to **23×** speedup over traditional taint analyses, while maintaining high precision and recall.

Designed for practicality and scalability, TaintTyper integrates easily into real-world codebases and continuous integration (CI) pipelines with minimal setup.


## How to run the checker

First, publish the checker and quals to your local Maven repository by running
`./gradlew publishToMavenLocal` in this repository.

Then, if you use Gradle, add the following to the `build.gradle` file in
the project you wish to type-check (using Maven is similar):

```
checkerFramework {
    checkers = [
            'edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker',
    ]
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'edu.ucr.cs.riple.taint:ucrtainting-checker:0.1'
    checkerFramework 'edu.ucr.cs.riple.taint:ucrtainting-checker:0.1'
}
```

Now, when you build your project, the UCR Tainting Checker will also run,
informing you of any potential errors related to TODO.


## How to specify your code


`@RTainted`:
TODO

`@RUntainted`:
TODO

`@RPolytainted`:
TODO

## How to build the checker

Run these commands from the top-level directory.

`./gradlew build`: build the checker

`./gradlew publishToMavenLocal`: publish the checker to your local Maven repository.
This is useful for testing before you publish it elsewhere, such as to Maven Central.

The UCR Tainting Checker is built upon the Checker Framework.  Please see
the [Checker Framework Manual](https://checkerframework.org/manual/) for
more information about using pluggable type-checkers, including this one.
