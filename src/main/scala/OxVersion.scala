import ox.flow.*

import java.nio.file.Paths

object OxVersion {

  def main(): Unit = {
    lazy val n = askForUpdate()

    Flow
      .fromFile(Paths.get("resources/protagonists.csv"))
      .linesUtf8
      .map(Protagonist.fromLine)
      .map(protagonist => {
        val newAge = protagonist.age + n
        println(s"The new age of ${protagonist.firstName} is $newAge")
        protagonist.copy(age = newAge)
      })
      .map(_.toLine)
      .intersperse("\n")
      .encodeUtf8
      .runToFile(Paths.get("resources/protagonists.csv"))
  }

  def askForUpdate(): Int = {
    print("By how much you want to change the protagonists' age? ")
    scala.io.StdIn.readInt()
  }
}