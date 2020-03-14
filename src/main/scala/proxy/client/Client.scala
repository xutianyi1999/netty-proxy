package proxy.client

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{Channel, ChannelInitializer}
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.bytes.{ByteArrayDecoder, ByteArrayEncoder}
import proxy.Factory
import proxy.client.handler.{ClientMuxHandler, ClientProxyHandler}
import proxy.common._
import proxy.common.handler.{RC4Decrypt, RC4Encrypt}

import scala.util.{Failure, Success, Try}

object Client {
  def start(listen: Int, host: String, port: Int, key: String): Unit = {
    startClientMux(host, port, new RC4(key))
    startClientProxy(listen)
  }

  def mapRemove(channelId: String): Option[Channel] = ClientCatch.map.remove(channelId)

  /**
   * 启动客户端代理服务器
   *
   * @param listen 监听端口
   */
  private def startClientProxy(listen: Int): Unit = {
    val putChannel: (String, Channel) => Unit = ClientCatch.map.put

    val initializer: ChannelInitializer[SocketChannel] = socketChannel => {
      socketChannel.pipeline()
        .addLast(new ByteArrayEncoder)
        .addLast(new ClientProxyHandler(putChannel, mapRemove))
    }

    Factory.createTcpServerBootstrap
      .childHandler(initializer)
      .bind(listen)
      .sync()

    Commons.log.info(s"Listen: $listen")
  }

  /**
   * 客户端远程连接
   *
   * @param host 服务器ip
   * @param port 服务器端口
   */
  private def startClientMux(host: String, port: Int, rc4: RC4): Unit = {
    val address = new InetSocketAddress(host, port)

    val write: (String, => Array[Byte]) => Unit = (channelId, data) => {
      ClientCatch.map.get(channelId).foreach(_.writeAndFlush(data))
    }

    val close: CloseInfo => Unit = {
      case CloseAll =>
        val values = ClientCatch.map.values
        ClientCatch.map.clear()
        values.foreach(_.close())

      case CloseOne(channelId) => mapRemove(channelId).foreach(_.close())
    }

    def connect(channel: => Channel): Unit = Factory.delay.curried { () =>
      Try(channel) match {
        case Failure(exception) => exception.printStackTrace(); connect(channel)
        case Success(v) =>
          ClientCatch.remoteChannelOption = Option(v)
          Commons.log.info("Connected")
      }
    }(3)(TimeUnit.SECONDS)

    val bootstrap = Factory.createTcpBootstrap

    def f(): Channel = bootstrap.connect(address).sync().channel()

    val clientInitializer: ChannelInitializer[SocketChannel] = socketChannel => {
      val disconnectListener = () => {
        Commons.log.severe("Disconnected")

        ClientCatch.remoteChannelOption = Option.empty
        connect(f())
      }

      import proxy.common.Convert.ByteBufConvert.byteArrayToByteBuf

      socketChannel.pipeline()
        .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, Message.delimiter))
        .addLast(new ByteArrayEncoder)
        .addLast(new ByteArrayDecoder)
        .addLast(new RC4Encrypt(rc4))
        .addLast(new RC4Decrypt(rc4))
        .addLast(new ClientMuxHandler(disconnectListener, write, close))
    }

    bootstrap.handler(clientInitializer)
    connect(f())
  }
}
