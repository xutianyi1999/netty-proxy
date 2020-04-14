package proxy.client

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.bytes.{ByteArrayDecoder, ByteArrayEncoder}
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.concurrent.GenericFutureListener
import proxy.Factory
import proxy.client.handler.ClientMuxHandler
import proxy.common._
import proxy.common.`case`.{CloseAll, CloseCase, CloseOne}
import proxy.common.crypto.CipherTrait
import proxy.common.handler.{DecryptHandler, EncryptHandler}

import scala.collection.JavaConverters._
import scala.collection.mutable

class ClientMuxChannel(name: String, host: String, port: Int, cipher: CipherTrait, heartbeatInterval: Int) {

  private val map: mutable.Map[String, Channel] = new ConcurrentHashMap[String, Channel].asScala
  @volatile private var channelOption = Option.empty[Channel]

  def isActive: Boolean = channelOption.isDefined

  def writeToRemote(data: => Array[Byte], readChannel: Channel): Unit =
    channelOption.foreach { remoteChannel =>
      remoteChannel.writeAndFlush(data)
      Commons.trafficShaping(remoteChannel, readChannel)
    }

  import proxy.common.Convert.ChannelIdConvert._
  import proxy.common.Convert.ChannelImplicit

  def register(localChannel: Channel): Unit =
    channelOption match {
      case Some(remoteChannel) => remoteChannel.eventLoop().execute { () =>
        if (remoteChannel.isActive) {
          implicit val localChannelId: String = localChannel

          map.put(localChannelId, localChannel)
          remoteChannel.writeAndFlush(Message.connectMessageTemplate)
        } else localChannel.safeClose()
      }

      case None => localChannel.safeClose()
    }

  def remove(channelId: String): Unit = if (map.remove(channelId).isDefined) {
    channelOption.foreach(_.writeAndFlush(Message.disconnectMessageTemplate(channelId)))
  }

  private val writeToLocal: (String, => Array[Byte]) => Unit = (channelId, data) => {
    map.get(channelId).foreach(_.writeAndFlush(data))
  }

  private val close: CloseCase => Unit = {
    case CloseAll =>
      val values = map.values
      map.clear()
      values.foreach(_.safeClose())

    case CloseOne(channelId) => map.remove(channelId).foreach(_.safeClose())
  }

  private val bootstrap = Factory.createTcpBootstrap()
    .option[WriteBufferWaterMark](ChannelOption.WRITE_BUFFER_WATER_MARK, Commons.waterMark)

  private val connectListener: GenericFutureListener[ChannelFuture] = future =>
    if (future.isSuccess) {
      Commons.log.info(s"$name connected")
      channelOption = Option(future.channel())
    } else {
      Commons.printError(future.cause())
      connect()
    }

  private def connect(): Unit = Factory.delay(() =>
    bootstrap.connect(host, port).addListener(connectListener),
    3, TimeUnit.SECONDS
  )

  private val disconnectListener = () => {
    Commons.log.error(s"$name disconnected")
    channelOption = Option.empty
    connect()
  }

  private val clientInitializer: ChannelInitializer[SocketChannel] = socketChannel => {
    import proxy.common.Convert.ByteBufConvert.byteArrayToByteBuf
    socketChannel.pipeline()
      .addLast(new IdleStateHandler(0, heartbeatInterval, 0))
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
