package proxy.client


import java.util.concurrent.ConcurrentHashMap

import io.netty.channel.Channel

import scala.collection.JavaConverters._
import scala.collection.mutable

object ClientCatch {

  // channelId -> channel
  val map: mutable.Map[String, Channel] = new ConcurrentHashMap[String, Channel].asScala

  var remoteChannelOption: Option[Channel] = Option.empty[Channel]
}
