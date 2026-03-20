import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

val FilePath = Path.of("/home/makingthematrix/workspace/ReadWriteUpdate/resources/protagonists.csv")
def readLines(path: Path): List[String] = Files.readAllLines(path).asScala.toList
def writeLines(path: Path, lines: List[String]): Unit = Files.writeString(path, lines.mkString("\n"))
def updateAge(p: Protagonist, n: Int): Protagonist = {
  val newAge = p.age + n
  println(s"The age of ${p.firstName} ${p.lastName} changes from ${p.age} to $newAge")
  p.copy(age = newAge)
}

val lines = readLines(FilePath)
val protagonists = lines.map(Protagonist.fromLine)
val n = 12
val updated = protagonists.map(updateAge(_, n))
val newLines = updated.map(_.toLine)
writeLines(FilePath, newLines)