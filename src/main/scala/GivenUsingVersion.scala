import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * A trait defining the contract for I/O operations needed in the protagonist update workflow.
 *
 * **Key Difference from DIVersion:**
 * The methods in this trait take NO parameters (except `writeLines` which takes only the data).
 * The file path is accessed differently — implementations use `GivenUsingVersion.FilePath` directly.
 *
 * **Why This Design?**
 * No good reason. Just to show you that it's possible to do it this way :)
 * In fact, the parameterless methods and reliance on the companion object's `FilePath` make this
 * implementation slightly less flexible than DIVersion's. On the other hand, it might be a bit easier
 * to see how `given`/`using` works.
 */
trait GivenUsingVersion {
  def readLines(): List[String]
  def askForUpdate(): Int
  def writeLines(list: List[String]): Unit
}

/**
 * Production implementation of the GivenUsingVersion trait using real file and console I/O.
 *
 * **Key Scala Features:**
 *  - **Trait Implementation**: Extends the trait and overrides all abstract methods
 *  - **Accessing Companion Object**: Uses `GivenUsingVersion.FilePath` to get the file path
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
 * **Key Differences from DIVersion:**
 *  - **No MacWire**: Uses Scala 3's built-in `given`/`using` instead of a framework
 *
 * **Key Differences from DefaultVersion:**
 *  - **Dependency Injection**: I/O operations injected via `using` parameter, not hardcoded
 *  - **Testability**: Can provide test `given` instances without touching production code
 *  - **Business Logic Separation**: `run` method doesn't know about file paths or real I/O
 *
 * **Scala 3 Features Demonstrated:**
 *  - **`given` Instances**: Define implicit values with clear intent
 *  - **`using` Parameters**: Consume implicit values explicitly in method signatures
 *  - **Context Parameters**: Modern replacement for Scala 2's implicit parameters
 *  - **Import Wildcard from Parameter**: `import fooVersion.*` brings methods into scope
 *
 * **How `given`/`using` Works:**
 * {{{
 * // 1. Define a given instance (production):
 * given GivenUsingVersion = new GivenUsingVersionImpl
 * 
 * // 2. Method declares it needs the instance:
 * def run(using fooVersion: GivenUsingVersion): Unit = ...
 * 
 * // 3. Call the method WITHOUT passing the parameter:
 * run  // Scala finds the 'given' instance automatically!
 * }}}
 *
 * **Testing Strategy:**
 * {{{
 * // In tests (GivenUsingVersionSuite), define a test given:
 * given GivenUsingVersion = new GivenUsingVersion {
 *   override def readLines() = List("mock data")
 *   override def askForUpdate() = 5
 *   override def writeLines(list: List[String]) = // capture output
 * }
 * 
 * // Call run without parameters—uses test given:
 * GivenUsingVersion.run  // Automatically picks up test given!
 * }}}
 *
 * **When to Use `given`/`using`?**
 *  - **Context-like dependencies**: When the dependency is obvious from context
 *  - **Cross-cutting concerns**: Logging, database connections, execution contexts
 *  - **Test flexibility**: When you want to easily override dependencies in different scopes
 *
 * **Advantages:**
 *  - **Less boilerplate**: No need to pass dependencies through every method call
 *  - **Flexible scoping**: Different `given` instances in different scopes
 *  - **Type-safe**: Compiler verifies given instances match using parameters
 *
 * **Trade-offs:**
 *  - **Implicit behavior**: Can be less obvious where dependencies come from
 *  - **Learning curve**: Requires understanding implicit resolution rules
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
   *
   * The `using` keyword declares that this method needs an implicit instance of `GivenUsingVersion`.
   * The caller doesn't need to pass it explicitly—the compiler finds it automatically.
   *
   * **Parameter Naming Note:**
   * The parameter is named `fooVersion` in the signature, but could be any name. This name
   * is used locally within the method (via `import fooVersion.*`). You could rename it to
   * something more descriptive like `ioOps` or `dependencies`.
   *
   * **Import Wildcard from Parameter:**
   * {{{
   * import fooVersion.*
   * // This brings all methods from the trait into scope:
   * readLines()      // Instead of: fooVersion.readLines()
   * askForUpdate()   // Instead of: fooVersion.askForUpdate()
   * writeLines(...)  // Instead of: fooVersion.writeLines(...)
   * }}}
   * Once you import the methods with `import fooVersion.*`, the business logic is
   * identical to DefaultVersion, MUnitVersion, and DIVersion. This shows that the
   * core algorithm doesn't change—only how we provide the I/O operations.
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
   *
   * **How This Works:**
   * {{{
   * def main(): Unit = run
   * // Equivalent to:
   * def main(): Unit = run(using given_GivenUsingVersion)
   * // Where given_GivenUsingVersion is the given defined above
   * }}}
   *
   * **Implicit Resolution:**
   * When `run` is called without arguments, Scala's compiler:
   *  1. Sees that `run` needs a `using GivenUsingVersion` parameter
   *  2. Searches the current scope for a `given GivenUsingVersion`
   *  3. Finds `given GivenUsingVersion = new GivenUsingVersionImpl` (defined above)
   *  4. Automatically passes it to `run`
   *
   * **Why So Simple?**
   * The `given`/`using` pattern eliminates the need for factory methods or explicit
   * instance creation in the main method. The compiler handles dependency resolution.
   */
  /* @main */ def main(): Unit = run

  /**
   * The path to the CSV file containing protagonist data.
   *
   * **Note:**
   * This is public (not private) because `GivenUsingVersionImpl` needs to access it.
   * Unlike MUnitVersion and DIVersion where the path is passed as a parameter,
   * here implementations access it directly.
   */
  val FilePath: Path = Path.of("resources/protagonists.csv")

  private def updateAge(p: Protagonist, n: Int): Protagonist = {
    val newAge = p.age + n
    println(s"The age of ${p.firstName} ${p.lastName} changes from ${p.age} to $newAge")
    p.copy(age = newAge)
  }
}
