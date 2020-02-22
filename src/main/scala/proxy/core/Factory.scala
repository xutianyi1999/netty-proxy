package proxy.core

import java.nio.charset.StandardCharsets

import com.alibaba.fastjson.{JSON, JSONObject}
import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.epoll.{EpollChannelOption, EpollEventLoopGroup, EpollServerSocketChannel, EpollSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.{ChannelInitializer, ChannelOption}
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import proxy.client.ClientCacheFactory
import proxy.client.handler.{ClientReceiveHandler, ClientSendHandler}
import proxy.server.handler.ServerProxyHandler

import scala.util.Using

object Factory {

  val delimiter: ByteBuf = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("¤".getBytes(StandardCharsets.UTF_8)))
  val isLinux: Boolean = System.getProperty("os.name").contains("Linux")

  private val jsonConfig: JSONObject = Using(this.getClass.getResourceAsStream("/config.json")) {
    in => JSON.parseObject[JSONObject](in, StandardCharsets.UTF_8, classOf[JSONObject])
  }.get

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

  def createServer(): () => Unit = {
    val serverConfig = jsonConfig.getJSONObject("server")
    val target = serverConfig.getJSONObject("target")

    def createClient(): Bootstrap = createBootstrap().remoteAddress(
      target.getString("host"),
      target.getIntValue("port")
    )

    val serverInitializer: ChannelInitializer[SocketChannel] = socketChannel => {
      socketChannel.pipeline()
        .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, delimiter))
        .addLast(new ServerProxyHandler(createClient))
    }

    val serverBootstrap = createServerBootstrap()
      .childHandler(serverInitializer)

    () => serverBootstrap.bind(serverConfig.getIntValue("localPort"))
  }

  def createClient(): () => Unit = {
    val clientConfig = jsonConfig.getJSONObject("client")
    val target = clientConfig.getJSONObject("target")

    var connect: () => Unit = null

    val clientInitializer: ChannelInitializer[SocketChannel] = socketChannel => {
      socketChannel.pipeline()
        .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, delimiter))
        .addLast(new ClientSendHandler(() => connect()))
    }

    val bootstrap = createBootstrap().handler(clientInitializer)

    connect = () => try {
      val channel = bootstrap.connect(
        target.getString("host"),
        target.getIntValue("port")
      ).sync().channel()

      ClientCacheFactory.mainChannelOption = Option(channel)
    } catch {
      case exception: Exception =>
        exception.printStackTrace()
        connect()
    }

    val serverBootstrap = createServerBootstrap().childHandler(new ClientReceiveHandler)
    () => {
      connect()
      serverBootstrap.bind(clientConfig.getIntValue("port"))
    }
  }

  private def createBootstrap(): Bootstrap = new Bootstrap().group(config._2)
    .channel(config._4)
    .option[Integer](EpollChannelOption.TCP_FASTOPEN, 256)
    .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
    .option[java.lang.Boolean](EpollChannelOption.TCP_QUICKACK, true)

  private def createServerBootstrap(): ServerBootstrap = new ServerBootstrap().group(config._1, config._2)
    .channel(config._3)
    .option[Integer](EpollChannelOption.TCP_FASTOPEN, 256)
    .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
    .childOption[java.lang.Boolean](EpollChannelOption.TCP_QUICKACK, true)
}
