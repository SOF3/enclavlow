# enclavlow
A Java flow analysis tool for SGX data sensitivity

## Modules
- `util`: Internal non-enclavlow-specific utilities
- `core`: Core module for flow analysis
- `plugin`: Gradle integration plugin
- `api`: API to include from user code
- `doc-fig`: Module for generating image assets used in docs

![](build/reports/dependency-graph/dependency-graph.png)

## API
### `enclavlow-core`
The `analyzeMethod` function returns a Contract Flow Graph (CFG) for a single method.
The CFG detects whether the method is an ECall/OCall,
and presents the information flow throughout the method.

## Tests
To run the test cases in the `enclavlow-core` module, use the gradle task:

```
./gradlew core:tests
```

This command requires the graphviz `dot` command to be in PATH.

For the sake of convenience, all test case classes are compiled together with the test runtime,
and the test runtime classpath is used as the classpath for soot.

The test case classes are located in `core/src/test/java`,
and the test case results are checked in `core/src/test/kotlin`.

The JUnit test results can be found in `core/build/reports/test/test/index.html`.
For each `testMethod` test case,
the final Local Flow Graphs that contribute to output contract graph
are rendered by `dot` in the `core/build/lfgOutput` directory (see `index.html`).
(Due to use of parallel tests, the order in `index.html` is highly inconsistent)
