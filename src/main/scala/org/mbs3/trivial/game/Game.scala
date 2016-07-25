package org.mbs3.trivial.game

import scala.collection.mutable._
import org.mbs3.trivial.ChannelContext

class Game(val title: String, val questionList: List[Question], val scores: Map[String,Float]) {

  var index = -1

  def advance : Question = {
    index += 1
    questionList(index)
  }

  def currentQuestion: Option[Question] = {
    if(index < questionList.length) {
      return Some(questionList(index))
    }
    else {
      return None
    }
  }
  def questionsAvailable : Boolean = { index+1 < questionList.length }
}

object Game {
  def find(token: String, full_message: String, context: ChannelContext) : Game = {
    if(token.startsWith("categories")) {
      
      val str_match = full_message.split("\\W").toArray
      val categories = str_match.slice(2, str_match.length).mkString(" ")
      
      return GameStorage.fromCategories(categories, context)
    }
    else if(token.equals("random")) { 
      return GameStorage.fromRandom(context)
    }
    else {
      return GameStorage.fromFile(token+".json", context)
    }
  }
  def empty() : Game = {
    val questionList : List[Question] = Nil
    val scores = new HashMap[String,Float]
    new Game("", questionList, scores)
  }
}
