package org.mbs3.trivial.game

import slack.rtm.SlackRtmClient
import slack.models.Message
import slack.api.BlockingSlackApiClient
import org.mbs3.trivial.ChannelState
import org.mbs3.trivial.ChannelState._
import org.mbs3.trivial.GlobalContext
import org.mbs3.trivial.ChannelContext
import org.mbs3.trivial.commands.Command
import org.mbs3.trivial.commands.ScoreCommand
import org.mbs3.trivial.MessageFormatter.msg

class ChannelManager(val channelContext: ChannelContext) {

  def client = channelContext.globalContext.client
  def debug = channelContext.globalContext.debug
  def gCtx = channelContext.globalContext
  
  def channelId = channelContext.channelId
  def game = channelContext.game
  def currentState = channelContext.currentState
  def changeState(s: ChannelState) = channelContext.changeState(s)
  def secondsSinceLastStateChange = channelContext.secondsSinceLastStateChange
  def isInGame = channelContext.isInGame
  def timing = channelContext.timing
    
  def tick {
    val oldState = currentState
    val newState = currentState match {
      case ChannelState.Initial => ChannelState.Initial
      case ChannelState.Emcee => ChannelState.Emcee
      case ChannelState.New => handleNewGame
      case ChannelState.PoseQuestion1 => handlePoseQuestion1
      case ChannelState.PoseWait => handlePoseWait
      case ChannelState.PoseQuestion2 => handlePoseQuestion2
      case ChannelState.AnswerWait => handleAnswerWait
      case ChannelState.NoAnswerTimeout => handleNoAnswerTimeout
      case ChannelState.QuestionWait => handleQuestionWait
      case ChannelState.FinalScore => handleFinalScore
      case ChannelState.Over => handleGameOver
    }
    changeState(newState)
  }

  // this can receive two things, commands or guesses
  def message(message: Message) {
    
    if(debug) {
      val user = client.state.getUserById(message.user).get
      val to_user = message.channel
      println("Message received for ChannelManager by user "+
              user.name+" on " + channelId + ": [" + message.text+"]")
    }
    
    
    val valid_commands = Command.list
        .filter  { _.accept(message, channelContext) }
        .foreach { _.handle(message, channelContext) }
    
    if(currentState == ChannelState.AnswerWait) {
      handleGuess(message)
    }

  }

  // someone has guessed while we're in AnswerWait
  def handleGuess(message: Message) {
    if(game.currentQuestion.isEmpty)
      return

    val q = game.currentQuestion.get
    if(!q.isAnsweredBy(message.text))
      return

    val user = client.state.getUserById(message.user).get
    if(q.explanation != null)
      client.sendMessage(channelId, msg("CORRECT_ANSWER_EXPLANATION", user.id, user.name, q.explanation))
    else
      client.sendMessage(channelId, msg("CORRECT_ANSWER", user.id, user.name))

    val scores = game.scores
    var points = 0f

    val sanitized_name = user.name.trim().toLowerCase()
    if(scores.contains(sanitized_name))
      points += scores.get(sanitized_name).get

    scores.put(sanitized_name, points+q.points)
    changeState(QuestionWait)
  }

  def handleNewGame(): ChannelState = {
    game.scores.clear
    if(debug) {
      return PoseQuestion1
    }

    if(secondsSinceLastStateChange > timing("cutoff")) {
      client.sendMessage(channelId, msg("NEW_GAME", game.title))
      return PoseQuestion1
    }
    else if(secondsSinceLastStateChange % timing("modamount") == 0) {
      client.sendMessage(channelId, msg("NEW_GAME2", game.title))
    }
    return New
  }

  def handlePoseQuestion1() : ChannelState = {
    if(game.questionsAvailable) {
      val q = game.advance
      val qText = q.text; val qPoints = q.points

      if(debug) {
        println(q.text + "/" + q.points)
      }

      if(q.qtype == "simple" || q.qtype == "image") {
        client.sendMessage(channelId, msg("QUESTION_POSE", q.points).trim())
      }
      else {
        client.sendMessage(channelId, "Someone entered an unknown question type " + q.qtype)
      }

      return PoseWait
    }
    else {
      return FinalScore
    }
  }

  def handlePoseWait() : ChannelState = {
      if(secondsSinceLastStateChange > 10) {
        return PoseQuestion2
      }
      else {
        return PoseWait
      }
  }

    def handlePoseQuestion2() : ChannelState = {
      val q = game.currentQuestion.get
      val qText = q.text;

      if(debug) {
        println(q.text + "/" + q.points)
      }

      if(q.qtype == "simple" || q.qtype == "image") {
        client.sendMessage(channelId, msg("QUESTION", q.text).trim())
      }
      else {
        client.sendMessage(channelId, "Someone entered an unknown question type " + q.qtype)
      }

      return AnswerWait
  }


  def handleAnswerWait() : ChannelState = {
    if(secondsSinceLastStateChange > timing("answerwait")) {
      if(currentState == AnswerWait)
        return NoAnswerTimeout
      else
        return currentState
    }

    return AnswerWait
  }

  def handleQuestionWait() : ChannelState = {
    if(secondsSinceLastStateChange < timing("questionwait")) {
      return currentState
    }

    return PoseQuestion1
  }

  def handleNoAnswerTimeout: ChannelState = {

    client.sendMessage(channelId, msg("TIMEOUT"))
    client.sendMessage(channelId, msg("MISSED_ANSWER", game.currentQuestion.get.possibleAnswers()).trim())
    return QuestionWait
  }

  def handleFinalScore: ChannelState = {
    client.sendMessage(channelId, msg("FINAL_SCORE"))
    ScoreCommand.handle(null, channelContext)
    return ChannelState.Over
  }

  def handleGameOver: ChannelState = {
    client.sendMessage(channelId, msg("GAME_ENDED"))
    return ChannelState.Initial
  }

}
