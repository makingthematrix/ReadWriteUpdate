import munit.FunSuite
import java.nio.file.Path

/**
 * Test suite for the `DIVersion` class that verifies correctness of the
 * read-update-write workflow with dependency injection.
 *
 * The class extends `FunSuite` to define unit tests for validating the
 * behavior of the `DIVersion` class when interacting with I/O operations
 * provided by a `ReadUpdateWrite` implementation.
 *
 * This test suite uses a mock implementation of the `ReadUpdateWrite` trait
 * to isolate the testing of business logic and ensures that external I/O
 * dependencies behave as expected without performing actual file or user
 * interaction operations.
 */
class DIVersionSuite extends FunSuite {
  test("run increments ages by n and writes updated CSV lines") {
    // mock inputs
    val inputLines = List(
      "Ada,Lovelace,36",
      "Alan,Turing,41",
      "Grace,Hopper,85"
    )

    // tracking the program
    var asked = 0
    var written: Option[List[String]] = None
    var writeCalls = 0

    val instance = new ReadUpdateWrite {
      override def readLines(path: Path): List[String]               = inputLines
      override def askForUpdate(): Int                               = { asked += 1; 2 }
      override def writeLines(path: Path, lines: List[String]): Unit = { writeCalls += 1; written = Some(lines) }
    }
    // run
    DIVersion(instance).run()

    // assertions
    assertEquals(asked, 1, "askForUpdate should be called exactly once")
    assertEquals(writeCalls, 1, "write should be called exactly once")

    val expected = List(
      "Ada,Lovelace,38",
      "Alan,Turing,43",
      "Grace,Hopper,87"
    )
    assertEquals(written, Some(expected))
  }
}
