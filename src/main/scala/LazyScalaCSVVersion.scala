import java.nio.file.Path
import com.github.tototoshi.csv.*

/**
 * LazyScalaCSVVersion demonstrates lazy evaluation and external library integration for CSV processing.
 * This version improves upon DefaultVersion by using dedicated CSV tooling and deferred computation.
 *
 * **Key Differences from DefaultVersion:**
 *  - **Lazy Evaluation**: Uses `lazy val` to defer file reading until actually needed
 *  - **External Library**: Uses scala-csv library instead of manual parsing with Java NIO
 *
 * **Program Flow:**
 *  1. Ask user for age increment (happens first, unlike DefaultVersion)
 *  2. **If** n > 0: Read CSV file (lazy, only when needed)
 *  3. Parse to List of Protagonist instances (using scala-csv)
 *  4. Transform protagonists by updating ages
 *  5. Write back to CSV (using scala-csv)
 *
 * **Why Lazy Evaluation?**
 * If the user enters 0 or negative number, the file is never read. This demonstrates:
 *  - Performance optimization: avoid unnecessary I/O
 *  - Resource management: file only opened when needed
 *  - Separation of definition from execution
 *
 * **Why scala-csv Library?**
 *  - Handles edge cases (quotes, escaping, different delimiters)
 *  - Less boilerplate than manual string parsing
 */
object LazyScalaCSVVersion {
  /**
   * Entry point that orchestrates the read-update-write workflow with lazy evaluation.
   *
   * In DefaultVersion, the file is read immediately at the start of `main`.
   * Here, we first check if we even need to read it. This is more efficient when
   * the user might cancel or provide invalid input.
   */
  /* @main */ def main(): Unit = {
    val n = askForUpdate()
    if (n > 0) {
      val updated = protagonists.map(updateAge(_, n))
      writeLines(FilePath, updated)
    }
  }

  private val FilePath = Path.of("resources/protagonists.csv")

  /**
   * Lazily loads protagonist data from the CSV file using the scala-csv library.
   *
   * **Key Scala Feature: `lazy val`**
   * Unlike a regular `val`, a `lazy val`:
   *  1. Is NOT evaluated when defined BUT on first access
   *  2. Is cached after first evaluation (not re-computed on subsequent access)
   *  3. Is thread-safe (Scala handles synchronization)
   *
   * **How `lazy val` Works:**
   * {{{
   * // At object initialization:
   * private lazy val protagonists = ...  // Not evaluated yet!
   * 
   * // Later, when accessed:
   * val updated = protagonists.map(...)  // NOW it evaluates:
   *                                       // 1. Opens file
   *                                       // 2. Reads all rows
   *                                       // 3. Maps to Protagonist instances
   *                                       // 4. Caches result
   * 
   * // Next access:
   * protagonists.foreach(...)  // Uses cached result, no file I/O
   * }}}
   *
   * **Using scala-csv Library:**
   *  - **`CSVReader.open(FilePath.toFile)`**: Creates a CSV reader for the file
   *  - **`.all()`**: Reads all rows as `List[List[String]]` (list of rows, each row is list of fields)
   *  - **`.map(Protagonist.fromList)`**: Converts each `List[String]` to a `Protagonist`
   *
   * **Type Inference Note:**
   * The type `List[Protagonist]` is inferred automatically. We could write:
   * {{{
   * private lazy val protagonists: List[Protagonist] = ...
   * }}}
   * but it's redundant.
   *
   * @return a List of Protagonist instances parsed from the CSV file
   */
  private lazy val protagonists =
    CSVReader.open(FilePath.toFile)
      .all()
      .map(Protagonist.fromList)

  private def askForUpdate(): Int = {
    printf("By how much should I update the age? ")
    val answer = scala.io.StdIn.readLine()
    answer.toInt
  }

  private def updateAge(p: Protagonist, n: Int): Protagonist = {
    val newAge = p.age + n
    println(s"The age of ${p.firstName} ${p.lastName} changes from ${p.age} to $newAge")
    p.copy(age = newAge)
  }

  /**
   * Writes protagonist data back to the CSV file using the scala-csv library.
   *
   * **Key Difference from DefaultVersion:**
   *  - Uses **scala-csv's CSVWriter** instead of Java NIO
   *  - Works with `List[Protagonist]` directly, not `List[String]`
   *  - Uses `Protagonist.toList` instead of `Protagonist.toLine`
   *
   * **Using scala-csv Library:**
   *  - **`CSVWriter.open(FilePath.toFile)`**: Creates a CSV writer for the file
   *  - **`.writeAll(data)`**: Expects `List[List[String]]` (list of rows)
   *  - **`updated.map(_.toList)`**: Converts each Protagonist to `List[String]`
   *
   * **Method Chaining Note:**
   * We could write this in two steps:
   * {{{
   * val writer = CSVWriter.open(FilePath.toFile)
   * writer.writeAll(updated.map(_.toList))
   * }}}
   * But chaining is more concise and idiomatic in Scala.
   *
   * @param path the file path where data will be written
   * @param updated the list of Protagonist instances to write
   */
  private def writeLines(path: Path, updated: List[Protagonist]): Unit =
    CSVWriter.open(FilePath.toFile).writeAll(updated.map(_.toList))
}
