package org.mbs3.trivial.actors;

import akka.actor._
import scala.concurrent.duration._
import slack.rtm.SlackRtmClient
import slack.models._
import org.mbs3.trivial.game._

class ChannelActor(client: SlackRtmClient, channelId: String, debug: Boolean) extends Actor with ActorLogging {
  import context._

  override def preStart() = system.scheduler.scheduleOnce(500 millis, self, "tick")
  override def postRestart(reason: Throwable) = {}
  
  val gameManager = new GameManager(client, channelId, debug) 
    
  def receive = {
    case "terminate" => context.stop(self)
    case "tick" => {
      gameManager.tick
      var tickSpeed : FiniteDuration = null 
      
      if(gameManager.isInGame) {
        // tick fast if we're a game going
        tickSpeed = 1000 millis
      }
      else {
        // tick slow otherwise
        tickSpeed = 8*1000 millis
      } 
      
      system.scheduler.scheduleOnce(tickSpeed, self, "tick")
      
    }
    // did we get an answer to the question?
    case m: Message => gameManager.message(m)
    case _ => log.warning("Channel! Unknown message received in {}", channelId)
  }
}