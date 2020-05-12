package proxy.server

import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.channel.socket.DuplexChannel
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.concurrent.GenericFutureListener
import proxy.Factory
import proxy.common.Commons

class ServerChildChannel(write: (ByteBuf, Channel) => Unit, closeListener: () => Unit, eventLoop: EventLoop) {

  private var isInitiativeClose = false

  private val localInitializer: ChannelInitializer[DuplexChannel] = socketChannel => socketChannel.pipeline()
    .addLast(new ReadTimeoutHandler(Commons.readTimeOut))
    .addLast(Commons.byteArrayEncoder)
    .addLast {
      new SimpleChannelInboundHandler[ByteBuf] {
        override def channelInactive(ctx: ChannelHandlerContext): Unit = if (!isInitiativeClose) closeListener()

        override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = write(msg, ctx.channel())
      }
    }

  private val channelFuture = Factory.createLocalBootstrap(eventLoop)
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

  def writeToLocal(msg: Array[Byte]): Unit =
    if (channel.isActive)
      channel.writeAndFlush(msg)
    else
      channel.write(msg)

  def close(): Unit = {
    import proxy.common.Convert.ChannelImplicit
    isInitiativeClose = true
    channel.safeClose()
  }
}
