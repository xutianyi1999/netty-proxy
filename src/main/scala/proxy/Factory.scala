package proxy

import java.util.concurrent.TimeUnit

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.{Epoll, EpollEventLoopGroup, EpollServerSocketChannel, EpollSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.socket.{ServerSocketChannel, SocketChannel}
import io.netty.util.concurrent.ScheduledFuture
import proxy.common.Commons

object Factory {

  private val tuple = if (Epoll.isAvailable) {
    Commons.log.info("Epoll transport")
    (new EpollEventLoopGroup(), new EpollEventLoopGroup(), classOf[EpollServerSocketChannel], classOf[EpollSocketChannel])
  } else {
    (new NioEventLoopGroup(), new NioEventLoopGroup(), classOf[NioServerSocketChannel], classOf[NioSocketChannel])
  }

  private val bossGroup: EventLoopGroup = tuple._1
  private val workerGroup: EventLoopGroup = tuple._2
  private val serverSocketChannel: Class[_ <: ServerSocketChannel] = tuple._3
  private val socketChannel: Class[_ <: SocketChannel] = tuple._4

  def createTcpBootstrap: Bootstrap = new Bootstrap()
    .group(workerGroup)
    .channel(socketChannel)

  def createTcpServerBootstrap: ServerBootstrap = new ServerBootstrap()
    .group(bossGroup, workerGroup)
    .channel(serverSocketChannel)

  def createLocalBootstrap: Bootstrap = new Bootstrap()
    .group(workerGroup)
    .channel(socketChannel)

  def createLocalServerBootstrap: ServerBootstrap = new ServerBootstrap()
    .group(bossGroup, workerGroup)
    .channel(serverSocketChannel)

  val delay: (Runnable, Long, TimeUnit) => ScheduledFuture[_] = workerGroup.schedule
}
