package org.mbs3.trivial.game

import scalaj.http.Http
import scala.io.Source
import scala.collection.mutable.HashMap
import scala.collection.immutable.List
import org.json4s._
import org.json4s.native.JsonMethods._
import scala.collection.mutable.ArrayBuffer
import java.util.ArrayList
import scala.util.Random
import org.mbs3.trivial.ChannelContext

object GameStorage {

  import org.json4s.DefaultFormats

  import scala.util.Try
  def list(offset: Integer): List[Game] = {
    List.empty[Game]
  }

  def search(token: String, offset: Integer): List[Game] = {
    List.empty[Game]
  }

  def fromCategories(args: String, context: ChannelContext): Game = {
    val categories : List[String] = List[String]()
    var questionList = List[Question]()
    var titles = List[String]()

    args.split(" ").foreach { s =>
      if(!s.trim().toLowerCase().equals("categories")) {
        val g = fromCategory(s, context)
        questionList = questionList ::: g.questionList
        titles = titles ::: List(g.title)
      }
    }

    questionList = Random.shuffle(questionList)
    if(questionList.length > 25) {
      questionList = questionList.take(25)
    }
    new Game(titles.mkString(", "), questionList, new HashMap[String,Float])
  }

  def fromCategory(categoryId: String, context: ChannelContext): Game = {
    var questionList = List[Question]()
    implicit val formats = DefaultFormats

      val category_response = Http("http://jservice.io/api/category?id=" + categoryId).asString.body
      val category_ast = parse(category_response)

      val title = (category_ast \ "title").extractOrElse[String]("unknown")

      if(context.globalContext.debug) {
        println("Fetched " + title + " (id=" + categoryId + ") and starting to parse it.")
      }

      val clues_ast = (category_ast \ "clues").asInstanceOf[JArray]
      clues_ast.arr.foreach { c =>
        {
          val answer = (c \ "answer").extractOrElse[String]("")
          val question = (c \ "question").extractOrElse[String]("")
          val invalid = (c \ "invalid_count").extractOrElse[Int](0)
          val points = (c \ "value").extractOrElse[Int](100) / 100.0f

          val duplicate = questionList.map { x => x.text.hashCode() }.contains(question.hashCode())

           if(
               !duplicate &&
               invalid <= 0 &&
               !answer.trim().equals("") &&
               !answer.contains(" or ") &&
               !answer.trim().contains("<i>") &&
               !question.trim().toLowerCase().contains("seen here") &&
               !question.trim().equals("")) {

             if(context.globalContext.debug) {
               println("Adding " + c)
             }
             val answers = Set(answer.split("/").toArray : _*)
             questionList = questionList ::: List(new Question("simple", points, question, answers.toList, ""))
           } else {
             if(context.globalContext.debug) {
              println("Skipping " + c)
             }
           }
        }

      }

    questionList = Random.shuffle(questionList)
    if(questionList.length > 25) {
      questionList = questionList.take(25)
    }
    val g = new Game(title, questionList, new HashMap[String,Float])
    return g
  }

  
  def fromRandom(context: ChannelContext): Game = {
    var questionList = List[Question]()

    implicit val formats = DefaultFormats
    var titles = List[String]()

    while(questionList.size < 25) {
      val random_response = Http("http://jservice.io/api/random").asString.body
      val randomAst = parse(random_response).asInstanceOf[JArray]

      val responses1 = randomAst.arr
      val category = responses1.map { x => x \ "category" }.head

      val categoryId = (category \ "id").extract[Int]
      val title = (category \ "title").extractOrElse[String]("unknown")
      if(!titles.contains(title)) {
        titles = List(title) ::: titles
      }

      val randomGame = fromCategory(categoryId.toString, context)
      randomGame.questionList.foreach { q =>
        if(questionList.size < 25) {
          questionList = questionList ::: List(q)
        }
      }
    }

    questionList = Random.shuffle(questionList)
    if(questionList.length > 25) {
      questionList = questionList.take(25)
    }
    val g = new Game(titles.mkString(", "), questionList, new HashMap[String,Float])
    return g
  }

  def fromFile(path: String, context: ChannelContext): Game = {

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
