package proxy.client.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.client.ClientCatch
import proxy.common.{Message, RC4}

@Sharable
class ClientProxyHandler(rc4: RC4) extends SimpleChannelInboundHandler[ByteBuf] {

  override def channelActive(ctx: ChannelHandlerContext): Unit = ClientCatch.remoteChannelOption.foreach {
    implicit val channelId: String = ctx.channel().id().asShortText()
    val data = Message.connectMessageTemplate(ctx.alloc().buffer())
    _.writeAndFlush(data)
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = ClientCatch.remoteChannelOption.foreach {
    implicit val channelId: String = ctx.channel().id().asShortText()
    val data = Message.disconnectMessageTemplate(ctx.alloc().buffer())
    _.writeAndFlush(data)
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    implicit val channelId: String = ctx.channel().id().asShortText()
    //TODO
    //    rc4.encryptMessage()
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
