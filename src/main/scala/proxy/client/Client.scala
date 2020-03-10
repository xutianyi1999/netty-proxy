package proxy.client

import java.net.InetSocketAddress

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{Channel, ChannelInitializer}
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.bytes.ByteArrayEncoder
import proxy.Factory
import proxy.client.handler.{ClientMuxHandler, ClientProxyHandler}
import proxy.common.Convert._
import proxy.common.{Message, RC4}

import scala.util.{Failure, Success, Try}

object Client {

  def start(listen: Int, host: String, port: Int, key: String): Unit = {
    val rc4 = new RC4(key)
    startClientMux(host, port, rc4)
    startClientProxy(listen, rc4)
  }

  private def startClientProxy(listen: Int, rc4: RC4): Unit = {
    val putChannel: (String, Channel) => Unit = ClientCatch.map.put

    val initializer: ChannelInitializer[SocketChannel] = socketChannel => {
      socketChannel.pipeline()
        .addLast(new ByteArrayEncoder)
        .addLast(new ClientProxyHandler(rc4, putChannel, mapRemove))
    }

    Factory.createTcpServerBootstrap
      .handler(initializer)
      .bind(listen)
      .sync()
  }

  private def startClientMux(host: String, port: Int, rc4: RC4): Unit = {
    val address = new InetSocketAddress(host, port)

    val write: (String, => Array[Byte]) => Unit = (channelId, data) => {
      ClientCatch.map.getOption(channelId).foreach(_.writeAndFlush(data))
    }

    val close: String => Unit = {
      case "all" =>
        val values = ClientCatch.map.values()
        ClientCatch.map.clear()
        values.forEach(_.close(): Unit)

      case channelId: String => mapRemove(channelId).foreach(_.close())
    }

    val clientInitializer: ChannelInitializer[SocketChannel] = socketChannel => {
      val disconnectListener = () => {
        ClientCatch.remoteChannelOption = Option.empty
        ClientCatch.remoteChannelOption = Option {
          connect(() => socketChannel.connect(address).sync().channel())
        }
      }

      socketChannel.pipeline()
        .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, Message.delimiter))
        .addLast(new ClientMuxHandler(rc4, disconnectListener, write, close))
    }

    val bootstrap = Factory.createTcpBootstrap
      .handler(clientInitializer)

    ClientCatch.remoteChannelOption = Option {
      connect(() => bootstrap.connect(host, port).sync().channel())
    }
  }

  def mapRemove(channelId: String): Option[Channel] = Option(ClientCatch.map.remove(channelId))

  @scala.annotation.tailrec
  def connect(f: () => Channel): Channel = Try(f()) match {
    case Failure(exception) =>
      exception.printStackTrace()
      connect(f)
    case Success(channel) => channel
  }
}
