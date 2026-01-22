import java.nio.file.{Files, Path}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future, Promise}
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Success, Failure}

/**
 * The `PromiseVersion` object demonstrates the use of Scala's `Future` and `Promise`
 * to handle asynchronous computations and user input. 
 *
 * **Concurrency Details:**
 *  - Uses a `Promise` to handle user input asynchronously.
 *  - Uses a `Future` for parallel reading and processing of file data.
 *  - Combines futures with `.zip` to coordinate asynchronous updates.
 *  - Awaits result completion with a timeout and handles potential errors.
 */
object PromiseVersion {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  /* @main */ def main(): Unit = {
    val nPromise = Promise[Int]()
    val pFuture = Future {
      readLines(FilePath).map(Protagonist.fromLine)
    }

    val resultFuture =
      pFuture
        .zip(nPromise.future)
        .map { (protagonists, n) =>
          val updated = protagonists.map(updateAge(_, n))
          val newLines = updated.map(_.toLine)
          writeLines(FilePath, newLines)
        }

    /* Here we delegate the promise to another place in the program. From there it can travel to even more distant
    * places. Eventually, it should be completed - we don't need to know how or where but we will get the result with
    * which it is completed as `n`, and it will be used in the logic above.*/
    askForUpdate(nPromise)

    Try(Await.result(resultFuture, 1.minute)) match {
      case Success(_)  => println(s"Program completed successfully")
      case Failure(ex) => println(s"FAILURE: ${ex.getMessage}")
    }
  }

  private val FilePath: Path = Path.of("resources/protagonists.csv")
  
  private def readLines(path: Path): List[String] =
    Files.readAllLines(path).asScala.toList

  /**
   * Asks the user for input and completes the provided Promise with the result.
   *
   * **Understanding Promise.complete:**
   * {{{
   * nPromise.complete(Try(answer.toInt))
   * }}}
   * 
   * If answer = "42": Try(answer.toInt) == Success(42)
   * - nPromise is completed with value 42
   * - nPromise.future will contain 42
   * 
   * If answer = "abc": Try(answer.toInt) == Failure(NumberFormatException)
   * - nPromise is completed with failure
   * - nPromise.future will contain the exception
   * }}}
   *
   * **Why Take Promise as Parameter?**
   * This demonstrates the "delegation" pattern:
   *  1. `main` creates the Promise
   *  2. `main` passes Promise to `askForUpdate`
   *  3. `askForUpdate` completes the Promise
   *  4. `main` uses `promise.future` without knowing when it will complete
   *
   * This separation is useful when:
   *  - The completion logic is in a different class/object
   *  - You want to test Promise completion independently
   *  - The Promise might be completed from multiple places
   *
   * @param nPromise A Promise that will be completed with the user's input value or a failure
   * @return Unit This method completes the Promise as a side effect, doesn't return a value
   */
  private def askForUpdate(nPromise: Promise[Int]): Unit = {
    printf("By how much should I update the age? ")
    val answer = scala.io.StdIn.readLine()
    nPromise.complete(Try(answer.toInt))
  }

  private def updateAge(p: Protagonist, n: Int): Protagonist = {
    val newAge = p.age + n
    println(s"The age of ${p.firstName} ${p.lastName} changes from ${p.age} to $newAge")
    p.copy(age = newAge)
  }

  private def writeLines(path: Path, lines: List[String]): Unit =
    Files.writeString(path, lines.mkString("\n"))
}
