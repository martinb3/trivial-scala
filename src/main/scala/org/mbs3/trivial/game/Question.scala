package org.mbs3.trivial.game

import org.mbs3.trivial.ChannelContext
import org.apache.commons.lang3.StringUtils

class Question(val qtype: String, val points: Float, val text: String, val answers: List[String], val explanation: String) {
  def isAnsweredBy(guess: String, context: ChannelContext) = {
    answers.map { a =>
       if(true || context.globalContext.debug) {
         println("Check [" + cleanString(guess) + "] contains [" + cleanString(a) + "]")
       }
       cleanString(guess).contains(cleanString(a))
    }.contains(true)
  }

  def possibleAnswers() : String = {
    answers.distinct.mkString("\n")
  }

  override def toString() = text + " (" + points + ")"

  def cleanString(s: String) = {
    var result = ""
    if(s.endsWith("s")) {
      result = s.substring(0, s.length - 1)
    }
    else {
      result = s
    }

    result = StringUtils.stripAccents(result)

    result // fix accents
      .toLowerCase()
      .replaceAll("\\(.*\\)", "")
      .replaceAll("[^A-Za-z0-9 ]", "")
      .replaceAll("^an ", "")
      .replaceAll("^a ", "")
      .replaceAll("^the ", "")
      .replaceAll("^his ", "")
      .replaceAll("^her ", "")
      .replaceAll("^its ", "")
      .replaceAll(" and ", "")
      .replaceAll("zero", "0")
      .replaceAll("one", "1")
      .replaceAll("two", "2")
      .replaceAll("three", "3")
      .replaceAll("four", "4")
      .replaceAll("five", "5")
      .replaceAll("six", "6")
      .replaceAll("seven", "7")
      .replaceAll("eight", "8")
      .replaceAll("nine", "9")
      .replaceAll(" +", " ")
      .replaceAll(" ", "")
      .replace("s$", "")
      .trim()
  }
}

object Question {
  def empty() = {
    new Question("simple", 1.0f, "Is this a question?", "Yes"::Nil, null)
  }
}
