package org.mbs3.trivial.commands

import org.mbs3.trivial._
import org.mbs3.trivial.ChannelState
import slack.models.Message
import org.mbs3.trivial.game.Game
import org.mbs3.trivial.MessageFormatter.msg

object NewGameCommand extends Command {
  def accept(message: Message, context: ChannelContext) = {
    context.currentState == ChannelState.Initial &&
    (
        message.text.toLowerCase().trim().startsWith("!emcee") ||
        message.text.toLowerCase().trim().startsWith("!game")
     )
  }
  def handle(message: Message, context: ChannelContext) {
    val gCtx = context.globalContext
    val client = gCtx.client
    val user = client.state.getUserById(message.user).get
    
    if(message.text.toLowerCase().trim().startsWith("!game")) {
      try {
        val str_match = message.text.split("\\W")(2)
        println("Looking for " + str_match + " as a new game")
        context.game = Game.find(str_match)
      }
      catch {
        case e: Exception => {
          client.sendMessage(context.channelId, msg("GAME_NOT_FOUND", user.name))
          return
        }
      }
      
      client.sendMessage(context.channelId, msg("NEW_GAME_REQUEST", user.name))
      context.changeState(ChannelState.New)
    }
    else if(message.text.startsWith("!emcee")) {
      context.game = Game.empty
      client.sendMessage(context.channelId, msg("NEW_GAME_EMCEE", user.name))
      context.changeState(ChannelState.Emcee)
    }
    
    context.gameMaster = message.user
    context.game.scores.clear
    
    if(context.globalContext.debug) {
      println(context.gameMaster + " is the game master!")
    }
  }
  
}