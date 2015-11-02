package org.mbs3.trivial

import akka.actor._
import org.mbs3.trivial.actors._
import slack.rtm.SlackRtmClient
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.mbs3.trivial.game.GameStorage
import org.mbs3.trivial.game.Game

object Main {

  def main(args: Array[String]): Unit = {
    // Game.find.questionList.foreach { x => x.isAnsweredBy("pawn") }
    // return;
    
    val conf = ConfigFactory.load()
    val token = conf.getString("trivial.slack_token")
    val debug = conf.getBoolean("trivial.debug")
    
    implicit val system = ActorSystem("org_mbs3_trivial")
    val client = SlackRtmClient(token)
    val selfId = client.state.self.id
    
    val channelManager = system.actorOf(Props(classOf[ChannelManager], client, debug), "ChannelManager")
    system.actorOf(Props(classOf[Terminator], channelManager, client), "terminator")
    client.addEventListener(channelManager)
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