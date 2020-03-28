package proxy.client

import java.util.concurrent.TimeUnit

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{Channel, ChannelFuture, ChannelInitializer, EventLoop}
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.bytes.{ByteArrayDecoder, ByteArrayEncoder}
import io.netty.util.concurrent.GenericFutureListener
import proxy.Factory
import proxy.client.handler.ClientMuxHandler
import proxy.common._
import proxy.common.crypto.CipherTrait
import proxy.common.handler.{DecryptHandler, EncryptHandler}

import scala.collection.mutable

class ClientMuxChannel(name: String, host: String, port: Int, cipher: CipherTrait) {

  private val map: mutable.Map[String, Channel] = mutable.Map.empty[String, Channel]
  private val eventLoop: EventLoop = Factory.getEventLoop
  @volatile private var channelOption = Option.empty[Channel]

  def isActive: Boolean = channelOption.isDefined

  def writeToRemote(data: => Array[Byte], readChannel: Channel): Unit =
    channelOption.foreach { remoteChannel =>
      remoteChannel.writeAndFlush(data)
      Commons.trafficShaping(remoteChannel, readChannel)
    }

  import proxy.common.Convert.ChannelIdConvert._

  def register(localChannel: Channel): Unit = eventLoop.execute { () =>
    implicit val localChannelId: String = localChannel

    channelOption match {
      case Some(channel) =>
        map.put(localChannelId, localChannel)
        channel.writeAndFlush(Message.connectMessageTemplate)

      case None => localChannel.close()
    }
  }

  def remove(channelId: String): Unit = eventLoop.execute { () =>
    if (map.remove(channelId).isDefined) {
      channelOption.foreach(_.writeAndFlush(Message.disconnectMessageTemplate(channelId)))
    }
  }

  private val writeToLocal: (String, => Array[Byte]) => Unit = (channelId, data) => eventLoop.execute { () =>
    map.get(channelId).foreach(_.writeAndFlush(data))
  }

  private val close: CloseInfo => Unit = closeInfo => eventLoop.execute { () =>
    closeInfo match {
      case CloseAll =>
        val values = map.values
        map.clear()
        values.foreach(_.close())

      case CloseOne(channelId) => map.remove(channelId).foreach(_.close())
    }
  }

  private val bootstrap = Factory.createTcpBootstrap

  private val connectListener: GenericFutureListener[ChannelFuture] = future =>
    eventLoop.execute { () =>
      if (future.isSuccess) {
        Commons.log.info(s"$name connected")
        channelOption = Option(future.channel())
      } else {
        Commons.log.severe(future.cause().getMessage)
        connect()
      }
    }

  private def connect(): Unit = eventLoop.schedule(() =>
    bootstrap.connect(host, port).addListener(connectListener),
    3, TimeUnit.SECONDS
  )

  private val disconnectListener = () => eventLoop.execute { () =>
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
      .addLast(new EncryptHandler(cipher))
      .addLast(new DecryptHandler(cipher))
      .addLast(new ClientMuxHandler(disconnectListener, writeToLocal, close))
  }

  bootstrap.handler(clientInitializer)
    .connect(host, port).addListener(connectListener)
}
