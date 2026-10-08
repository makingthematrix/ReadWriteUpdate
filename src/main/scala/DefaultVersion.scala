import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * DefaultVersion demonstrates a straightforward, idiomatic Scala program using basic language features.
 * This version serves as the foundation for understanding more advanced Scala concepts demonstrated
 * in other versions (CatsEffectVersion, FutureVersion, DIVersion, etc.).
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
   */
  def main(): Unit = {
    val lines        = readLines(FilePath)
    val protagonists = lines.map(Protagonist.fromLine)
    val n            = askForUpdate()
    val updated      = protagonists.map(updateAge(_, n))
    val newLines     = updated.map(_.toLine)
    writeLines(FilePath, newLines)
  }

  /**
   * The path to the CSV file containing protagonist data.
   */
  private val FilePath = Path.of("resources/protagonists.csv")

  /**
   * Reads all lines from a file as a List of Strings.
   *
   * @param path the file path to read from
   * @return a List where each element is one line from the file
   */
  private def readLines(path: Path): List[String] =
    Files.readAllLines(path).asScala.toList

  /**
   * Prompts the user for input and returns the entered integer.
   *
   * @return the integer value entered by the user
   */
  private def askForUpdate(): Int = {
    print("By how much should I update the age? ")
    val answer = scala.io.StdIn.readLine()
    answer.toInt
  }

  /**
   * Creates a new Protagonist with an updated age.
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
   * @param path the file path where data will be written
   * @param lines the list of strings to write (one per line)
   */
  private def writeLines(path: Path, lines: List[String]): Unit =
    Files.writeString(path, lines.mkString("\n"))
}
