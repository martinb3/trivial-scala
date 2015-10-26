package org.mbs3.trivial.game

import scala.collection.mutable.HashSet
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

class Game(val title: String) {
  val questionList = new ArrayBuffer[Question]
  val scores = new HashMap[String,Float]
  
  var index = 0
  
  def advance : Question = {
    val currentIndex = index
    index += 1
    questionList(currentIndex)
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
  def stub : Game = {
    val g = new Game("Stub game")
    
    g.questionList.append(new Question)
    g.questionList.append(new Question)
    g.questionList.append(new Question)
    
    g
  }
}