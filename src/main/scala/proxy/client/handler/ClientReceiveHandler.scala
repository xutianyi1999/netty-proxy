package proxy.client.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.client.ClientCacheFactory

@Sharable
class ClientReceiveHandler extends SimpleChannelInboundHandler[ByteBuf] {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    val channel = ctx.channel()
    ClientCacheFactory.channelMap.put(channel.id().asShortText(), channel)
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {

  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
