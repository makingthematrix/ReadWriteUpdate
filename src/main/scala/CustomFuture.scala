import scala.concurrent.ExecutionContext
import scala.util.{Failure, Random, Success, Try}

/**
 * A custom implementation of a lightweight future that executes an asynchronous computation.
 * The computation is executed immediately upon instantiation using the provided execution context.
 * This is here to give you an insight of how the standard [[Future]] works.
 *
 * @tparam T The result type of the asynchronous computation.
 */
class CustomFuture[T] {
  private var value: Option[Try[T]] = None

  /* The custom future is completed when the value is defined */
  def isCompleted: Boolean = value.isDefined

  private var onCompleted: List[Try[T] => Unit] = Nil

  private def completeWith(result: Try[T]): Unit = {
    value = Some(result)
    onCompleted.foreach(f => f(result))
  }

  /**
   * Registers a callback function to be executed when the computation completes.
   * If the computation is already completed, the function is executed immediately.
   *
   * Also, note that a new callback function is prepended to the list with the `::` operator.
   * Adding a new element to a list in Scala this way is of O(1) time complexity.
   *
   * @param f A function that takes a `Try[T]` representing the result of the computation.
   *          If the computation is successful, the `Success` case contains the result.
   *          If it fails, the `Failure` case contains the exception.
   * @return Unit This method does not return a value.
   */
  def onComplete(f: Try[T] -> Unit): Unit =
    if (isCompleted)
      value.foreach(v => f(v))
    else
      onCompleted = f :: onCompleted
}

object CustomFuture {
  def apply[T](code : => T)(using ec: ExecutionContext): CustomFuture[T] = {
    val cf = new CustomFuture[T]()

    /** Immediately after creation, an instance of `CustomFuture` runs this code on the given execution context.
     * It wraps the `code` function in a `Try`, runs it, gets the result, and completes the future with it.
     * The `completeWith` function runs every function on the `onCompleted` list with the result as its parameter.
     */
    ec.execute(() => cf.completeWith(Try(code))) // can be also `new Runnable { ... }`

    cf
  }

  /**
   * The main method demonstrates the usage of a custom implementation of a future, `CustomFuture`.
   * It creates an asynchronous computation that sleeps for 1 second and then returns a number or fails.
   * A callback is registered to handle both success and failure cases of the computation.
   * The method ensures that the program runs for sufficient time to allow the computation to complete.
   */
   /*@main*/ def main(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val cf: CustomFuture[Int] = CustomFuture {
      Thread.sleep(1000L)
      val n = Random.between(0, 10)
      if (n < 5) n else throw Exception(s"The number is too big ($n)")
    }
    cf.onComplete {
      case Failure(exception) => println(s"FAILURE: $exception")
      case Success(value)     => println(s"value: $value")
    }

    Thread.sleep(2000L)
  }
}