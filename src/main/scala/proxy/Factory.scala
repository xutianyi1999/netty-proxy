package proxy

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.channel.epoll._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.unix.DomainSocketAddress
import io.netty.util.concurrent.ScheduledFuture
import proxy.common.Commons

object Factory {

  private val (bossGroup, workerGroup, serverSocketChannel, socketChannel, localServerSocketChannel, localSocketChannel) =
    if (Epoll.isAvailable) {
      Commons.log.info("Epoll transport")
      Commons.localAddress = new DomainSocketAddress("netty-proxy-domain-address")

      (
        new EpollEventLoopGroup(),
        new EpollEventLoopGroup(),
        classOf[EpollServerSocketChannel],
        classOf[EpollSocketChannel],
        classOf[EpollServerDomainSocketChannel],
        classOf[EpollDomainSocketChannel]
      )
    } else {
      Commons.localAddress = new InetSocketAddress("127.0.0.1", 20001)
      val (serverSocketChannel, socketChannel) = classOf[NioServerSocketChannel] -> classOf[NioSocketChannel]

      (
        new NioEventLoopGroup(),
        new NioEventLoopGroup(),
        serverSocketChannel,
        socketChannel,
        serverSocketChannel,
        socketChannel
      )
    }

  def createTcpBootstrap: Bootstrap = new Bootstrap()
    .group(workerGroup)
    .channel(socketChannel)

  def createTcpServerBootstrap: ServerBootstrap = new ServerBootstrap()
    .group(bossGroup, workerGroup)
    .channel(serverSocketChannel)

  def createLocalBootstrap: Bootstrap = new Bootstrap()
    .group(workerGroup)
    .channel(localSocketChannel)

  def createLocalServerBootstrap: ServerBootstrap = new ServerBootstrap()
    .group(bossGroup, workerGroup)
    .channel(localServerSocketChannel)

  val delay: (Runnable, Long, TimeUnit) => ScheduledFuture[_] = workerGroup.schedule
}
