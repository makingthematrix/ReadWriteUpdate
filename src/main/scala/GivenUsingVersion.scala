import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * A trait defining the contract for I/O operations needed in the protagonist update workflow.
 */
trait GivenUsingVersion {
  def readLines(): List[String]
  def askForUpdate(): Int
  def writeLines(list: List[String]): Unit
}

/**
 * Production implementation of the GivenUsingVersion trait using real file and console I/O.
 */
class GivenUsingVersionImpl extends GivenUsingVersion {
  import GivenUsingVersion.FilePath

  override def readLines(): List[String] =
    Files.readAllLines(FilePath).asScala.toList

  override def askForUpdate(): Int = {
    printf("By how much should I update the age? ")
    val answer = scala.io.StdIn.readLine()
    answer.toInt
  }

  override def writeLines(lines: List[String]): Unit =
    Files.writeString(FilePath, lines.mkString("\n"))
}

/**
 * GivenUsingVersion demonstrates Scala 3's `given`/`using` pattern for implicit dependency injection.
 * This version shows how to use context parameters to inject dependencies automatically, combining
 * the testability of MUnitVersion with the elegance of implicit resolution.
 *
 * Key Differences from DIVersion: No MacWire. Uses Scala 3's built-in `given`/`using` instead of a framework.
 */
object GivenUsingVersion {
  /**
   * Production `given` instance of GivenUsingVersion.
   *
   * The `given` keyword declares that this value should be used as an implicit instance
   * when a method needs a `GivenUsingVersion` via a `using` parameter.
   */
  given GivenUsingVersion = new GivenUsingVersionImpl

  /**
   * Executes the read-update-write workflow using a context-provided GivenUsingVersion instance.
   * The `using` keyword declares that this method needs an implicit instance of `GivenUsingVersion`.
   * The caller doesn't need to pass it explicitly—the compiler finds it automatically.
   *
   * @param fooVersion Implicit instance providing I/O operations (resolved by compiler)
   */
  def run(using fooVersion: GivenUsingVersion): Unit = {
    import fooVersion.*
    val lines        = readLines()
    val protagonists = lines.map(Protagonist.fromLine)
    val n            = askForUpdate()
    val updated      = protagonists.map(updateAge(_, n))
    val newLines     = updated.map(_.toLine)
    writeLines(newLines)
  }

  /**
   * Entry point that runs the application with the production `given` instance.
   */
  /* @main */ def main(): Unit = run

  /**
   * The path to the CSV file containing protagonist data.
   *
   * Note: This is public (not private) because `GivenUsingVersionImpl` needs to access it.
   * Unlike MUnitVersion and DIVersion where the path is passed as a parameter, here implementations access it directly.
   */
  val FilePath: Path = Path.of("resources/protagonists.csv")

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
