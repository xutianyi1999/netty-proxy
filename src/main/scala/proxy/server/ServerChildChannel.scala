package proxy.server

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelFuture, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.util.concurrent.GenericFutureListener
import proxy.LocalTransportFactory
import proxy.common.Commons

class ServerChildChannel(write: ByteBuf => Unit, closeListener: () => Unit) {

  private var isInitiativeClose = false

  private val channelFuture = LocalTransportFactory.createLocalBootstrap
    .handler {
      new SimpleChannelInboundHandler[ByteBuf] {
        override def channelInactive(ctx: ChannelHandlerContext): Unit = if (!isInitiativeClose) closeListener()

        override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = write(msg)

        override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
      }
    }.connect(Commons.localAddress)

  private val channel = channelFuture.channel()

  private val connectListener: GenericFutureListener[ChannelFuture] = future =>
    if (future.isSuccess) {
      channel.flush()
    } else {
      future.cause().printStackTrace()
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
}
