package proxy.client.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.client.ClientCatch
import proxy.common.Convert._
import proxy.common.{Message, RC4}

@Sharable
class ClientProxyHandler(rc4: RC4,
                         connectListener: (String, Channel) => Unit,
                         disconnectListener: String => Unit) extends SimpleChannelInboundHandler[ByteBuf] {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    implicit val channelId: String = ctx
    connectListener(channelId, ctx.channel())

    ClientCatch.remoteChannelOption.foreach {
      val data = Message.connectMessageTemplate(ctx.alloc().buffer())
      _.writeAndFlush(data)
    }
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    implicit val channelId: String = ctx

    if (ClientCatch.map.containsKey(channelId)) {
      disconnectListener(channelId)

      ClientCatch.remoteChannelOption.foreach {
        val data = Message.disconnectMessageTemplate(ctx.alloc().buffer())
        _.writeAndFlush(data)
      }
    }
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = ClientCatch.remoteChannelOption.foreach {
    val data = Message.dataMessageTemplate(ctx.alloc().buffer(), msg)(ctx)
    _.writeAndFlush(data)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
