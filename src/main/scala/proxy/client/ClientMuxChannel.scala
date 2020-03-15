package proxy.client

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{Channel, ChannelFuture, ChannelInitializer}
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.bytes.{ByteArrayDecoder, ByteArrayEncoder}
import io.netty.util.concurrent.GenericFutureListener
import proxy.Factory
import proxy.client.handler.ClientMuxHandler
import proxy.common._
import proxy.common.handler.{RC4Decrypt, RC4Encrypt}

import scala.collection.JavaConverters._
import scala.collection.mutable

class ClientMuxChannel(name: String, host: String, port: Int, rc4: RC4) {

  private val map: mutable.Map[String, Channel] = new ConcurrentHashMap[String, Channel].asScala
  private var channelOption = Option.empty[Channel]

  def isActive: Boolean = channelOption.isDefined

  def writeToRemote(data: => Array[Byte]): ClientMuxChannel = {
    channelOption.foreach(_.writeAndFlush(data))
    this
  }

  def register(channelId: String, channel: Channel): ClientMuxChannel = {
    map.put(channelId, channel)
    this
  }

  def remove(channelId: String): Option[Channel] = map.remove(channelId)

  private val writeToLocal: (String, => Array[Byte]) => Unit = (channelId, data) => {
    map.get(channelId).foreach(_.writeAndFlush(data))
  }

  private val close: CloseInfo => Unit = {
    case CloseAll =>
      val values = map.values
      map.clear()
      values.foreach(_.close())

    case CloseOne(channelId) => map.remove(channelId).foreach(_.close())
  }

  private val bootstrap = Factory.createTcpBootstrap

  private def connect(): Unit = {
    val connectListener: GenericFutureListener[ChannelFuture] = future =>
      if (future.isSuccess) {
        Commons.log.info(s"$name connected")
        channelOption = Option(future.channel())
      } else {
        future.cause().printStackTrace()
        connect()
      }

    Factory.delay.curried { () =>
      bootstrap.connect(host, port).addListener(connectListener)
    }(3)(TimeUnit.SECONDS)
  }

  private val disconnectListener = () => {
    Commons.log.severe(s"$name disconnected")
    channelOption = Option.empty
    connect()
  }

  private val clientInitializer: ChannelInitializer[SocketChannel] = socketChannel => {
    import proxy.common.Convert.ByteBufConvert.byteArrayToByteBuf
    socketChannel.pipeline()
      .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, Message.delimiter))
      .addLast(new ByteArrayEncoder)
      .addLast(new ByteArrayDecoder)
      .addLast(new RC4Encrypt(rc4))
      .addLast(new RC4Decrypt(rc4))
      .addLast(new ClientMuxHandler(disconnectListener, writeToLocal, close))
  }

  bootstrap.handler(clientInitializer)
  connect()
}
