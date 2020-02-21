package proxy.server.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

@Sharable
class ServerChildProxyHandler(writeFunction: (String, ByteBuf) => Unit,
                              disconnectionFunction: String => Unit) extends SimpleChannelInboundHandler[ByteBuf] {

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    writeFunction(ctx.channel().id().asShortText(), msg)
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = disconnectionFunction(ctx.channel().id().asShortText())

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
