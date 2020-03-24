package proxy

import java.util.concurrent.TimeUnit

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.channel.DefaultEventLoopGroup
import io.netty.channel.local.{LocalChannel, LocalServerChannel}
import io.netty.util.concurrent.ScheduledFuture

object LocalTransportFactory {

  private val localBossGroup: DefaultEventLoopGroup = new DefaultEventLoopGroup
  private val localWorkerGroup: DefaultEventLoopGroup = new DefaultEventLoopGroup

  def createLocalBootstrap: Bootstrap = new Bootstrap()
    .group(localWorkerGroup)
    .channel(classOf[LocalChannel])

  def createLocalServerBootstrap: ServerBootstrap = new ServerBootstrap()
    .group(localBossGroup, localWorkerGroup)
    .channel(classOf[LocalServerChannel])

  val delay: (Runnable, Long, TimeUnit) => ScheduledFuture[_] = localWorkerGroup.schedule
}
