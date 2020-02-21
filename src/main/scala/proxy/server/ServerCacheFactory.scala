package proxy.server

import java.util.concurrent.ConcurrentHashMap

import io.netty.channel.Channel

class ServerCacheFactory {

  // remoteChannelId -> channel
  private val remoteChannelIdMap = new ConcurrentHashMap[String, Channel]
  // localChannelId -> channel
  private val localChannelIdMap = new ConcurrentHashMap[String, Channel]
  // localChannelId -> remoteChannelId
  private val channelIdMap = new ConcurrentHashMap[String, String]

  def addChannel(remoteChannelId: String, channel: Channel): Unit = {
    val localChannelId = channel.id().asShortText()

    localChannelIdMap.put(localChannelId, channel)
    remoteChannelIdMap.put(remoteChannelId, channel)
    channelIdMap.put(localChannelId, remoteChannelId)
  }

  def removeAndClose(remoteChannelId: String, localChannelId: String): Unit = {
    channelIdMap.remove(localChannelId)
    localChannelIdMap.remove(localChannelId)
    val channel = remoteChannelIdMap.remove(remoteChannelId)

    if (channel != null) channel.close
  }

  def closeAll(): Unit = remoteChannelIdMap.values().forEach(_.close)

  def findLocalChannelId(remoteChannelId: String, consumer: String => Unit): Unit = {
    channelIdMap.entrySet().stream()
      .filter(_.getValue.equals(remoteChannelId))
      .findFirst()
      .map[String](_.getKey)
      .ifPresent(consumer(_))
  }

  def removeByRemoteChannelId(remoteChannelId: String): Unit = {
    findLocalChannelId(remoteChannelId, removeAndClose(remoteChannelId, _))
  }

  def removeByLocalChannelId(localChannelId: String): Unit = {
    val remoteChannelId = channelIdMap.get(localChannelId)
    if (remoteChannelId != null) removeAndClose(remoteChannelId, localChannelId)
  }

  def getRemoteChannelId(localChannelId: String): Option[String] = Option(channelIdMap.get(localChannelId))

  def getChannelByRemoteChannelId(remoteChannelId: String): Option[Channel] = Option(remoteChannelIdMap.get(remoteChannelId))
}
