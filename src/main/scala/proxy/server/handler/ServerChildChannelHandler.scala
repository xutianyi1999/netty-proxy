package proxy.server.handler

import java.nio.charset.StandardCharsets

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.{Channel, ChannelFuture}
import io.netty.util.concurrent.GenericFutureListener
import proxy.Message
import proxy.core.Factory
import proxy.server.ServerCacheFactory

class ServerChildChannelHandler(bootstrap: Bootstrap, mainChannel: Channel) {

  private val cacheFactory = new ServerCacheFactory
  private val tcpClient: Bootstrap = bootstrap.handler(new ServerChildProxyHandler(writeToMain, passiveDisconnect))

  def connect(remoteChannelId: String): Unit = {
    val channelFuture = tcpClient.connect()
    val channel = channelFuture.channel()

    cacheFactory.addChannel(remoteChannelId, channel)

    val connectListener: GenericFutureListener[ChannelFuture] = future => if (future.isSuccess) {
      future.channel().flush()
    } else {
      cacheFactory.removeAndClose(remoteChannelId, channel.id().asShortText)
      future.cause().printStackTrace()
    }
    channelFuture.addListener(connectListener)
  }

  def disconnectAll(): Unit = cacheFactory.closeAll()

  def activeDisconnect(remoteChannelId: String): Unit = cacheFactory.removeByRemoteChannelId(remoteChannelId)

  def writeToChild(remoteChannelId: String, data: ByteBuf): Unit = {
    cacheFactory.getChannelByRemoteChannelId(remoteChannelId).foreach(channel => if (channel.isActive) {
      channel.writeAndFlush(data)
    } else {
      channel.write(data)
    })
  }

  private def writeToMain(localChannelId: String, data: ByteBuf): Unit = cacheFactory.getRemoteChannelId(localChannelId)
    .foreach(remoteChannelId => {
      val message = mainChannel.alloc().buffer()

      message
        .writeByte(Message.data)
        .writeCharSequence(remoteChannelId, StandardCharsets.UTF_8)

      message
        .writeBytes(data)
        .writeBytes(Factory.delimiter)

      mainChannel.writeAndFlush(message)
    })

  private def passiveDisconnect(localChannelId: String): Unit = cacheFactory.getRemoteChannelId(localChannelId)
    .foreach(remoteChannelId => {
      val disconnectMessage = mainChannel.alloc().buffer()

      disconnectMessage
        .writeByte(Message.disconnect)
        .writeCharSequence(remoteChannelId, StandardCharsets.UTF_8)

      disconnectMessage.writeBytes(Factory.delimiter)

      mainChannel.writeAndFlush(disconnectMessage)
      cacheFactory.removeAndClose(remoteChannelId, localChannelId)
    })
}
