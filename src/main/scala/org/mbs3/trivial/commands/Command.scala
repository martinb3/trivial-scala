package org.mbs3.trivial.commands

import org.mbs3.trivial.ChannelContext
import slack.models.Message

trait Command {
  def accept(message: Message, context: ChannelContext): Boolean
  def handle(message: Message, context: ChannelContext): Unit
}

object Command {
  val commands = 
    PingCommand ::
    NewGameCommand ::
    ScoreCommand ::
    PointsCommand ::
    GameOverCommand ::
    Nil
  
  def list() : List[Command] = commands
}