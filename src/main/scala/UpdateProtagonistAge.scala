import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

object UpdateProtagonistAge {
  /*@main*/ def main(): Unit = {
    val lines        = readLines(FilePath)
    val protagonists = lines.map(Protagonist.fromLine)
    val n            = askForUpdate()
    val updated      = protagonists.map(updateAge(_, n))
    val newLines     = updated.map(_.toLine)
    writeLines(FilePath, newLines)
  }

  private val FilePath = Path.of("resources/protagonists.csv")

  private def readLines(path: Path): List[String] =
    Files.readAllLines(path).asScala.toList

  private def askForUpdate(): Int = {
    print("By how much should I update the age? ")
    val answer = scala.io.StdIn.readLine()
    answer.toInt
  }

  private def updateAge(p: Protagonist, n: Int): Protagonist =
    p.copy(age = p.age + n)

  private def writeLines(path: Path, lines: List[String]): Unit =
    Files.writeString(path, lines.mkString("\n"))
}
