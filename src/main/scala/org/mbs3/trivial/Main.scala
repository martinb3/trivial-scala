package org.mbs3.trivial

import akka.actor._
import org.mbs3.trivial.actors._
import slack.rtm.SlackRtmClient
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Main {

  def main(args: Array[String]): Unit = {    
    val conf = ConfigFactory.load()
    val token = conf.getString("trivial.slack_token")
    
    implicit val system = ActorSystem("org_mbs3_trivial")
    val client = SlackRtmClient(token)
    val selfId = client.state.self.id
    
    val channelManager = system.actorOf(Props(classOf[ChannelManager], client), "ChannelManager")
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