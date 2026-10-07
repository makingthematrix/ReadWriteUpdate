import java.nio.file.Path
import com.github.tototoshi.csv.*

/**
 * LazyScalaCSVVersion demonstrates lazy evaluation and external library integration for CSV processing.
 * This version improves upon DefaultVersion by using dedicated CSV tooling and deferred computation.
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
   * @return a List of Protagonist instances parsed from the CSV file
   */
  private lazy val protagonists = CSVReader.open(FilePath.toFile).all().map(Protagonist.fromList)

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
   * @param path the file path where data will be written
   * @param updated the list of Protagonist instances to write
   */
  private def writeLines(path: Path, updated: List[Protagonist]): Unit =
    CSVWriter.open(FilePath.toFile).writeAll(updated.map(_.toList))
}
