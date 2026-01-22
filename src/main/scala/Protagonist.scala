/**
 * Represents a protagonist with a first name, last name, and age.
 *
 * **Key Scala Features Demonstrated:**
 *  - **Case Classes**: Automatically generates useful methods like `copy`, `equals`, `hashCode`, and `toString`
 *  - **Immutability**: All fields are immutable (`val` by default in case classes)
 *  - **Constructor Parameters as Fields**: Parameters automatically become public fields
 *  - **String Interpolation**: Using `s"..."` for building strings with embedded values
 *
 * **What is a Case Class?**
 * A case class is Scala's way of creating a data container with built-in functionality:
 *  - You get a `copy` method for creating modified copies (used in `updateAge`)
 *  - You get automatic `equals` and `hashCode` for value-based equality
 *  - You get a readable `toString` for debugging
 *  - Pattern matching works automatically (useful in advanced Scala)
 *
 * **Companion Object Pattern:**
 * This case class has a companion object below with factory methods (`fromLine`, `fromList`).
 * Companions share the same name and let you group related functionality together.
 *
 * @param firstName The first name of the protagonist
 * @param lastName  The last name of the protagonist
 * @param age       The age of the protagonist (must be non-negative in a real application)
 */
case class Protagonist(firstName: String, lastName: String, age: Int) {
  /**
   * Serializes this Protagonist to CSV format (comma-separated values).
   *
   * **Scala Features:**
   *  - **String Interpolation (`s"..."`**: Embeds variables directly in strings
   *  - **Direct field access**: No getters needed—`firstName`, `lastName`, `age` are accessible
   *
   * **Example:**
   * {{{
   * val p = Protagonist("Jonas", "Kahnwald", 17)
   * p.toLine  // Returns: "Jonas,Kahnwald,17"
   * }}}
   *
   * **Design Note:**
   * This method is the inverse of `Protagonist.fromLine` in the companion object.
   * Together they form a serialization/deserialization pair.
   *
   * @return a CSV-formatted string representation of this protagonist
   */
  def toLine: String = s"$firstName,$lastName,$age"

  /**
   * Converts this Protagonist to a list of strings for CSV writing.
   * **Example:**
   * {{{
   * val p = Protagonist("Jonas", "Kahnwald", 17)
   * p.toList  // Returns: List("Jonas","Kahnwald","17")
   * }}}
   *
   * @return a List containing the firstName, lastName, and age (as String)
   */
  def toList: List[String] = List(firstName, lastName, age.toString)
}

/**
 * Companion object for the Protagonist case class, providing factory methods for
 * creating Protagonist instances from different data representations.
 *
 * **What is a Companion Object?**
 * In Scala, you can define an `object` with the same name as a `class` in the same file.
 * This is called a companion object. Common uses:
 *  - Factory methods (like `fromLine` and `fromList` here)
 *  - Smart constructors (`apply`) and destructuring methods (`unapply`) (NOTE: this is not a C++-style destructor)
 *  - Constants shared across all instances
 *  - Utility methods related to the class
 *
 * **Why Companion Objects?**
 * They provide a namespace for related functionality without requiring an instance.
 * Compare with Java's static methods, but more powerful and type-safe.
 */
object Protagonist {
  /**
   * Parses a comma-separated string to create a Protagonist instance.
   *
   * **Error Handling Note:**
   * This method assumes the input is well-formed. In production code, you'd want to:
   *  - Validate the number of fields
   *  - Handle invalid integers gracefully
   *  - Return e.g. `Option[Protagonist]` or `Either[Error, Protagonist]` for safety
   *
   * **Design Pattern:**
   * This is a factory method that encapsulates the parsing logic, making it reusable
   * and testable.
   *
   * @param line A string in the format "firstName,lastName,age"
   * @return A Protagonist instance created from the parsed string
   */
  def fromLine(line: String): Protagonist = {
    val list = line.split(",").toList
    fromList(list)
  }

  /**
   * Creates a Protagonist instance from a list of strings.
   *
   * **Scala Features:**
   *  - **List indexing**: `list(0)`, `list(1)`, `list(2)` access elements by position
   *    (could be also `list.head` instead of `list(0)`)
   *  - **`.toInt`**: String method that parses to Int (throws exception if invalid)
   *
   * **Example:**
   * {{{
   * val data = List("Hannah", "Kahnwald", "47")
   * val p = Protagonist.fromList(data) // Returns: Protagonist("Hannah", "Kahnwald", 47)
   * }}}
   *
   * **Why This Method Exists:**
   * Some CSV libraries (like scala-csv in LazyScalaCSVVersion) return data as `List[String]`.
   * This method makes it easy to convert that format to Protagonist instances.
   *
   * **Error Handling Note:**
   * This assumes the list has exactly 3 elements and the third is a valid integer.
   * Production code should validate these assumptions.
   *
   * @param list A list of strings in the order [firstName, lastName, age]
   * @return A Protagonist instance created from the provided list
   */
  def fromList(list: List[String]): Protagonist =
    Protagonist(list(0), list(1), list(2).toInt)
}
