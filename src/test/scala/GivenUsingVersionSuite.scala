/**
 * A test suite for verifying the behavior of the `GivenUsingVersion` implementation and its interaction 
 * with the `run` method. It tests the complete read-update-write workflow of the program and ensures 
 * that the expected side effects (e.g., read operations, update calculations, and write operations) occur as intended.
 *
 * This test suite uses the MUnit testing framework and provides a controlled environment by specifying fixed input data 
 * (`inputLines`) and stubbing the behavior of the `GivenUsingVersion` trait within the test.
 *
 * Functionality tested includes:
 * - Ensuring the `askForUpdate` method is called exactly once.
 * - Ensuring the `writeLines` method is called exactly once.
 * - Validating that the output of the `writeLines` method matches the expected data.
 *
 * Variables tracked during tests:
 * - `asked`: Counts the number of times `askForUpdate` is invoked.
 * - `written`: Stores the value passed to `writeLines` for later validation.
 * - `writeCalls`: Counts the number of times `writeLines` is invoked.
 *
 * Note:
 * This suite relies on an implicit `GivenUsingVersion` instance being provided via the `given` keyword.
 */
class GivenUsingVersionSuite extends munit.FunSuite {

  val inputLines: List[String] = List(
    "Ada,Lovelace,36",
    "Alan,Turing,41",
    "Grace,Hopper,85"
  )

  // tracking the program
  var asked = 0
  var written: Option[List[String]] = None
  var writeCalls = 0

  given GivenUsingVersion = new GivenUsingVersion {
    override def readLines(): List[String] = inputLines

    override def askForUpdate(): Int = {
      asked += 1; 5
    }

    override def writeLines(list: List[String]): Unit = {
      writeCalls += 1; written = Some(list)
    }
  }

  test("test if read, update, write give valid results") {
    GivenUsingVersion.run

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
