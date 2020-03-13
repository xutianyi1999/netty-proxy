package proxy

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.channel.epoll.{EpollEventLoopGroup, EpollServerSocketChannel, EpollSocketChannel}
import io.netty.channel.local.{LocalChannel, LocalServerChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.socket.{ServerSocketChannel, SocketChannel}
import io.netty.channel.{DefaultEventLoopGroup, EventLoopGroup}

object Factory {
  val isLinux: Boolean = System.getProperty("os.name").contains("Linux")

  private val tuple = if (isLinux) {
    println("Epoll transport")
    (new EpollEventLoopGroup(), new EpollEventLoopGroup(), classOf[EpollServerSocketChannel], classOf[EpollSocketChannel])
  } else {
    (new NioEventLoopGroup(), new NioEventLoopGroup(), classOf[NioServerSocketChannel], classOf[NioSocketChannel])
  }

  val bossGroup: EventLoopGroup = tuple._1
  val workerGroup: EventLoopGroup = tuple._2
  val localBossGroup: DefaultEventLoopGroup = new DefaultEventLoopGroup
  val localWorkerGroup: DefaultEventLoopGroup = new DefaultEventLoopGroup
  val serverSocketChannel: Class[_ <: ServerSocketChannel] = tuple._3
  val socketChannel: Class[_ <: SocketChannel] = tuple._4

  def createTcpBootstrap: Bootstrap = new Bootstrap()
    .group(workerGroup)
    .channel(socketChannel)

  def createTcpServerBootstrap: ServerBootstrap = new ServerBootstrap()
    .group(bossGroup, workerGroup)
    .channel(serverSocketChannel)

  def createLocalBootstrap: Bootstrap = new Bootstrap()
    .group(localWorkerGroup)
    .channel(classOf[LocalChannel])

  def createLocalServerBootstrap: ServerBootstrap = new ServerBootstrap()
    .group(localBossGroup, localWorkerGroup)
    .channel(classOf[LocalServerChannel])
}
