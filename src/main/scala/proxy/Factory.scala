package proxy

import java.util.concurrent.TimeUnit

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.util.concurrent.ScheduledFuture
import proxy.common.Commons

object Factory {

  private val (group, serverSocketChannel, socketChannel) = if (Epoll.isAvailable) {
    Commons.log.info("Epoll transport")

    (new EpollEventLoopGroup(), classOf[EpollServerSocketChannel], classOf[EpollSocketChannel])
  } else {
    (new NioEventLoopGroup(), classOf[NioServerSocketChannel], classOf[NioSocketChannel])
  }

  def createBootstrap(eventLoopGroup: EventLoopGroup = group): Bootstrap = new Bootstrap()
    .group(eventLoopGroup)
    .channel(socketChannel)

  def createServerBootstrap: ServerBootstrap = new ServerBootstrap()
    .group(group)
    .channel(serverSocketChannel)

  val delay: (Runnable, Long, TimeUnit) => ScheduledFuture[_] = group.schedule
}
