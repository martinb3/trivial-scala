package org.mbs3.trivial.game

import java.util.Properties
import slack.rtm.SlackRtmClient
import slack.models.Message

class GameManager(client: SlackRtmClient, channelId: String) {
  object GameState extends Enumeration {   
    type GameState = Value
    val 
      Initial, // we've never played a game
      New, // we want to start a new game
      PoseQuestion, // ask/announce a question
      AnswerWait, // waiting for an answer
      NoAnswerTimeout, // time ran out, no answer
      FinalScore, // out of questions, say score
      Over // we've said the score, game can be cleaned up
        = Value
  }
  
  import GameState._
  var game : Game = null
  
  def tick {
    val oldState = currentState
    val newState = currentState match {
      case GameState.Initial => GameState.Initial
      case GameState.New => handleNewGame
      case GameState.PoseQuestion => handlePoseQuestion
      case GameState.AnswerWait => handleAnswerWait 
      case GameState.NoAnswerTimeout => handleNoAnswerTimeout
      case GameState.FinalScore => handleFinalScore    
      case GameState.Over => handleGameOver
    }
    changeState(newState)
  }
  
  def isInGame = currentState != GameState.Initial
  
  // this can receive two things, commands or guesses
  def message(message: Message) {
    if(message.text.startsWith(client.state.self.name)) {
      handleCommand(message)
    }
    else if(currentState == GameState.AnswerWait) {
      handleGuess(message)
    }
  }
  
  // someone has guessed while we're in AnswerWait
  def handleGuess(message: Message) {
    val q = game.currentQuestion
    if(q.isDefined && message.text.toLowerCase().contains(q.get.answer.toLowerCase())) {
      val user = client.state.getUserById(message.user).get
      client.sendMessage(channelId, msg("CORRECT_ANSWER", user.name))
      val scores = game.scores
      var points = 0f
      
      if(scores.contains(user.name))
        points += scores.get(user.name).get
        
      scores.put(user.name, points+q.get.points)
      changeState(GameState.PoseQuestion)
    }
  }
  
  // someone has guessed while we're in AnswerWait
  def handleCommand(message: Message) {
    if(message.text.endsWith("!game") && currentState == GameState.Initial) {
      val user = client.state.getUserById(message.user).get
      client.sendMessage(channelId, msg("NEW_GAME_REQUEST", user.name))
      changeState(New)
    }
  }
  
  def handleNewGame(): GameState = {
    if(secondsSinceLastStateChange > (2*5)) {
      game = Game.stub
      client.sendMessage(channelId, msg("NEW_GAME", game.title))
      return PoseQuestion
    }
    else if(secondsSinceLastStateChange % 5 == 0) {
      client.sendMessage(channelId, msg("NEW_GAME2"))
    }
    return New
  }
  
  def handlePoseQuestion() : GameState = {
    if(game.questionsAvailable) {
      val q = game.advance
      val qText = q.text; val qPoints = q.points
      client.sendMessage(channelId, msg("QUESTION_POSE", q.text, q.points)) 
      return AnswerWait
    }
    else {
      return FinalScore
    }
  }
  
  def handleAnswerWait() : GameState = {
    if(secondsSinceLastStateChange > 10) {
      if(currentState == AnswerWait)
        return NoAnswerTimeout
      else
        return currentState
    }
    
    return AnswerWait
  }
  
  def handleNoAnswerTimeout: GameState = {
    client.sendMessage(channelId, msg("TIMEOUT"))
    return PoseQuestion    
  }
  
  def handleFinalScore: GameState = {
    
    val scoreboard = game.scores
    client.sendMessage(channelId, msg("FINAL_SCORE"))
    scoreboard.keySet.foreach { username => client.sendMessage(channelId, username+": "+scoreboard.get(username).get) }
    return GameState.Over
  }
  
  def handleGameOver: GameState = {
    client.sendMessage(channelId, msg("GAME_ENDED"))
    return GameState.Initial
  }

  // everything below this point is synchronized so we don't accidentally have a guess
  // come in while a state is changing
  
  import java.time._
  var _lastStateChange : Instant = null
  var _currentState = GameState.Initial
  def changeState(requestedState: GameState) {
    _currentState.synchronized({
      // don't if we are already there
      if(requestedState == _currentState) { return }
      
      _lastStateChange = Instant.now()
      _currentState = requestedState  
    })
  }
  def currentState : GameState = _currentState.synchronized({ return _currentState})
   
  def secondsSinceLastStateChange : Long = { 
    _currentState.synchronized({
      return Duration.between(_lastStateChange, Instant.now()).getSeconds
    })
  }
  
  def msg(key: String, args: Any*): String = {
    if(props.containsKey(key))
      return props.getProperty(key).format(args:_*)
    return "THIS IS AN ERROR. PLEASE REPORT ERROR: %s".format(key.toUpperCase())
  }
  
  lazy val props = loadProperties
  def loadProperties : Properties = {
    val p = new Properties()
    p.load(new java.io.FileReader("messages.properties"))
    p
  }
}

