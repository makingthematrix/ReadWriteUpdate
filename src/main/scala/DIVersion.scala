import com.softwaremill.macwire.wire

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * A trait defining the contract for I/O operations needed in the protagonist update workflow.
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
 * It's compile-time logic that eliminates boilerplate instantiation code.
 *
 * @param instance The ReadUpdateWrite implementation providing I/O operations
 */
class DIVersion(instance: ReadUpdateWrite) {
  import DIVersion.{FilePath, updateAge}
  import instance.*

  /**
   * Executes the read-update-write workflow using injected dependencies.
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
 */
object DIVersion {
  /**
   * Entry point that runs the application with MacWire-injected dependencies.
   */
  def main(): Unit = diVersion.run()

  /**
   * MacWire-instantiated production implementation of ReadUpdateWrite.
   */
  private lazy val instance: ReadUpdateWrite = wire[ReadUpdateWriteImpl]

  /**
   * MacWire-instantiated DIVersion with automatically resolved dependencies.
   */
  private lazy val diVersion: DIVersion = wire[DIVersion]

  /**
   * The path to the CSV file containing protagonist data.
   */
  private val FilePath = Path.of("resources/protagonists.csv")

  /**
   * Same as in the DefaultVersion - creates a new Protagonist with an updated age.
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
}
