import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import cats.effect.{IO, IOApp}
import cats.syntax.all.*

/**
 * CatsEffectVersion demonstrates pure functional programming with referential transparency using Cats Effect.
 * This version shows how to manage side effects (file I/O, console I/O) while maintaining purity through
 * the IO monad and an effect system.
 *
 * Here's an explanation how this version works. If you want to read a more detailed version of it, check the Markdown
 * file `ReferentialTransparency.md` in the main folder.
 *
 * **Key Differences from DefaultVersion:**
 *  - **Referential Transparency**: All effectful operations wrapped in `IO` monad
 *  - **Deferred Execution**: Operations described, not executed, until `run` is called by Cats Effect
 *  - **Pure Functions**: The `run` method is pure—it returns a description of effects, doesn't perform them
 *  - **Effect System**: Cats Effect framework manages execution, concurrency, and resource safety
 *
 * **What is Referential Transparency?**
 * A function is referentially transparent if you can replace a function call with its return value
 * without changing program behavior.
 *
 * {{{
 * // Pure, referentially transparent:
 * def add(a: Int, b: Int): Int = a + b
 * val x = add(2, 3)        // Returns 5
 * val y = add(2, 3)        // Can replace with literal 5
 * 
 * // Impure, NOT referentially transparent:
 * def readFile(path: String): List[String] = Files.readAllLines(path).asScala.toList
 * val lines1 = readFile("data.txt")  // Reads file now
 * val lines2 = readFile("data.txt")  // Reads file again (file might have changed!)
 * // We can't replace call with a literal value—behavior depends on external state
 * }}}
 *
 * **What is the IO Monad?**
 * `IO[A]` is a data structure that represents a computation that:
 *  - When executed, will produce a value of type `A`
 *  - May perform side effects (I/O, printing, etc.)
 *  - Is itself a pure, immutable value
 *
 * Think of `IO[A]` as a "recipe" or "blueprint" for an effectful computation:
 * {{{
 * // Creating the recipe (pure):
 * val recipe: IO[List[String]] = IO { Files.readAllLines(path).asScala.toList }
 * // File not read yet! We just described how to read it.
 * 
 * // Executing the recipe (impure, done by Cats Effect):
 * val lines: List[String] = recipe.execute()
 * // NOW file is read
 * }}}
 *
 * **IO is a Monad:**
 * Like `Option`, `Either`, and `Try`, you can compose IO values with `map`, `flatMap`, and `for`:
 * {{{
 * val io1: IO[String] = IO("hello")
 * val io2: IO[String] = io1.map(_.toUpperCase)  // IO("HELLO")
 * val io3: IO[Int] = io2.map(_.length)          // IO(5)
 * 
 * // Nothing executed yet—we just built a chain of descriptions
 * // Only when Cats Effect runs it do effects occur
 * }}}
 *
 * **Effect System (Cats Effect):**
 * Cats Effect is a framework that:
 *  - Takes your IO descriptions
 *  - Executes them safely
 *  - Manages concurrency and parallelism
 *  - Handles resource cleanup
 *  - Provides cancellation and error handling
 *
 * You describe WHAT you want to do (pure), Cats Effect decides HOW to do it (impure).
 *
 * **When to Use Cats Effect?**
 *  - **Production applications**: Better error handling, resource safety, concurrency
 *  - **Testability**: Pure functions easier to test
 *  - **Composability**: Build complex programs from simple pieces
 *  - **Concurrent systems**: Cats Effect excels at async/concurrent code
 *
 * **Trade-offs:**
 *  - **Learning curve**: Requires understanding monads and effect systems
 *  - **Boilerplate**: More verbose than direct side effects (for simple programs)
 *
 * **Why IOApp.Simple?**
 * `IOApp.Simple` is a Cats Effect trait that:
 *  - Provides the entry point for your program
 *  - Requires you to implement `run: IO[Unit]`
 *  - Handles execution, error reporting, and JVM shutdown
 *  - Manages the runtime system for you
 */
object CatsEffectVersion extends IOApp.Simple {
  /**
   * The main program logic, described as a pure IO value.
   *
   * **Key Insight: This Method is Pure!**
   * Despite containing file I/O, user input, and printing, this method is referentially transparent.
   * It doesn't PERFORM effects - it only DESCRIBES them.
   *
   * @return IO[Unit] A pure description of the program's effects
   */
  override def run: IO[Unit] =
    for {
      lines        <- readLines(FilePath)
      protagonists =  lines.map(Protagonist.fromLine)
      n            <- askForUpdate
      updated      <- protagonists.traverse(updateAge(_, n))
                      // `traverse` turns a collection "inside-out": `List[IO[A]]` becomes `IO[List[A]]`
      newLines     =  updated.map(_.toLine)
      _            <- writeLines(FilePath, newLines)
    } yield ()

  private val FilePath = Path.of("resources/protagonists.csv")

  /**
   * Creates an IO that describes reading all lines from a file.
   *
   * IO constructor takes a by-name parameter (=> A). The code inside { } is NOT executed now.
   * It's captured and will be executed later
   *
   * @param path the file path to read from
   * @return IO[List[String]] A description of reading the file
   */
  private def readLines(path: Path): IO[List[String]] =
    IO { Files.readAllLines(path).asScala.toList }

  /**
   * An IO that describes prompting the user for input and parsing it to an Int.
   *
   * @return IO[Int] A description of asking user and parsing input
   */
  private val askForUpdate: IO[Int] =
    for {
      _      <- IO.print("By how much should I update the age? ")
      answer <- IO(scala.io.StdIn.readLine())
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
    IO { Files.writeString(path, lines.mkString("\n")) }
}
