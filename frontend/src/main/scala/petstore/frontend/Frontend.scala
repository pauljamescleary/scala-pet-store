package petstore.frontend
import org.scalajs.dom
import org.scalajs.dom.document
import petstore.shared.models.{Pet, PetStatus}

object Frontend {

  def main(args: Array[String]): Unit = {
    setTitle("Pet Store")
    val pet = Pet("", "", "", PetStatus.Available)
    println(pet)
  }

  def addTitle(targetNode: dom.Node, title: String): Unit = {
    val titleNode = document.createElement("h1")
    val textNode = document.createTextNode(title)
    titleNode.appendChild(textNode)
    targetNode.appendChild(titleNode)
  }

  def setTitle(title: String): Unit = {
    document.getElementById("title").innerHTML = title
  }

}
