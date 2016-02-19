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

object Main {

  def main(args: Array[String]): Unit = {
    // do this to be sure the game is loadable
    Game.find.questionList
    
    val conf = ConfigFactory.load()
    val token = conf.getString("trivial.slack_token")
    val debug = conf.getBoolean("trivial.debug")
    
    implicit val system = ActorSystem("org_mbs3_trivial")
    val client = SlackRtmClient(token, 30 seconds)
    val selfId = client.state.self.id
    
    val globalContext = new GlobalContext(client, debug)
    val channelRouter = system.actorOf(Props(classOf[ChannelRouter], globalContext), "ChannelRouter")
    system.actorOf(Props(classOf[Terminator], channelRouter, client), "terminator")
    client.addEventListener(channelRouter)
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