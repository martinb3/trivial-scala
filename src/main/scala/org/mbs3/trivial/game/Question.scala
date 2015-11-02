package org.mbs3.trivial.game

class Question(val points: Float, val text: String, val answers: List[String], val explanation: String) {
  def isAnsweredBy(guess: String) = {
    answers.map { a => a.toLowerCase().trim().equals(guess.toLowerCase().trim()) }.contains(true)
  }
  
  override def toString() = text + " (" + points + ")"
}

object Question { 
  def empty() = {
    new Question(1.0f, "Is this a question?", "Yes"::Nil, null)
  }
}