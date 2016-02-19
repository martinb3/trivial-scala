package org.mbs3.trivial.commands

import org.mbs3.trivial._
import slack.models.Message
import org.mbs3.trivial.MessageFormatter.msg

object PointsCommand extends Command {
  def accept(message: Message, context: ChannelContext) = {
    context.isInGame &&
      message.text.startsWith("!") &&
      message.text.toLowerCase().contains("point") && 
      context.gameMaster == message.user
  }
  def handle(message: Message, context: ChannelContext) {
      var messageText = message.text
      if(messageText.startsWith("!")) {
        messageText = message.text.substring(1)
      }
      
      val pattern = "([\\-0-9.]+) points? to ([0-9A-Za-z ]+)".r
      messageText match {
        case pattern(points_to_give, user_name) => {
          val scores = context.game.scores
          var points = 0f
          
          val sanitized_name = user_name.trim().toLowerCase()
          if(scores.contains(sanitized_name))
            points += scores.get(sanitized_name).get

          val newpoints = points_to_give.toFloat
          scores.put(sanitized_name, points+newpoints)
          context.globalContext.client.sendMessage(context.channelId, msg("GIVE_POINTS", newpoints, user_name, points+newpoints))
        }
        case _ => println("'" + messageText + "' did not match a 'points for' regex")
      }
  }
}
