
# ReadUpdateWrite

A Scala educational project demonstrating different approaches to the same simple program: reading data from a CSV file, updating protagonist ages based on user input, and writing the results back.

## Project Purpose

This project serves as a comprehensive learning resource for Scala programming, showcasing how the same basic logic can be implemented using various programming paradigms, patterns, and libraries. Each version highlights different aspects of Scala and functional programming concepts.

## The Program

All versions perform the same task:
1. Read a CSV file containing protagonist data (first name, last name, age)
2. Ask the user for an age increment value
3. Update each protagonist's age
4. Write the updated data back to the CSV file

## Versions

### Core Learning Versions

#### **DefaultVersion.scala**
The foundation version using straightforward, idiomatic Scala.
- **Focus**: Basic Scala features (objects, immutability, higher-order functions)
- **Execution**: Sequential, eager evaluation
- **Key Concepts**: Simple method calls, direct side effects
- **Best For**: Understanding basic Scala syntax and program structure

#### **LazyScalaCSVVersion.scala**
Demonstrates lazy evaluation and external library integration.
- **Focus**: `lazy val` for deferred computation, scala-csv library
- **Execution**: Conditional—file read only if needed
- **Key Concepts**: Lazy evaluation, CSV library usage, resource optimization
- **Best For**: Understanding lazy evaluation and working with external libraries

#### **CatsEffectVersion.scala**
Pure functional programming with referential transparency using Cats Effect.
- **Focus**: IO monad, effect system, pure functions
- **Execution**: Deferred—operations described, not executed until runtime
- **Key Concepts**: Referential transparency, IO monad, separation of description from execution
- **Best For**: Learning functional effect systems and pure FP

### Dependency Injection Versions

#### **MUnitVersion.scala**
Manual dependency injection using function parameters.
- **Focus**: Testability through function parameter injection
- **Execution**: Sequential with injected I/O operations
- **Key Concepts**: Higher-order functions, manual DI, testing with mocks
- **Best For**: Understanding basic dependency injection without frameworks

#### **DIVersion.scala**
Framework-assisted dependency injection using MacWire.
- **Focus**: Trait-based DI with compile-time wiring
- **Execution**: Sequential with trait-based abstraction
- **Key Concepts**: Traits as contracts, MacWire `wire[]` macro, OOP-style DI
- **Best For**: Learning framework-assisted dependency injection

#### **GivenUsingVersion.scala**
Scala 3's implicit context parameters for DI.
- **Focus**: `given`/`using` syntax for implicit dependency resolution
- **Execution**: Sequential with compiler-resolved dependencies
- **Key Concepts**: Context parameters, implicit resolution, Scala 3 features
- **Best For**: Understanding Scala 3's modern approach to implicits

#### **CapabilitiesVersion.scala** ⚠️ Experimental
Capability-based programming with context functions.
- **Focus**: Fine-grained capabilities, capture checking, `?->` syntax
- **Execution**: Sequential with explicit capability requirements
- **Key Concepts**: Context functions, capability-based security, type-level effects
- **Best For**: Exploring cutting-edge Scala 3 experimental features
- **Note**: Requires `-experimental` and `-language:experimental.captureChecking` flags

### Concurrency Versions

#### **FutureVersion.scala**
Two variants demonstrating Future-based concurrency.
- **FutureVersionWithAwait**: Basic Future usage with blocking wait
- **FutureVersionWithThreadPool**: Custom thread pool with non-blocking callbacks
- **Focus**: Parallel execution, async/await patterns, ExecutionContext
- **Execution**: File reading and user input happen in parallel
- **Key Concepts**: `Future`, `Await`, thread pools, concurrent I/O
- **Best For**: Learning Scala's standard async programming model

#### **PromiseVersion.scala**
Manual Future completion with Promise.
- **Focus**: Promise for explicit Future control
- **Execution**: Similar to Future but with manual completion
- **Key Concepts**: `Promise`, manual async control, delegation pattern
- **Best For**: Understanding the write-side of Futures and complex async scenarios

#### **PekkoVersion.scala** 
Actor Model implementation using Apache Pekko (formerly Akka).
- **Focus**: Actor-based concurrency, message passing
- **Execution**: Three independent actors communicating via messages
- **Key Concepts**: Actor Model, message protocols, asynchronous coordination
- **Best For**: Learning actor-based systems and message-driven architecture
- **Actors**: PekkoSystem (coordinator), ReadWriteActor (file I/O), UpdateActor (user input)

### Special: Educational Supplement

#### **CustomFuture.scala** 📚 Implementation Example
A minimal Future implementation showing how futures work under the hood.
- **Purpose**: Educational—demonstrates Future internals
- **Not a program variant**: Standalone example
- **Key Concepts**:
    - Asynchronous computation execution
    - Callback registration and execution
    - Completion state management
    - Try-based result handling
- **What It Shows**:
  ```scala
  class CustomFuture[T] {
    private var value: Option[Try[T]] = None
    private var onCompleted: List[Try[T] => Unit] = Nil
    
    def onComplete(f: Try[T] => Unit): Unit = /* ... */
    def isCompleted: Boolean = value.isDefined
  }
  ```
- **Best For**: Understanding how Scala's standard `Future` works internally
- **Compare With**: `FutureVersion` and `PromiseVersion` to see how the concepts apply

## Learning Path

**Recommended study order:**

1. **Start Here**: `DefaultVersion.scala` - Master the basics
2. **Lazy Evaluation**: `LazyScalaCSVVersion.scala` - Understand deferred computation
3. **Testing**: `MUnitVersion.scala` - Learn testable code design
4. **Frameworks**: `DIVersion.scala` - Explore framework-assisted DI
5. **Given/Using**: `GivenUsingVersion.scala` - Implicit parameters
6. **Pure FP**: `CatsEffectVersion.scala` - Referential transparency and effect systems
7. **Async Basics**: `CustomFuture.scala` - How futures work (supplement)
8. **Concurrency**: `FutureVersion.scala` - Standard async patterns
9. **Advanced Async**: `PromiseVersion.scala` - Manual control
10. **Actor Model**: `PekkoVersion.scala` - Message-driven concurrency
11. **Experimental**: `CapabilitiesVersion.scala` - Cutting-edge features (optional)


## Testing

Unit tests are provided for testable versions:
- `MUnitVersionSuite.scala` - Tests for MUnitVersion
- `DIVersionSuite.scala` - Tests for DIVersion
- `GivenUsingVersionSuite.scala` - Tests for GivenUsingVersion
- `CapabilitiesVersionSuite.scala` - Tests for CapabilitiesVersion

## Running the Code

Each version has a `main()` method (commented out with `/* @main */`). Uncomment one at a time to run.

## Documentation

Each version contains comprehensive Scaladoc comments explaining:
- What Scala features are demonstrated
- How the version differs from DefaultVersion
- Key concepts and patterns
- When to use each approach
- Step-by-step execution flow

Read the source files for detailed educational commentary!

## License

GPL 3.0 - See LICENSE file for details

## Author

Maciej Gorywoda (makingthematrix@protonmail.com)

---

**Happy Learning! 🎓**

This project is designed for students learning Scala. Take your time with each version, understand the concepts, and see how the same problem can be solved in many different ways.