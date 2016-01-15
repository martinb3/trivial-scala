package org.mbs3.trivial.game

import java.util.Properties
import slack.rtm.SlackRtmClient
import slack.models.Message
import slack.api.BlockingSlackApiClient

class GameManager(client: SlackRtmClient, channelId: String, debug: Boolean) {
  object GameState extends Enumeration {   
    type GameState = Value
    val 
      Initial, // we've never played a game
      New, // we want to start a new game
      QuestionWait, // wait for the question 
      PoseQuestion, // ask/announce a question
      AnswerWait, // waiting for an answer
      NoAnswerTimeout, // time ran out, no answer
      FinalScore, // out of questions, say score
      Over // we've said the score, game can be cleaned up
        = Value
  }
  
  import GameState._
  var game = Game.find
  
  def tick {
    val oldState = currentState
    val newState = currentState match {
      case GameState.Initial => GameState.Initial
      case GameState.New => handleNewGame
      case GameState.PoseQuestion => handlePoseQuestion
      case GameState.AnswerWait => handleAnswerWait 
      case GameState.NoAnswerTimeout => handleNoAnswerTimeout
      case GameState.QuestionWait => handleQuestionWait
      case GameState.FinalScore => handleFinalScore    
      case GameState.Over => handleGameOver
    }
    changeState(newState)
  }
  
  def isInGame = currentState != GameState.Initial
  
  // this can receive two things, commands or guesses
  def message(message: Message) {
    if(message.text.startsWith("<@"+client.state.self.id+">: ")) {
      handleCommand(message)
    }
    else if(currentState == GameState.AnswerWait) {
      handleGuess(message)
    }
    else if(debug) {
      val user = client.state.getUserById(message.user).get
      val to_user = message.channel
      println("Message received (unmatched) for GameManager by user "+user.name+" on " + channelId + ": [" + message.text+"]")
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
    
    if(scores.contains(user.name))
      points += scores.get(user.name).get
    
    scores.put(user.name, points+q.points)
    changeState(QuestionWait)
  }
  
  // someone has said a game command while we don't have a game going
  var game_master : String = null
  def handleCommand(message: Message) {
    if(message.text.endsWith("!game") && currentState == GameState.Initial) {
      game_master = message.user
      val user = client.state.getUserById(message.user).get
      client.sendMessage(channelId, msg("NEW_GAME_REQUEST", user.name))
      changeState(New)
    }
    else if(message.text.endsWith("!scores") && currentState != GameState.Initial) {
      handleScoreRequest
    }
  }
  
  def handleNewGame(): GameState = {
    if(debug) {
      return PoseQuestion
    }
    
    if(secondsSinceLastStateChange > timing("cutoff")) {
      client.sendMessage(channelId, msg("NEW_GAME", game.title))
      return PoseQuestion
    }
    else if(secondsSinceLastStateChange % timing("modamount") == 0) {
      client.sendMessage(channelId, msg("NEW_GAME2", game.title))
    }
    return New
  }
  
  def handlePoseQuestion() : GameState = {
    if(game.questionsAvailable) {
      val q = game.advance
      val qText = q.text; val qPoints = q.points
      
      if(debug) {
        println(q.text + "/" + q.points)
      }
      
      if(q.qtype == "simple") {
        client.sendMessage(channelId, 
            msg("QUESTION_POSE", q.points).trim() + 
            "\n" + 
            msg("QUESTION", q.text).trim()
            )
      }
      else if(q.qtype == "image") {
        client.sendMessage(channelId, msg("QUESTION_POSE", q.points).trim())
        client.sendMessage(channelId, msg("QUESTION", q.text).trim()) 
      }
      else {
        client.sendMessage(channelId, "Someone entered an unknown question type " + q.qtype)
      }
          
      return AnswerWait
    }
    else {
      return FinalScore
    }
  }
  
  def handleAnswerWait() : GameState = {
    if(secondsSinceLastStateChange > timing("answerwait")) {
      if(currentState == AnswerWait)
        return NoAnswerTimeout
      else
        return currentState
    }
    
    return AnswerWait
  }
  
  def handleQuestionWait() : GameState = {
    if(secondsSinceLastStateChange < timing("questionwait")) {
      return currentState
    }
    
    return PoseQuestion
  }
  
  def handleNoAnswerTimeout: GameState = {
    
    client.sendMessage(channelId, msg("TIMEOUT"))
    client.sendMessage(channelId, msg("MISSED_ANSWER", game.currentQuestion.get.possibleAnswers()).trim()) 
    return QuestionWait
  }
  
  def handleFinalScore: GameState = {
    client.sendMessage(channelId, msg("FINAL_SCORE"))
    handleScoreRequest
    return GameState.Over
  }
  
  def handleScoreRequest {
    val scoreboard = game.scores
    val score_set = scoreboard.keySet
    
    if(score_set.size > 0) {
      score_set.foreach { username => client.sendMessage(channelId, username+": "+scoreboard.get(username).get) }
    }
    else {
      client.sendMessage(channelId, msg("NO_SCORE"))
    }
  }
  
  def handleGameOver: GameState = {
    client.sendMessage(channelId, msg("GAME_ENDED"))
    return GameState.Initial
  }
  
  def timing : Map[String, Integer] = {
    if(debug) {
      Map(
        "cutoff" -> 10,
        "modamount" -> 5,
        "answerwait" -> 10,
        "questionwait" -> 5
      )
    } else {
        Map(
        "cutoff" -> 120,
        "modamount" -> 30,
        "answerwait" -> 45,
        "questionwait" -> 45
       )
    }
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

