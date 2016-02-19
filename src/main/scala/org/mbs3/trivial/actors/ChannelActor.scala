package org.mbs3.trivial.actors;

import akka.actor._
import scala.concurrent.duration._
import slack.rtm.SlackRtmClient
import slack.models._
import org.mbs3.trivial.game._
import slack.api.BlockingSlackApiClient
import org.mbs3.trivial.ChannelContext

class ChannelActor(channelContext: ChannelContext) extends Actor with ActorLogging {
  import context._

  override def preStart() = system.scheduler.scheduleOnce(500 millis, self, "tick")
  override def postRestart(reason: Throwable) = {}
  
  val channelManager = new ChannelManager(channelContext)
    
  def receive = {
    case "terminate" => context.stop(self)
    case "tick" => {
      channelManager.tick
      var tickSpeed : FiniteDuration = null 
      
      // tick fast
      tickSpeed = 1000 millis

      system.scheduler.scheduleOnce(tickSpeed, self, "tick")
      
    }
    // did we get an answer to the question?
    case m: Message => channelManager.message(m)
    case _ => log.warning("Channel! Unknown message received in {}", channelContext.channelId)
  }
}