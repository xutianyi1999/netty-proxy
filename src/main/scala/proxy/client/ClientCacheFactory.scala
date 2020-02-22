package proxy.client

import java.util.concurrent.ConcurrentHashMap

import io.netty.channel.Channel

object ClientCacheFactory {

  // channelId -> channel
  val channelMap = new ConcurrentHashMap[String, Channel]

  @volatile var mainChannelOption = Option.empty[Channel]
}
