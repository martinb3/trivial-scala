package org.mbs3.trivial.game


import scala.io.Source
import scala.collection.mutable.HashMap
import scala.collection.immutable.List
import org.json4s._
import org.json4s.native.JsonMethods._
import scala.collection.mutable.ArrayBuffer
import java.util.ArrayList
import scala.util.Random

object GameStorage {

  import org.json4s.DefaultFormats

  import scala.util.Try
  def list(offset: Integer): List[Game] = {
    List.empty[Game]
  }

  def search(token: String, offset: Integer): List[Game] = {
    List.empty[Game]
  }

  def fromUrl(url: String): Game = {
    new Game("unknown", List.empty[Question], new HashMap[String,Float])
  }

  def fromFile(path: String): Game = {

    val stream = getClass.getClassLoader.getResourceAsStream(path)

    if(stream == null)
      throw new RuntimeException("Could not find json data file " + path)

    val source = Source.fromInputStream(stream)
    val lines = source.getLines mkString "\n"
    val jsonAst = parse(lines)

    implicit val formats = DefaultFormats
    val title = (jsonAst \ "title").extractOrElse[String]("unknown")
    val ref = (jsonAst \ "ref").extractOrElse[String]("")
    val order = (jsonAst \ "order").extractOrElse[String]("")
    val subset = (jsonAst \ "subset").extractOrElse[String]("")

    val questionJson = (jsonAst \ "questions")
    var questionList = questionJson.children.map { q =>

       val fPoints = (q \ "points")
       val points = fPoints.extractOrElse[Float](1.0f)

       val qtype = (q \ "type").extractOrElse[String]("simple")
       val text = (q \ "text").extract[String]
       val explanation = (q \ "explanation").extractOrElse[String](null)

       val fAnswer = (q \ "answer")
       val answer = fAnswer.extract[List[String]]

       new Question(qtype, points, text, answer, explanation)
    }
    
    if(order == "rand") {
      questionList = Random.shuffle(questionList)
    }
    
    // handle subset
    if (subset != "") {
      try {
        val s = Integer.parseInt(subset)
        if(questionList.length > s) {
          questionList = questionList.take(s)
        }
      } catch { 
        case _ : Throwable => ; 
      }
    }

    val g = new Game(title, questionList, new HashMap[String,Float])
    return g
  }
}
