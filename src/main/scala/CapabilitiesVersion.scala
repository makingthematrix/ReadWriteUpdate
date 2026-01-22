import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * CapabilitiesVersion demonstrates Scala 3's experimental capture checking and context functions.
 * This version uses the `?->` syntax (context functions) to require capabilities as implicit evidence,
 * representing a cutting-edge approach to dependency injection and effect tracking.
 *
 * **Key Differences from DefaultVersion:**
 *  - **Context Functions (`?->`)**: Methods return functions that require capabilities as context
 *  - **Capability-Based Security**: Each operation explicitly declares what capabilities it needs
 *  - **Compile-Time Effect Tracking**: The type system tracks which effects each function requires
 *  - **Granular Permissions**: Instead of one big trait, separate capabilities for each operation
 *
 * **Key Differences from GivenUsingVersion:**
 *  - **Multiple Context Parameters**: Can require multiple capabilities in one function
 *  - **Context Functions vs. Using Parameters**: `?->` instead of `using` parameters
 *  - **Function-Level Granularity**: Each helper function declares its own needed capabilities
 *  - **No Trait Grouping**: Capabilities are separate traits, not one umbrella trait
 *
 * **What is a Context Function?**
 * {{{
 * // Regular function:
 * def readLines(path: Path): List[String] = ...
 * 
 * // Context function (this version):
 * def readLines(path: Path): ReadLines ?-> List[String] = { r ?=> r.readLines(path) }
 * //                          ^^^^^^^^^^^^^^^^^^^^^^^^
 * //                          This is a context function type
 * //                          Means: "returns a function that needs ReadLines capability
 * }}}
 *
 * Instead of one big interface with all methods (like GivenUsingVersion's trait), we have:
 *  - **ReadLines**: Capability to read from files
 *  - **ReadNumber**: Capability to read user input
 *  - **Print**: Capability to print to console
 *  - **WriteLines**: Capability to write to files
 *
 * Each function declares exactly which capabilities it needs.
 *
 * **Why Separate Capabilities?**
 *  - **Principle of Least Privilege**: Functions only get the capabilities they actually need
 *  - **Clearer Dependencies**: Type signatures show exactly what effects are performed
 *  - **Easier Testing**: Mock only the capabilities a specific function uses
 *
 * **Trade-offs:**
 *  - **Verbose types**: Function signatures can get long with many capabilities
 *  - **Learning curve**: Most complex DI pattern in this codebase
 */
object CapabilitiesVersion {
  private val FilePath = Path.of("resources/protagonists.csv")

  /**
   * Capability trait for reading lines from a file.
   */
  trait ReadLines {
    def readLines(path: Path): List[String]
  }

  /**
   * Context function wrapper for reading lines.
   *
   * **Why Wrap It?**
   * The context function takes an instance of `ReadLines` as an implicit parameter, so its name doesn't need to be
   * be used in the function calling this one.
   *
   * @param path the file path to read from
   * @return a context function that needs ReadLines capability to produce List[String]
   */
  inline private def readLines(path: Path): ReadLines ?-> List[String] = { r ?=> r.readLines(path) }

  /**
   * Capability trait for reading numeric input from the user.
   */
  trait ReadNumber {
    def readNumber(): Option[Int]
  }

  /**
   * Context function wrapper for reading a number.
   * NOTE: Functions that do not have regular parameters can be declared as `val` but
   * they cannot be `inline`. An inline value must contain a literal constant value.
   *
   * @return a context function that needs ReadNumber capability to produce Option[Int]
   */
  private val readNumber: ReadNumber ?-> Option[Int] = { r ?=> r.readNumber() }

  /**
   * Capability trait for printing to console.
   */
  trait Print {
    def printLine(str: String): Unit
  }

  /**
   * Context function wrapper for printing.
   *
   * @param str the string to print
   * @return a context function that needs Print capability
   */
  inline private def printLine(str: String): Print ?-> Unit = { p ?=> p.printLine(str) }

  /**
   * Capability trait for writing lines to a file.
   */
  trait WriteLines {
    def writeLines(path: Path, lines: List[String]): Unit
  }

  /**
   * Context function wrapper for writing lines.
   *
   * @param path the file path where data will be written
   * @param lines the list of strings to write
   * @return a context function that needs WriteLines capability
   */
  inline private def writeLines(path: Path, lines: List[String]): WriteLines ?-> Unit = { w ?=> w.writeLines(path, lines) }

  /**
   * Prompts the user for an age update value with error handling.
   *
   * **Multiple Capabilities Required:**
   * {{{
   * (Print, ReadNumber) ?-> Int
   * // Reads as: "Given Print AND ReadNumber capabilities, produce an Int"
   * }}}
   *
   * **Why Two Capabilities?**
   * This function needs to:
   *  1. Print a prompt (needs `Print`)
   *  2. Read user input (needs `ReadNumber`)
   *  3. Handle invalid input by printing error (needs `Print` again)
   *
   * @return a context function requiring Print and ReadNumber capabilities
   */
  private val askForUpdate: (Print, ReadNumber) ?-> Int = {
    printLine("By how much should I update the age? ")
    readNumber.getOrElse{ printLine("Invalid input"); 0 }
  }

  /**
   * Creates a new Protagonist with an updated age and logs the change.
   *  - Takes a protagonist and a number as explicit parameters (no capability needed)
   *  - Prints a log message (needs `Print`)
   *  - Returns a new protagonist (no capability needed)
   *
   * It doesn't need file access or user input, so it doesn't require those capabilities.
   *
   * @param p the original protagonist
   * @param n the amount to add to the protagonist's age
   * @return a context function requiring Print capability to produce updated Protagonist
   */
  private def updateAge(p: Protagonist, n: Int): Print ?-> Protagonist = {
    val newAge = p.age + n
    printLine(s"The age of ${p.firstName} ${p.lastName} changes from ${p.age} to $newAge\n")
    p.copy(age = newAge)
  }

  /**
   * Type alias for the main program's capability requirements.
   * The type list is quite long, so we give it a short name. This makes the code more readable.
   */
  type RunType = (ReadLines, ReadNumber, Print, WriteLines) ?-> Unit

  /**
   * The main program logic orchestrating the read-update-write workflow.
   *
   * **Declared Capabilities:**
   * This `val` has type `RunType`, which expands to:
   * {{{
   * (ReadLines, ReadNumber, Print, WriteLines) ?-> Unit
   * }}}
   * This means when `run` is invoked, the caller must provide all four capabilities.

   * **Capability Propagation:**
   * The business logic is identical - only the capability tracking has been added.
   * But since each called function takes the capabilities implicitly, there is no need to given them names.
   * The Scala compile will figure out which capability provided implicitly to `run` should be passed implicitly
   * to which subsequent function call.
   */
  val run: RunType = {
    val lines        = readLines(FilePath)
    val protagonists = lines.map(Protagonist.fromLine)
    val n            = askForUpdate
    val updated      = protagonists.map(updateAge(_, n))
    val newLines     = updated.map(_.toLine)
    writeLines(FilePath, newLines)
  }

  /**
   * Object providing production implementations of all required capabilities.
   *
   * **Four Given Instances:**
   * Each `given` provides an implementation of one capability trait:
   *  - **ReadLines**: Lambda that reads from file using Java NIO
   *  - **ReadNumber**: Lambda that reads from console and parses to Option[Int]
   *  - **Print**: Lambda that prints to console using printf
   *  - **WriteLines**: Lambda that writes to file using Java NIO
   *
   * **Lambda Implementations:**
   * {{{
   * given ReadLines = (path: Path) => Files.readAllLines(path).asScala.toList
   * // This is shorthand for:
   * given ReadLines = new ReadLines {
   *   override def readLines(path: Path): List[String] =
   *     Files.readAllLines(path).asScala.toList
   * }
   * }}}
   *
   * Scala allows this concise syntax when the trait has a single abstract method (SAM).
   */
  private object System {
    given ReadLines  = (path: Path)                      => Files.readAllLines(path).asScala.toList
    given ReadNumber = ()                                => scala.io.StdIn.readLine().toIntOption
    given Print      = (str: String)                     => printf(str)
    given WriteLines = (path: Path, lines: List[String]) => Files.writeString(path, lines.mkString("\n"))

    inline def apply(run: RunType): Unit = run
  }

  /**
   * Entry point that runs the application with production capabilities.
   *
   * **Capability Resolution:**
   * 1. `System(run)` is called
   * 2. Because `apply` is `inline`, compiler expands it to just `run`, but the expansion happens in
   *    `System`'s scope where all givens are defined
   * 3. The compiler finds all four `given` instances
   * 4. Passes them to `run` as context parameters
   * 5. `run` executes with real file/console I/O capabilities
   */
  /* @main */ def main(): Unit = System(run)
}
