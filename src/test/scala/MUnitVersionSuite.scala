import munit.FunSuite

/**
 * MUnitVersionSuite provides unit tests for the `MUnitVersion` class using the MUnit framework.
 *
 * The primary goal is to verify the behavior of the `run` method of `MUnitVersion` in various scenarios
 * by substituting real dependencies with mock functions for controlled testing.
 *
 * **Key Testing Scenarios:**
 * - Incrementing ages in input lines by a positive number.
 * - Handling empty input by ensuring the output is also empty.
 * - Supporting zero and negative increments in age adjustments.
 *
 * **Test Strategies:**
 * - **Mock Input and Output**: Instead of real I/O, mock functions are passed to inject necessary data.
 * - **Behavior Verification**: Ensure that injected functions are invoked with the right frequency and data.
 * - **Output Validation**: Compare the output to expected results after processing.
 */
class MUnitVersionSuite extends FunSuite {

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

    // run
    val app = new MUnitVersion(
      readLines    = ()          => inputLines,
      askForUpdate = ()          => { asked += 1; 2 },
      writeLines   = outputLines => { writeCalls += 1; written = Some(outputLines) }
    )
    app.run()

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

  test("run handles empty input by writing empty list") {
    var written: Option[List[String]] = None

    new MUnitVersion(
      readLines    = ()  => Nil,
      askForUpdate = ()  => 5,
      writeLines   = out => { written = Some(out) }
    ).run()

    assertEquals(written, Some(Nil))
  }

  test("run supports negative and zero increments") {
    val inputLines = List(
      "Aragorn,Son of Arathorn,87",
      "Frodo,Baggins,50"
    )

    // Zero increment
    var zeroOut: Option[List[String]] = None
    val appZero = new MUnitVersion(
      readLines    = ()  => inputLines,
      askForUpdate = ()  => 0,
      writeLines   = out => zeroOut = Some(out)
    )
    appZero.run()
    assertEquals(zeroOut, Some(inputLines))

    // Negative increment
    var negOut: Option[List[String]] = None
    val appNeg = new MUnitVersion(
      readLines    = ()  => inputLines,
      askForUpdate = ()  => -2,
      writeLines   = out => negOut = Some(out)
    )
    appNeg.run()
    val expectedNeg = List(
      "Aragorn,Son of Arathorn,85",
      "Frodo,Baggins,48"
    )
    assertEquals(negOut, Some(expectedNeg))
  }
}
