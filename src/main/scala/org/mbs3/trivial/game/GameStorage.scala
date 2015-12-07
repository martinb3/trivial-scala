package org.mbs3.trivial.game

import scala.io.Source
import scala.collection.mutable.HashMap
import org.json4s._
import org.json4s.native.JsonMethods._

object GameStorage {

  import org.json4s.DefaultFormats

  def fromFile(path: String): Game = {

    val stream = getClass.getClassLoader.getResourceAsStream(path)

    if(stream == null)
      throw new RuntimeException("Could not find json data file " + path)

    val source = Source.fromInputStream(stream)
    val lines = source.getLines mkString "\n"
    val jsonAst = parse(lines)

    implicit val formats = DefaultFormats
    val title = (jsonAst \ "title").extract[String]
    val ref = (jsonAst \ "ref").extract[String]
    val order = (jsonAst \ "order").extract[String]

    val questionJson = (jsonAst \ "questions")
    val questionList = questionJson.children.map { q =>

       val fPoints = (q \ "points")
       val points = fPoints.extractOrElse[Float](1.0f)

       val qtype = (q \ "type").extractOrElse[String]("simple")
       val text = (q \ "text").extract[String]
       val explanation = (q \ "explanation").extractOrElse[String](null)

       val fAnswer = (q \ "answer")
       val answer = fAnswer.extract[List[String]]

       new Question(qtype, points, text, answer, explanation)
    }

    val g = new Game(title, questionList, new HashMap[String,Float])
    return g
  }
}
