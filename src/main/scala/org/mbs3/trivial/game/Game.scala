package org.mbs3.trivial.game

import scala.collection.mutable._

class Game(val title: String, val questionList: List[Question], val scores: Map[String,Float]) {

  // val questionList = new ArrayBuffer[Question]
  // val scores = new HashMap[String,Float]

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
  def find : Game = GameStorage.fromFile("quiz8.json")
}
