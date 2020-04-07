package org.mbs3.trivial

import org.mbs3.trivial.game.Game

/**
 * Context of a particular channel, which might include a game. This is a state
 * machine, where the current state is an enumeration of GameState._ values.
 *
 * There should be no Slack references in here; this is just the raw state
 * machine, with some helper methods to understand timing and move between states.
 */
class ChannelContext(val globalContext: GlobalContext, val channelId: String, var gameMaster: String = null) {

  import ChannelState._
  var game : Game = null

  def isInGame = currentState != ChannelState.Initial

  // everything below this point is synchronized so we don't accidentally have a guess
  // come in while a state is changing

  import java.time._
  var _lastStateChange : Instant = null
  var _currentState = ChannelState.Initial
  def changeState(requestedState: ChannelState) {
    _currentState.synchronized({
      // don't if we are already there
      if(requestedState == _currentState) { return }

      _lastStateChange = Instant.now()
      _currentState = requestedState
    })
  }
  def currentState : ChannelState = _currentState.synchronized({ return _currentState})

  def secondsSinceLastStateChange : Long = {
    _currentState.synchronized({
      return Duration.between(_lastStateChange, Instant.now()).getSeconds
    })
  }



  def timing : Map[String, Integer] = {
    if(globalContext.debug) {
      Map(
        "cutoff" -> 10,
        "modamount" -> 5,
        "answerwait" -> 10,
        "questionwait" -> 5
      )
    } else {
        Map(
        "cutoff" -> 120,
        "modamount" -> 60,
        "answerwait" -> 15,
        "questionwait" -> 5
       )
    }
  }
}

object ChannelState extends Enumeration {
  type ChannelState = Value
  val
    Initial, // we've never played a game
    Emcee, // we're doing an emcee game, just respond to scores
    New, // we want to start a new game
    QuestionWait, // wait for the question
    PoseQuestion1, // ask/announce a question's points
    PoseWait, // pause for suspense
    PoseQuestion2, // ask/announce a question
    AnswerWait, // waiting for an answer
    NoAnswerTimeout, // time ran out, no answer
    FinalScore, // out of questions, say score
    Over // we've said the score, game can be cleaned up
      = Value
}
