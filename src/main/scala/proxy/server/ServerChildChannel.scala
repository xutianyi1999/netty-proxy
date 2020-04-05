package proxy.server

import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.channel.local.LocalChannel
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.concurrent.GenericFutureListener
import proxy.LocalTransportFactory
import proxy.common.Commons

class ServerChildChannel(write: (ByteBuf, Channel) => Unit, closeListener: () => Unit) {

  @volatile private var isInitiativeClose = false

  private val localInitializer: ChannelInitializer[LocalChannel] = localChannel => localChannel.pipeline()
    .addLast(new ReadTimeoutHandler(Commons.readTimeOut))
    .addLast {
      new SimpleChannelInboundHandler[ByteBuf] {
        override def channelInactive(ctx: ChannelHandlerContext): Unit = if (!isInitiativeClose) closeListener()

        override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = write(msg, ctx.channel())

        override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = Commons.printError(cause)
      }
    }

  private val channelFuture = LocalTransportFactory.createLocalBootstrap
    .handler(localInitializer)
    .connect(Commons.localAddress)

  private val channel = channelFuture.channel()

  private val connectListener: GenericFutureListener[ChannelFuture] = future =>
    if (future.isSuccess) {
      channel.flush()
    } else {
      Commons.printError(future.cause())
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
