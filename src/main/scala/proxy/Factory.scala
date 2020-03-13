package proxy

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.{EpollEventLoopGroup, EpollServerSocketChannel, EpollSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.socket.{ServerSocketChannel, SocketChannel}

object Factory {
  val isLinux: Boolean = System.getProperty("os.name").contains("Linux")

  private val tuple = if (isLinux) {
    println("Epoll transport")
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
}
