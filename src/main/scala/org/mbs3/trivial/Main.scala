package org.mbs3.trivial

import akka.actor._
import org.mbs3.trivial.actors._
import slack.rtm.SlackRtmClient
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.mbs3.trivial.game.GameStorage
import org.mbs3.trivial.game.Game
import slack.api.SlackApiClient
import slack.api.BlockingSlackApiClient
import java.util.Properties
import scala.concurrent.duration._
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import slack.models.Message

object Main {

  def main(args: Array[String]): Unit = {    
    val conf = ConfigFactory.load()
    val token = conf.getString("trivial.slack_token")
    val debug = false // conf.getBoolean("trivial.debug")
    
    // Game.find("food")
    
    implicit val system = ActorSystem("org_mbs3_trivial")
    val client = SlackRtmClient(token, 60 seconds)
    val selfId = client.state.self.id
    
    val globalContext = new GlobalContext(client, debug)
    val channelRouter = system.actorOf(Props(classOf[ChannelRouter], globalContext), "ChannelRouter")
    system.actorOf(Props(classOf[Terminator], channelRouter, client), "terminator")
    client.addEventListener(channelRouter)
    
   val m = Message("", "", "", "", None)
/*  ts: String,
  channel: String,
  user: String,
  text: String,
  is_starred: Option[Boolean]
) extends SlackEvent */
    
    val scheduler = QuartzSchedulerExtension(system)
    scheduler.createSchedule(
        "GameStarter", 
        None,
        // Seconds, Minutes, Hours, DoM, Month, DoW
        "0 * * ? * *", 
        None, 
        java.util.TimeZone.getTimeZone("America/New_York")
     )
     scheduler.schedule("GameStarter", channelRouter, "#ABC")
    
  }

  class Terminator(ref: ActorRef, client: SlackRtmClient) extends Actor with ActorLogging {
    context watch ref
    def receive = {
      case Terminated(_) =>
        log.info("{} has terminated, shutting down system", ref.path)
        context.system.terminate()
        client.close();
    }
  }

}