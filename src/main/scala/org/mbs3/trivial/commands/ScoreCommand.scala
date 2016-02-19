package org.mbs3.trivial.commands

import org.mbs3.trivial._
import slack.models.Message
import org.mbs3.trivial.MessageFormatter.msg

object ScoreCommand extends Command {

  def accept(message: Message, context: ChannelContext) = {
    context.isInGame && 
      message.text.toLowerCase().trim().startsWith("!score")
  }
  def handle(message: Message, context: ChannelContext) { 
    val scoreboard = context.game.scores
    val sorted_scores = scoreboard.toList.sortBy { _._2  }
    
    sorted_scores.foreach(tuple => {
      val (name, score) = tuple
      context.globalContext.client.sendMessage(context.channelId, name+": "+score)
    })

    if(scoreboard.isEmpty) {
      context.globalContext.client.sendMessage(context.channelId, msg("NO_SCORE"))
    }
  }
}
