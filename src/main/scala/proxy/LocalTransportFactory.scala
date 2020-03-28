package proxy

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.channel.DefaultEventLoopGroup
import io.netty.channel.local.{LocalChannel, LocalServerChannel}

object LocalTransportFactory {

  private val localBossGroup: DefaultEventLoopGroup = new DefaultEventLoopGroup
  private val localWorkerGroup: DefaultEventLoopGroup = new DefaultEventLoopGroup

  def createLocalBootstrap: Bootstrap = new Bootstrap()
    .group(localWorkerGroup)
    .channel(classOf[LocalChannel])

  def createLocalServerBootstrap: ServerBootstrap = new ServerBootstrap()
    .group(localBossGroup, localWorkerGroup)
    .channel(classOf[LocalServerChannel])
}
