package proxy.server

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelFuture, ChannelHandlerContext, ChannelOption, SimpleChannelInboundHandler}
import io.netty.util.concurrent.GenericFutureListener
import proxy.LocalTransportFactory
import proxy.common.Commons

class ServerChildChannel(isWriteable: Boolean, write: ByteBuf => Unit, closeListener: () => Unit) {

  @volatile private var isInitiativeClose = false

  private val channelFuture = LocalTransportFactory.createLocalBootstrap
    .option[java.lang.Boolean](ChannelOption.AUTO_READ, isWriteable)
    .handler {
      new SimpleChannelInboundHandler[ByteBuf] {
        override def channelInactive(ctx: ChannelHandlerContext): Unit = if (!isInitiativeClose) closeListener()

        override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = write(msg)

        override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = Commons.log.severe(cause.getMessage)
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

  def writeToLocal(msg: Array[Byte]): Unit =
    if (channel.isActive)
      channel.writeAndFlush(msg)
    else
      channel.write(msg)

  def close(): Unit = {
    isInitiativeClose = true
    channel.close()
  }

  def setAutoRead(flag: Boolean): Unit = channel.config().setAutoRead(flag)
}
