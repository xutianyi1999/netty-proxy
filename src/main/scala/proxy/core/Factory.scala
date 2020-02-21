package proxy.core

import java.nio.charset.StandardCharsets

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.epoll.{EpollChannelOption, EpollEventLoopGroup, EpollServerSocketChannel, EpollSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.{ChannelInitializer, ChannelOption}
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import proxy.server.handler.ServerProxyHandler

object Factory {

  val delimiter: ByteBuf = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("¤".getBytes(StandardCharsets.UTF_8)))
  val isLinux: Boolean = System.getProperty("os.name").contains("Linux")

  private val config = if (isLinux)
    (
      new EpollEventLoopGroup(),
      new EpollEventLoopGroup(),
      classOf[EpollServerSocketChannel],
      classOf[EpollSocketChannel]
    )
  else
    (
      new NioEventLoopGroup(),
      new NioEventLoopGroup(),
      classOf[NioServerSocketChannel],
      classOf[NioSocketChannel]
    )

  def createBootstrap(): Bootstrap = new Bootstrap().group(config._2)
    .channel(config._4)
    .option[Integer](EpollChannelOption.TCP_FASTOPEN, 256)
    .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
    .option[java.lang.Boolean](EpollChannelOption.TCP_QUICKACK, true)

  def createServerBootstrap(): ServerBootstrap = new ServerBootstrap().group(config._1, config._2)
    .channel(config._3)
    .option[Integer](EpollChannelOption.TCP_FASTOPEN, 256)
    .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
    .childOption[java.lang.Boolean](EpollChannelOption.TCP_QUICKACK, true)

  def createServer(): ServerBootstrap = {
    val serverInitializer: ChannelInitializer[SocketChannel] = socketChannel => {
      socketChannel.pipeline()
        .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, delimiter))
        .addLast(new ServerProxyHandler(createBootstrap))
    }

    createServerBootstrap().childHandler(serverInitializer)
  }

  def clientCreate(): ServerBootstrap = {
    val serverInitializer: ChannelInitializer[SocketChannel] = socketChannel => {

    }

  }
}
