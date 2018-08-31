package petstore

import org.scalajs.dom
import dom.document

object Frontend {

  def main(args: Array[String]): Unit = {
    setTitle("Pet Store")
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
