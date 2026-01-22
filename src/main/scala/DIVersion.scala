import com.softwaremill.macwire.wire

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * A trait defining the contract for I/O operations needed in the protagonist update workflow.
 *
 * **Key Difference from MUnitVersion:**
 * MUnitVersion uses function parameters (`() => List[String]`, etc.) while DIVersion uses
 * a trait with methods. This is a more object-oriented approach to dependency injection.
 *
 * **Scala Features:**
 *  - **Trait**: Similar to Java interfaces, but can contain concrete implementations
 *  - **Abstract methods**: No implementation, must be provided by implementing classes
 *
 * **Testing Strategy:**
 * In tests (DIVersionSuite), we implement this trait with mock behavior, just like
 * MUnitVersion passes mock functions.
 */
trait ReadUpdateWrite {
  def readLines(path: Path): List[String]
  def askForUpdate(): Int
  def writeLines(path: Path, lines: List[String]): Unit
}

/**
 * Production implementation of the ReadUpdateWrite trait using real file and console I/O.
 *
 * **Key Scala Features:**
 *  - **Trait Implementation**: Uses `extends` to implement the trait interface
 *  - **Override keyword**: Required when implementing trait methods (unlike Java's @Override)
 *
 * **Why a Separate Implementation Class?**
 *  - **Clean separation**: Trait defines contract, class provides implementation
 *  - **Testability**: Easy to create alternative implementations for testing
 *  - **Dependency Injection**: MacWire can automatically instantiate this class
 *
 * **Note:**
 * The implementation methods are identical to those in DefaultVersion and MUnitVersion.
 * The difference is in how they're organized and injected, not what they do.
 */
class ReadUpdateWriteImpl extends ReadUpdateWrite {
  override def readLines(path: Path): List[String] =
    Files.readAllLines(path).asScala.toList

  override def askForUpdate(): Int = {
    printf("By how much should I update the age? ")
    val answer = scala.io.StdIn.readLine()
    answer.toInt
  }

  override def writeLines(path: Path, lines: List[String]): Unit =
    Files.writeString(path, lines.mkString("\n"))
}

/**
 * DIVersion demonstrates framework-assisted dependency injection using MacWire.
 * This version builds on MUnitVersion's testability pattern but uses a trait-based approach
 * with automated wiring instead of manual function parameter passing.
 *
 * **Key Differences from MUnitVersion:**
 *  - **Trait vs. Functions**: Uses `ReadUpdateWrite` trait instead of function parameters
 *  - **MacWire Framework**: Uses `wire[]` macro for automatic dependency resolution
 *  - **Import wildcard**: Uses `import instance.*` to bring trait methods into scope
 *  - **More OOP-style**: Object-oriented approach vs. functional approach
 *
 * **Key Differences from DefaultVersion:**
 *  - **Class vs. Object**: Uses a `class` to allow multiple instances with different dependencies
 *  - **Dependency Injection**: I/O operations provided via constructor, not hardcoded
 *  - **Separation of Concerns**: Business logic separated from I/O implementation
 *  - **Testability**: Can inject mock implementations for testing
 *
 *
 * **What Does `wire[]` Do?**
 * The `wire[]` macro:
 *  1. Looks at the constructor parameters of the class
 *  2. Searches the current scope for matching values/instances
 *  3. Automatically passes them to the constructor
 *  4. Returns the constructed instance
 *
 * It's compile-time logic that eliminates boilerplate instantiation code.
 *
 * **Testing Strategy:**
 * {{{
 * // In tests (DIVersionSuite), create mock implementation:
 * val mockInstance = new ReadUpdateWrite {
 *   override def readLines(path: Path) = List("mock data")
 *   override def askForUpdate() = 5
 *   override def writeLines(path: Path, lines: List[String]) = // capture output
 * }
 * val app = new DIVersion(mockInstance)
 * app.run()
 * }}}
 *
 * **When to Use DI Pattern?**
 *  - **Larger applications**: When you have many dependencies to wire
 *  - **Complex dependency graphs**: When dependencies have dependencies
 *  - **Team preferences**: When your team prefers OOP-style DI over functional DI
 *  - **Framework integration**: When using other frameworks that expect trait-based DI
 *
 * @param instance The ReadUpdateWrite implementation providing I/O operations
 */
class DIVersion(instance: ReadUpdateWrite) {
  import DIVersion.{FilePath, updateAge}
  import instance.*

  /**
   * Executes the read-update-write workflow using injected dependencies.
   *
   * **Key Scala Feature: Import Wildcard**
   * The `import instance.*` at the class level brings all methods from the `ReadUpdateWrite`
   * trait into scope, so we can call them directly without prefixing with `instance.`
   *
   * **Almost Identical to DefaultVersion:**
   * Notice how similar this `run` method is to `DefaultVersion.main`. The business logic
   * is the same, we've just abstracted where the I/O operations come from.
   */
  def run(): Unit = {
    val lines        = readLines(FilePath)
    val protagonists = lines.map(Protagonist.fromLine)
    val n            = askForUpdate()
    val updated      = protagonists.map(updateAge(_, n))
    val newLines     = updated.map(_.toLine)
    writeLines(FilePath, newLines)
  }
}

/**
 * Companion object for DIVersion demonstrating MacWire-based dependency injection setup.
 *
 * **Key Scala Features Demonstrated:**
 *  - **MacWire `wire[]` Macro**: Automatic dependency resolution at compile time
 *  - **Lazy Initialization**: Using `lazy val` to defer instantiation
 *
 * **MacWire Dependency Resolution:**
 * {{{
 * // MacWire looks at this:
 * class DIVersion(instance: ReadUpdateWrite)  // Needs a ReadUpdateWrite
 * // MacWire searches the scope and finds:
 * private lazy val instance: ReadUpdateWrite = wire[ReadUpdateWriteImpl]
 * // So when you write:
 * private lazy val diVersion: DIVersion = wire[DIVersion]
 * // MacWire generates (conceptually):
 * private lazy val diVersion: DIVersion = new DIVersion(instance)
 * }}}
 *
 * **Why Lazy Initialization?**
 * Using `lazy val` means:
 *  - Instances created only when needed (not at object initialization)
 *  - Safe(r) circular dependency resolution (if needed in complex graphs)
 *  - Efficient resource usage
 *
 * **Benefits of MacWire Approach:**
 *  - **Less boilerplate**: No manual wiring code
 *  - **Compile-time safety**: Errors caught at compile time, not runtime
 *  - **Refactoring-friendly**: Add constructor parameters, MacWire adapts automatically
 *
 * **Trade-offs:**
 *  - **Learning curve**: Need to understand how MacWire works
 *  - **Macro magic**: Less explicit than manual wiring
 */
object DIVersion {
  /**
   * Entry point that runs the application with MacWire-injected dependencies.
   */
  def main(): Unit = diVersion.run()

  /**
   * MacWire-instantiated production implementation of ReadUpdateWrite.
   *
   * **Why Lazy?**
   * This instance is only created when `diVersion` is first accessed, which happens
   * when `main()` is called. This is efficient and safe.
   *
   * **Testing Override:**
   * In tests, you don't use this — you create your own mock implementation:
   * {{{
   * // Tests don't call main(), they do:
   * val testInstance = new ReadUpdateWrite { /* mock implementation */ }
   * val testApp = new DIVersion(testInstance)
   * }}}
   */
  private lazy val instance: ReadUpdateWrite = wire[ReadUpdateWriteImpl]

  /**
   * MacWire-instantiated DIVersion with automatically resolved dependencies.
   *
   * **Dependency Resolution:**
   * MacWire automatically matches `instance` parameter with the `instance`
   * defined above.
   */
  private lazy val diVersion: DIVersion = wire[DIVersion]

  private val FilePath = Path.of("resources/protagonists.csv")

  private def updateAge(p: Protagonist, n: Int): Protagonist = {
    val newAge = p.age + n
    println(s"The age of ${p.firstName} ${p.lastName} changes from ${p.age} to $newAge")
    p.copy(age = newAge)
  }
}
