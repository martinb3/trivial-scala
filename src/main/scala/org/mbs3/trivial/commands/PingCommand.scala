package org.mbs3.trivial.commands

import org.mbs3.trivial.ChannelContext
import slack.models.Message

object PingCommand extends Command {
  def accept(message: Message, context: ChannelContext) = {
    message.text.toLowerCase().trim().equals("!ping")
  }
  def handle(message: Message, context: ChannelContext) { 
    context.globalContext.client.sendMessage(context.channelId, "PONG!")
  }
  
}