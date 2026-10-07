import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import org.slf4j.LoggerFactory

/**
 * MUnitVersion demonstrates logic injection through constructor parameters to enable unit testing.
 * This version refactors DefaultVersion to separate business logic from I/O operations, making
 * the code testable without requiring actual file system or console access.
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

object MUnitVersion {
  /**
   * Entry point that creates and runs a MUnitVersion with production implementations.
   * This delegates to `apply()` which injects the real file and console I/O functions.
   */
  /* @main */ def main(): Unit = MUnitVersion().run()

  /**
   * Factory method that creates a MUnitVersion instance with production implementations.
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
