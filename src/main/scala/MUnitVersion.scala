import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import org.slf4j.LoggerFactory

/**
 * MUnitVersion demonstrates logic injection through constructor parameters to enable unit testing.
 * This version refactors DefaultVersion to separate business logic from I/O operations, making
 * the code testable without requiring actual file system or console access.
 *
 * **Key Differences from DefaultVersion:**
 *  - **Class vs. Object**: Uses a `class` instead of a singleton `object` to allow multiple instances
 *  - **Function parameters**: I/O operations passed as function parameters, not hardcoded
 *  - **Testability**: Can be instantiated with mock functions for testing (see MUnitVersionSuite)
 *  - **Separation of Concerns**: Business logic (the `run` method) separated from I/O implementation
 *
 * **Key Scala Features Demonstrated:**
 *  - **Function Types**: `() => List[String]`, `() => Int`, `List[String] => Unit` as parameters
 *  - **Higher-Order Functions**: Functions that accept other functions as parameters
 *  - **Apply Method**: Companion object's `apply()` creates instances with default behavior
 *  - **Manual Dependency Injection**: Simple DI without frameworks (compare with DIVersion which uses MacWire)
 *
 * **Testing Strategy:**
 * {{{
 * // In tests (MUnitVersionSuite), we inject mock functions:
 * val app = new MUnitVersion(
 *   readLines    = () => List("Jonas,Kahnwald,17"),  // Mock data, no file I/O
 *   askForUpdate = () => 5,                          // Mock user input
 *   writeLines   = lines => mockStorage = lines      // Capture output for assertions
 * )
 * app.run()  // Runs with mocks, no side effects!
 * 
 * // In production (main), we inject real functions:
 * val app = MUnitVersion()  // Uses apply() with real file/console I/O
 * app.run()  // Actually reads/writes files and prompts user
 * }}}
 *
 * **Why This Pattern?**
 *  - **Tests don't touch file system**: Fast, reliable, no cleanup needed
 *  - **Tests don't need user input**: Fully automated
 *  - **Tests can verify behavior**: Track how many times functions were called
 *  - **Business logic is pure**: The `run` method just coordinates, doesn't do I/O itself
 *
 * **Learning Note:**
 * Compare with:
 *  - **DefaultVersion**: Direct I/O calls, hard to test
 *  - **DIVersion**: Same pattern but with MacWire framework and trait-based injection
 *  - **GivenUsingVersion**: Same pattern but with Scala 3's `given`/`using` for implicit injection
 *
 * @param readLines A function that returns a list of CSV lines (abstracts file reading)
 * @param askForUpdate A function that returns the age increment (abstracts user input)
 * @param writeLines A function that accepts updated CSV lines (abstracts file writing)
 */
class MUnitVersion(readLines   : () => List[String],
                   askForUpdate: () => Int,
                   writeLines  : List[String] => Unit) {
  import MUnitVersion.updateAge

  /**
   * Executes the read-update-write workflow using injected dependencies.
   * Note how similar it is to the `run` method of `DefaultVersion`.
   */
  def run(): Unit = {
    val lines        = readLines()
    val protagonists = lines.map(Protagonist.fromLine)
    val n            = askForUpdate()
    val updated      = protagonists.map(updateAge(_, n))
    val newLines     = updated.map(_.toLine)
    writeLines(newLines)
  }
}

/**
 * Companion object for MUnitVersion providing production implementations and factory methods.
 *
 * **Key Scala Features Demonstrated:**
 *  - **Companion Objects**: Can access private members of the class, provide factory methods
 *  - **Apply Method**: Allows `MUnitVersion()` instead of `new MUnitVersion(...)`
 *  - **Partial Application**: `writeLines(FilePath, _)` creates a function with one parameter fixed
 */
object MUnitVersion {
  /**
   * Entry point that creates and runs a MUnitVersion with production implementations.
   * This delegates to `apply()` which injects the real file and console I/O functions.
   */
  /* @main */ def main(): Unit = MUnitVersion().run()

  /**
   * Factory method that creates a MUnitVersion instance with production implementations.
   *
   * **Key Scala Features:**
   *  - **Apply Method**: Special method name that allows calling `MUnitVersion()` like a function
   *  - **Lambda Syntax**: `() => readLines(FilePath)` creates a zero-parameter function
   *  - **Partial Application**: `writeLines(FilePath, _)` creates a function expecting one parameter
   *  - **Method Reference**: `askForUpdate` (no parentheses) passes the method as a function
   *
   * **Understanding Method Reference:**
   * {{{
   * askForUpdate
   * // This passes the method as a function value
   * // Equivalent to: () => askForUpdate()
   * // Scala automatically converts methods to functions when needed
   * }}}
   *
   * @return A MUnitVersion instance configured with real file and console I/O
   */
  def apply(): MUnitVersion =
    new MUnitVersion(
      readLines    = () => readLines(FilePath),
      askForUpdate = askForUpdate,
      writeLines   = writeLines(FilePath, _)
    )
  
  private val FilePath = Path.of("resources/protagonists.csv")

  /**
   * Logger instance for the MUnitVersion class using SLF4J.
   *
   * **Scala Features:**
   *  - **`classOf[MUnitVersion]`**: Gets the Java Class object for MUnitVersion
   *
   * **Why SLF4J?**
   *  - Industry standard for Java/Scala logging
   *  - Facade pattern: can swap logging implementations (Logback, Log4j, etc.)
   *  - Better than `println` for production code
   */
  private val log = LoggerFactory.getLogger(classOf[MUnitVersion])

  private def updateAge(p: Protagonist, n: Int): Protagonist = {
    val newAge = p.age + n
    log.info(s"The age of ${p.firstName} ${p.lastName} changes from ${p.age} to $newAge")
    p.copy(age = newAge)
  }

  private def readLines(path: Path): List[String] =
    Files.readAllLines(path).asScala.toList

  private def askForUpdate(): Int = {
    printf("By how much should I update the age? ")
    val answer = scala.io.StdIn.readLine()
    answer.toInt
  }

  private def writeLines(path: Path, lines: List[String]): Unit =
    Files.writeString(path, lines.mkString("\n"))
}
