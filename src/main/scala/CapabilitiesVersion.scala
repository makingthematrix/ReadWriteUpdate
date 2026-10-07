import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * CapabilitiesVersion demonstrates Scala 3's experimental capture checking and context functions.
 * This version uses the `?->` syntax (context functions) to require capabilities as implicit evidence,
 * representing a cutting-edge approach to dependency injection and effect tracking.
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
   * The context function takes an instance of `ReadLines` as an implicit parameter,
   * so its name doesn't need to be used in the function calling this one.
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

   * 1. `System(run)` is called
   * 2. Because `apply` is `inline`, compiler expands it to just `run`, but the expansion happens in
   *    `System`'s scope where all givens are defined
   * 3. The compiler finds all four `given` instances
   * 4. Passes them to `run` as context parameters
   * 5. `run` executes with real file/console I/O capabilities
   */
   @main  def main(): Unit = System(run)
}
