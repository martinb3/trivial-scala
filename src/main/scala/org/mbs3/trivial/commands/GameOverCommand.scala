package org.mbs3.trivial.commands

import org.mbs3.trivial._
import slack.models.Message
import org.mbs3.trivial.ChannelState

object GameOverCommand extends Command {
  def accept(message: Message, context: ChannelContext) = {
    message.text.toLowerCase().equals("!fin") && context.isInGame
  }
  def handle(message: Message, context: ChannelContext) { 
    context.changeState(ChannelState.FinalScore)
  }
  
}