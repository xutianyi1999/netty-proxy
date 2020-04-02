package proxy.server

import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.util.concurrent.GenericFutureListener
import proxy.LocalTransportFactory
import proxy.common.Commons

class ServerChildChannel(write: (ByteBuf, Channel) => Unit, closeListener: () => Unit) {

  @volatile private var isInitiativeClose = false

  private val channelFuture = LocalTransportFactory.createLocalBootstrap
    .handler {
      new SimpleChannelInboundHandler[ByteBuf] {
        override def channelInactive(ctx: ChannelHandlerContext): Unit = if (!isInitiativeClose) closeListener()

        override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = write(msg, ctx.channel())

        override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = Commons.printError(cause)
      }
    }.connect(Commons.localAddress)

  private val channel = channelFuture.channel()

  private val connectListener: GenericFutureListener[ChannelFuture] = future =>
    if (future.isSuccess) {
      channel.flush()
    } else {
      Commons.log.severe(future.cause().getMessage)
      closeListener()
    }

  channelFuture.addListener(connectListener)

  def writeToLocal(msg: Array[Byte]): Unit = {
    def f(f2: () => Unit): Unit =
      if (channel.isActive)
        channel.writeAndFlush(msg)
      else
        f2()

    f { () =>
      channel.eventLoop().execute { () =>
        f(() => channel.write(msg))
      }
    }
  }

  def close(): Unit = {
    import proxy.common.Convert.ChannelImplicit
    isInitiativeClose = true
    channel.safeClose()
  }
}
