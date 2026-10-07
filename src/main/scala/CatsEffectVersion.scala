import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import cats.effect.{IO, IOApp}
import cats.syntax.all.*

/**
 * CatsEffectVersion demonstrates pure functional programming with referential transparency using Cats Effect.
 * This version shows how to manage side effects (file I/O, console I/O) while maintaining purity through
 * the IO monad and an effect system.
 *
 * If you want to read a more detailed explanation, check the file `ReferentialTransparency.md` in the main folder.
 */
object CatsEffectVersion extends IOApp.Simple {
  /**
   * The main program logic, described as a pure IO value.
   *
   * @return IO[Unit] A pure description of the program's effects
   */
  override def run: IO[Unit] =
    for {
      lines        <- readLines(FilePath)
      protagonists =  lines.map(Protagonist.fromLine)
      n            <- askForUpdate
      updated      <- protagonists.traverse(p => updateAge(p, n))
                      // `traverse` turns a collection "inside-out": `List[IO[A]]` becomes `IO[List[A]]`
      newLines     =  updated.map(_.toLine)
      _            <- writeLines(FilePath, newLines)
    } yield ()

  private val FilePath = Path.of("resources/protagonists.csv")

  /**
   * Creates an IO that describes reading all lines from a file.
   *
   * IO.blocking takes a by-name parameter (=> A). `.blocking` gives Cats Effect a hint that the operation is blocking
   * and should be executed on a separate thread.
   * The code inside { } is NOT executed now. It's captured and will be executed later.
   *
   * @param path the file path to read from
   * @return IO[List[String]] A description of reading the file
   */
  private def readLines(path: Path): IO[List[String]] =
    IO.blocking { Files.readAllLines(path).asScala.toList }

  /**
   * An IO that describes prompting the user for input and parsing it to an Int.
   *
   * @return IO[Int] A description of asking user and parsing input
   */
  private val askForUpdate: IO[Int] =
    for {
      _      <- IO.print("By how much should I update the age? ")
      answer <- IO.blocking(scala.io.StdIn.readLine())
    } yield answer.toInt

  /**
   * Creates an IO that describes updating a protagonist's age and logging the change.
   *
   * @param p the original protagonist
   * @param n the amount to add to the protagonist's age
   * @return IO[Protagonist] A description of updating and logging
   */
  private def updateAge(p: Protagonist, n: Int): IO[Protagonist] =
    for {
      newAge  =  p.age + n
      _       <- IO.println(s"The age of ${p.firstName} ${p.lastName} changes from ${p.age} to $newAge")
      updated =  p.copy(age = newAge)
    } yield updated

  /**
   * Creates an IO that describes writing strings to a file.
   *
   * @param path the file path where data will be written
   * @param lines the list of strings to write (one per line)
   * @return IO[Unit] A description of writing to the file
   */
  private def writeLines(path: Path, lines: List[String]): IO[Unit] =
    IO.blocking { Files.writeString(path, lines.mkString("\n")) }
}
