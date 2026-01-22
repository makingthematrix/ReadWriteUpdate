import java.nio.file.Path

/**
 * A test suite for verifying the behavior of capabilities-based operations involving
 * reading, updating, and writing data with the `CapabilitiesVersion` module.
 *
 * The tests validate the following:
 * - The `read` capability correctly retrieves input lines simulating source data.
 * - The `update` capability processes the input data, applying transformations using
 * a provided update number, and ensures the transformation logic is correctly applied.
 * - The `write` capability properly persists the transformed data as output, ensuring
 * correctness in the written results.
 *
 * This suite uses the `munit.FunSuite` testing framework for assertions. It tracks
 * and verifies:
 * - The correct number of calls to input and output operations.
 * - Proper state handling, including ensuring the updated data matches expected results.
 *
 * The test utilizes given instances (`ReadLines`, `ReadNumber`, `Print`, and `WriteLines`) 
 * to perform dependency injection, encapsulating behaviors for reading, updating, 
 * printing, and writing operations in a mutable state for test purposes.
 */
class CapabilitiesVersionSuite extends munit.FunSuite {
  
  test("test if read, update, write give valid results") {
    import CapabilitiesVersion.*
    // tracking the program
    var asked = 0
    var written: Option[List[String]] = None
    var writeCalls = 0
    val UpdateNumber = 5
    val inputLines: List[String] = List(
      "Ada,Lovelace,36",
      "Alan,Turing,41",
      "Grace,Hopper,85"
    )
    
    object Test {
      given ReadLines  = (path: Path)                      => inputLines
      given ReadNumber = ()                                => { asked +=1; Some(UpdateNumber) }
      given Print      = (str: String)                     => ()
      given WriteLines = (path: Path, lines: List[String]) => { writeCalls += 1; written = Some(lines) }

      inline def apply(run: RunType): Unit = run
    }

    Test(run)

    // assertions
    assertEquals(asked, 1, "askForUpdate should be called exactly once")
    assertEquals(writeCalls, 1, "write should be called exactly once")

    val expected = List(
      "Ada,Lovelace,41",
      "Alan,Turing,46",
      "Grace,Hopper,90"
    )
    assertEquals(written, Some(expected))
  }
}
