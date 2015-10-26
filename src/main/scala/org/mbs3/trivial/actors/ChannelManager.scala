package org.mbs3.trivial.actors;

import akka.actor.Actor
import scala.concurrent.duration._
import slack.rtm.SlackRtmClient
import slack.models._
import akka.actor.Props
import akka.actor.ActorRef
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import akka.actor.ActorLogging

class ChannelManager(client: SlackRtmClient) extends Actor with ActorLogging {
  import context._

  val ignoredChannelIds = new ConcurrentSkipListSet[String] 
  val channelMap = new ConcurrentHashMap[String, ActorRef]
    
  override def postRestart(reason: Throwable) = {}
    
  def receive = {
    case m: Hello => {
      log.info("Hello! I'm connected to Slack.")

      val channels = client.state.channels
        .filter(c => c.is_member.isDefined && c.is_member.get) // member or unknown, we keep
        .filter(c => !channelMap.containsKey(c.id)) // don't include channels we already know 
        .filter(c => c.is_general.isDefined && !c.is_general.get )
        .map { channel =>
         log.info("Hello! creating actor for {} ({})", channel.name, channel.id)
          channelMap.put(
              channel.id, 
              system.actorOf(Props(classOf[ChannelActor], client, channel.id), channel.id)
          )         
        }
    }
    // you left a channel
    case m: ChannelLeft => {
      val channel_id = m.channel
      val actorRef = channelMap.remove(channel_id)
      if(actorRef == null) {
        log.warning("ChannelLeft! but I wasn't in {}", channel_id)
      }
      else {
        log.info("ChannelLeft! leaving {}", channel_id)
        actorRef ! "terminate"
      }
    }
    // you joined a channel
    case m: ChannelJoined if !ignoredChannelIds.contains(m.channel.id) => {
      log.info("Joined channel {}", m.channel)
      val channel = m.channel
      val ref = system.actorOf(Props(classOf[ChannelActor], client, channel.id), channel.id)
      channelMap.put(channel.id, ref)
    }
    
    // you saw a message and we know the channel but it wasn't us
    case m: Message if channelMap.containsKey(m.channel) && m.user != client.state.self.id => {
      channelMap.get(m.channel) ! m
    }
    case m: SlackEvent => log.debug("SlackEvent! " + m)
    case _ => log.info("Other! Message received")
  }
 
  def matchesIgnoredChannels(channel: Channel): Boolean = matchesIgnoredChannels(channel.id)
  def matchesIgnoredChannels(channelId: String): Boolean = ignoredChannelIds.contains(channelId)
  
  // context.stop(self) when we're really finished
}