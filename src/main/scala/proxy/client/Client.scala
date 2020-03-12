package proxy.client

import java.net.InetSocketAddress

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{Channel, ChannelInitializer}
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.bytes.ByteArrayEncoder
import proxy.Factory
import proxy.client.handler.{ClientMuxHandler, ClientProxyHandler}
import proxy.common.Convert._
import proxy.common._

import scala.util.{Failure, Success, Try}

object Client {

  def start(listen: Int, host: String, port: Int, key: String): Unit = {
    val rc4 = new RC4(key)
    startClientMux(host, port, rc4)
    startClientProxy(listen, rc4)
  }

  def mapRemove(channelId: String): Option[Channel] = ClientCatch.map.remove(channelId)

  /**
   * 启动客户端代理服务器
   *
   * @param listen 监听端口
   * @param rc4    rc4加密
   */
  private def startClientProxy(listen: Int, rc4: RC4): Unit = {
    val putChannel: (String, Channel) => Unit = ClientCatch.map.put

    val initializer: ChannelInitializer[SocketChannel] = socketChannel => {
      socketChannel.pipeline()
        .addLast(new ByteArrayEncoder)
        .addLast(new ClientProxyHandler(rc4, putChannel, mapRemove))
    }

    Factory.createTcpServerBootstrap
      .childHandler(initializer)
      .bind(listen)
      .sync()
  }

  /**
   * 客户端远程连接
   *
   * @param host 服务器ip
   * @param port 服务器端口
   * @param rc4  rc4加密
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

    def connect(t: Try[Channel]): Unit = {
      @scala.annotation.tailrec
      def re(): Channel = t match {
        case Failure(exception) => exception.printStackTrace(); re()
        case Success(channel) => channel
      }

      ClientCatch.remoteChannelOption = Option(re())
    }

    val clientInitializer: ChannelInitializer[SocketChannel] = socketChannel => {
      val disconnectListener = () => {
        ClientCatch.remoteChannelOption = Option.empty

        connect {
          Try(socketChannel.connect(address).sync().channel())
        }
      }

      socketChannel.pipeline()
        .addLast(new ByteArrayEncoder)
        .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, Message.delimiter))
        .addLast(new ClientMuxHandler(rc4, disconnectListener, write, close))
    }

    val bootstrap = Factory.createTcpBootstrap
      .handler(clientInitializer)

    connect {
      Try(bootstrap.connect(address).sync().channel())
    }
  }
}
