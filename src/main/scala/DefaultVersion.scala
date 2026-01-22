import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * DefaultVersion demonstrates a straightforward, idiomatic Scala program using basic language features.
 * This version serves as the foundation for understanding more advanced Scala concepts demonstrated
 * in other versions (CatsEffectVersion, FutureVersion, DIVersion, etc.).
 *
 * **Key Scala Features Demonstrated:**
 *  - **Singleton Objects**: Using `object` instead of `class` to create a single instance
 *  - **Immutability**: All values are declared with `val` (immutable references)
 *  - **Higher-Order Functions**: Using `map` to transform collections
 *  - **Java Interoperability**: Using Java NIO for file operations with Scala converters
 *
 * **Program Flow:**
 *  1. Read CSV file → List of String lines
 *  2. Parse lines → List of Protagonist instances
 *  3. Ask user for age increment
 *  4. Transform protagonists by updating ages
 *  5. Serialize protagonists back to CSV format
 *  6. Write to file
 *
 * **Note for Students:**
 * This version executes operations immediately (eager evaluation) and produces side effects
 * (file I/O, console I/O, printing). Later versions will show different approaches to managing
 * these side effects, including referential transparency (CatsEffectVersion), concurrency
 * (FutureVersion), and dependency injection (DIVersion, GivenUsingVersion).
 */
object DefaultVersion {
  /**
   * Entry point that orchestrates the read-update-write workflow.
   *
   * **Scala Features in This Method:**
   *  - **`val` declarations**: Immutable local variables (lines, protagonists, etc.)
   *  - **Type inference**: Scala infers types; we could write `val lines: List[String] = ...`
   *    but it's redundant
   *  - **Underscore syntax**: `updateAge(_, n)` is shorthand for `p => updateAge(p, n)`
   *
   * **Evaluation Model:**
   * Each line executes immediately and in sequence. This is "eager" evaluation—contrast this
   * with CatsEffectVersion where operations are built into a description (IO monad) and
   * executed later.
   *
   * **Learning Path:**
   * After understanding this version, examine:
   *  - **CatsEffectVersion**: Same logic, but with referential transparency
   *  - **FutureVersion**: Same logic, but with concurrent execution
   *  - **DIVersion/GivenUsingVersion**: Same logic, but with testable abstractions
   */
  /* @main */ def main(): Unit = {
    val lines        = readLines(FilePath)
    val protagonists = lines.map(Protagonist.fromLine)
    val n            = askForUpdate()
    val updated      = protagonists.map(updateAge(_, n))
    val newLines     = updated.map(_.toLine)
    writeLines(FilePath, newLines)
  }

  /**
   * The path to the CSV file containing protagonist data.
   *
   * **Scala Features:**
   *  - **`private val`**: Immutable constant accessible only within this object
   *  - The type of the field (`Path`) is inferred from the method declaration of `Path.of`
   *  
   * **Design Note:**
   * Defining this as a constant makes it easy to change and improves code maintainability.
   * In DIVersion and GivenUsingVersion, you'll see how to inject such configuration values.
   */
  private val FilePath = Path.of("resources/protagonists.csv")

  /**
   * Reads all lines from a file as a List of Strings.
   *
   * **Scala Features:**
   *  - **Java Interoperability**: Uses Java's `Files.readAllLines()` which returns a Java List
   *  - **`.asScala`**: Converts Java collections to Scala collections (requires the import
   *    `scala.jdk.CollectionConverters.*`)
   *  - **`.toList`**: Converts to Scala's immutable `List` type
   *
   * **Why List?**
   * Scala's `List` is immutable and supports functional operations like `map`, `filter`, etc.
   *
   * @param path the file path to read from
   * @return a List where each element is one line from the file
   */
  private def readLines(path: Path): List[String] =
    Files.readAllLines(path).asScala.toList

  /**
   * Prompts the user for input and returns the entered integer.
   *
   * **Scala Features:**
   *  - **`scala.io.StdIn.readLine()`**: Scala's standard library for console input
   *  - **`.toInt`**: String method that parses to Int (throws exception if invalid)
   *
   * **Side Effects:**
   * This method has two side effects:
   *  1. Prints to console (output side effect)
   *  2. Reads from console (input side effect)
   *
   * In CatsEffectVersion, you'll see how to make this referentially transparent by wrapping
   * it in the `IO` monad. In DIVersion/GivenUsingVersion, you'll see how to abstract this
   * for testing (no real console needed in tests).
   *
   * @return the integer value entered by the user
   */
  private def askForUpdate(): Int = {
    printf("By how much should I update the age? ")
    val answer = scala.io.StdIn.readLine()
    answer.toInt
  }

  /**
   * Creates a new Protagonist with an updated age.
   *
   * **Scala Features:**
   *  - **Case Class `copy` method**: `p.copy(age = newAge)` creates a new instance with
   *    modified fields. This is key to immutability—we never mutate `p`
   *  - **String Interpolation**: `s"..."` lets us embed variables with `$variable` or
   *    expressions with `${expression}`
   *  - **Named parameters**: `copy(age = newAge)` uses the field name explicitly
   *
   * **Immutability:**
   * The original Protagonist `p` is never modified. We create a new instance instead. This is
   * fundamental to functional programming and makes reasoning about code much easier.
   *
   * **Side Effect:**
   * The `println` is a side effect that makes this function not referentially transparent.
   * Compare with CatsEffectVersion where this is wrapped in `IO`.
   *
   * @param p the original protagonist
   * @param n the amount to add to the protagonist's age
   * @return a new Protagonist instance with updated age
   */
  private def updateAge(p: Protagonist, n: Int): Protagonist = {
    val newAge = p.age + n
    println(s"The age of ${p.firstName} ${p.lastName} changes from ${p.age} to $newAge")
    p.copy(age = newAge)
  }

  /**
   * Writes a list of strings to a file, with each string as a separate line.
   *
   * **Scala Features:**
   *  - **`mkString`**: Joins list elements into a single String with a separator
   *  - **Java Interoperability**: Uses Java's `Files.writeString()`
   *
   * **Why `mkString`?**
   * `lines.mkString("\n")` converts `List("a", "b", "c")` to `"a\nb\nc"`. This is a common
   * pattern for serializing collections to text.
   *
   * **Side Effect:**
   * Writing to a file is a side effect. This method modifies external state (the file system).
   * In FutureVersion, this happens asynchronously. In CatsEffectVersion, it's wrapped in `IO`.
   *
   * @param path the file path where data will be written
   * @param lines the list of strings to write (one per line)
   */
  private def writeLines(path: Path, lines: List[String]): Unit =
    Files.writeString(path, lines.mkString("\n"))
}
