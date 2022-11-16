# UCR Tainting Checker


## How to run the checker

First, publish the checker and quals to your local Maven repository by running
`./gradlew publishToMavenLocal` in this repository.

Then, if you use Gradle, add the following to the `build.gradle` file in
the project you wish to type-check (using Maven is similar):

```
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    annotationProcessor 'edu.ucr.cs.riple.taint:ucrtainting-checker:0.1'
    implementation 'edu.ucr.cs.riple.taint:ucrtainting-checker:0.1'
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
