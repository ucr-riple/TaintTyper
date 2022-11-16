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

checkerFramework {
    checkers = [
            'edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingChecker',
    ]
    extraJavacArgs = [
            '-Astubs=' + projectDir + '/stubs'
    ]
}

dependencies {
    checkerFramework 'edu.ucr.cs.riple.taint:ucrtainting-checker:0.1'
    implementation 'edu.ucr.cs.riple.taint:ucrtainting-qual:0.1'
}
```

Now, when you build your project, the UCR Tainting Checker will also run,
informing you of any potential errors related to TODO.


## How to specify your code


`@Tainted`:
TODO

`@Untainted`:
TODO

`@Polytainted`:
TODO

## How to build the checker

Run these commands from the top-level directory.

`./gradlew build`: build the checker

`./gradlew publishToMavenLocal`: publish the checker to your local Maven repository.
This is useful for testing before you publish it elsewhere, such as to Maven Central.

You will need to uncomment _publishing_ task for _ucrtainting-checker-qual/build.gradle_
to build and publish the qual jar. TODO: it's a bit odd to publish this 
separately. There should be another way to do this.
## More information

The UCR Tainting Checker is built upon the Checker Framework.  Please see
the [Checker Framework Manual](https://checkerframework.org/manual/) for
more information about using pluggable type-checkers, including this one.
