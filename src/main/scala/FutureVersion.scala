import java.nio.file.{Files, Path}
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/**
 * Common utility object providing shared helper methods for Future-based versions.
 */
object FutureVersion {
  val FilePath: Path = Path.of("resources/protagonists.csv")

  def readLines(path: Path): List[String] =
    Files.readAllLines(path).asScala.toList

  def askForUpdate(): Int = {
    printf("By how much should I update the age? ")
    val answer = scala.io.StdIn.readLine()
    answer.toInt
  }

  def updateAge(p: Protagonist, n: Int): Protagonist = {
    val newAge = p.age + n
    println(s"The age of ${p.firstName} ${p.lastName} changes from ${p.age} to $newAge")
    p.copy(age = newAge)
  }

  def writeLines(path: Path, lines: List[String]): Unit =
    Files.writeString(path, lines.mkString("\n"))
}

/**
 * This is a version of the program that reads, updates, and writes protagonist
 * data using asynchronous computations with `Future` and blocking operation with `Await`.
 *
 * **Key Differences from DefaultVersion:**
 *  - **Parallel Execution**: File reading and user input happen simultaneously
 *  - **Blocking Wait**: Uses `Await.result` to block until Futures complete
 *  - **Error Handling**: Uses `Try` to catch timeouts and failures
 *
 * **Key Scala Features Demonstrated:**
 *  - **Future**: Represents a value that will be available in the future
 *  - **ExecutionContext**: Thread pool that executes Future tasks
 *  - **Future.zip**: Combines two Futures into one Future of a tuple
 *  - **Await.result**: Blocks until Future completes (or times out)
 *  - **Pattern Matching on Try**: Handle Success/Failure cases
 *
 * **What is a Future?**
 * A `Future[T]` represents a computation that will produce a value of type `T` at some point.
 * When you create a Future:
 *  1. The computation starts running on a background thread (from ExecutionContext)
 *  2. The main thread continues without waiting
 *  3. You can later get the result (blocking with Await or non-blocking with callbacks)
 */
object FutureVersionWithAwait {
  import FutureVersion.*

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
   * The main entry point for the program that asynchronously reads, updates, and writes protagonist data.
   *
   * The program makes use of `Future` for asynchronous computations and `Await` for blocking the main thread
   * until the asynchronous operations are complete. Error handling is managed using a `Try` block to handle
   * possible failures, such as timeouts or file I/O issues.
   */
  /*@main */def main(): Unit = {
    val pFuture = Future { readLines(FilePath).map(Protagonist.fromLine) }
    val nFuture = Future { askForUpdate() }

    val resultFuture =
      pFuture
        .zip(nFuture)
        .map { (protagonists, n) =>
          val updated  = protagonists.map(updateAge(_, n))
          val newLines = updated.map(_.toLine)
          writeLines(FilePath, newLines)
        }

    Try(Await.result(resultFuture, 1.minute)) match {
       case Success(_)  => println(s"Program completed successfully")
       case Failure(ex) => println(s"FAILURE: ${ex.getMessage}")
    }
  }
}

/**
 * FutureVersionWithThreadPool demonstrates Future-based concurrency with a custom thread pool.
 * In contrast to the global execution context, when a program using a custom thread pool reaches its end, it doesn't
 * terminate immediately, but wait for the running futures to complete. For us, it means we don't need to use [[Await]].
 * Instead, we can simply use the `.onComplete` method to register a function that will be executed when `resultFuture`
 * completes. There can be any number of `.onComplete` calls, each one registering a different function.
 * Look to [[CustomFuture]] to see how it works underneath.
 *
 * Another change is that `resultFuture` is created using the for/yield syntax. Compare it with the first version.
 * for/yield allows for a more readable chaining of operations but there is a catch: It is important that `nFuture`
 * is not inlined in for/yield but starts before it. for/yield waits at the first line until pFuture is completed,
 * and only then moves to the second line. If `nFuture` was inlined at the second line, the whole logic of asking
 * the user for an update would be executed only at that point, which would make the program sequential again.
 */
object FutureVersionWithThreadPool {
  private val threadPool = Executors.newFixedThreadPool(2)
  given ExecutionContext = ExecutionContext.fromExecutor(threadPool)
  import FutureVersion.*

  /*@main */def main(): Unit = {
    val pFuture = Future { readLines(FilePath).map(Protagonist.fromLine) }
    val nFuture = Future { askForUpdate() }

    val resultFuture = for {
      protagonists <- pFuture
      n            <- nFuture
      updated      =  protagonists.map(updateAge(_, n))
      newLines     =  updated.map(_.toLine)
    } yield writeLines(FilePath, newLines)

    resultFuture.onComplete {
      case Success(_)  => println(s"Program completed successfully")
      case Failure(ex) => println(s"FAILURE: ${ex.getMessage}")
    }
    resultFuture.onComplete(_ => threadPool.shutdown())
  }
}
