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
        message.text.toLowerCase().trim().equals("!emcee") ||
        message.text.toLowerCase().trim().equals("!game")
     )
  }
  def handle(message: Message, context: ChannelContext) {
    val gCtx = context.globalContext
    val client = gCtx.client
    val user = client.state.getUserById(message.user).get
    context.gameMaster = message.user
    context.game = Game.find
    context.game.scores.clear
  
    if(context.globalContext.debug) {
      println(context.gameMaster + " is the game master!")
    }
    
    if(message.text.endsWith("!game")) {
      client.sendMessage(context.channelId, msg("NEW_GAME_REQUEST", user.name))
      context.changeState(ChannelState.New)
    }
    else if(message.text.endsWith("!emcee")) {
      client.sendMessage(context.channelId, msg("NEW_GAME_EMCEE", user.name))
      context.changeState(ChannelState.Emcee)
    }
  }
  
}