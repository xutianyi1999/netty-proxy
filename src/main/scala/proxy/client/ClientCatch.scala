package proxy.client

import java.util.concurrent.ConcurrentHashMap

import io.netty.channel.Channel

object ClientCatch {

  // channelId -> channel
  val map: java.util.Map[String, Channel] = new ConcurrentHashMap

  var remoteChannelOption: Option[Channel] = Option.empty[Channel]
}
