package proxy.server

import io.netty.buffer.ByteBuf
import io.netty.channel.local.LocalChannel
import io.netty.channel.{ChannelFuture, ChannelHandlerContext, ChannelInitializer, SimpleChannelInboundHandler}
import io.netty.handler.codec.bytes.ByteArrayEncoder
import io.netty.util.concurrent.GenericFutureListener
import proxy.Factory
import proxy.common.Commons

class ServerChildChannel(write: ByteBuf => Unit, closeListener: () => Unit) {

  private var isInitiativeClose = false

  private val localInitializer: ChannelInitializer[LocalChannel] = localChannel => localChannel.pipeline()
    .addLast(new ByteArrayEncoder)
    .addLast {
      new SimpleChannelInboundHandler[ByteBuf] {
        override def channelInactive(ctx: ChannelHandlerContext): Unit = if (!isInitiativeClose) closeListener()

        override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = write(msg)

        override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
      }
    }

  private val channelFuture = Factory.createLocalBootstrap
    .handler(localInitializer)
    .connect(Commons.localAddress)

  private val channel = channelFuture.channel()

  private val connectListener: GenericFutureListener[ChannelFuture] = future =>
    if (future.isSuccess) {
      future.channel().flush()
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
