package proxy.client.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.client.ClientCatch
import proxy.common.Message

@Sharable
class ClientProxyHandler(connectListener: (String, Channel) => Unit,
                         disconnectListener: String => Option[Channel]) extends SimpleChannelInboundHandler[ByteBuf] {

  import proxy.common.Convert.ChannelIdConvert._

  override def channelActive(ctx: ChannelHandlerContext): Unit = ClientCatch.remoteChannelOption match {
    case Some(channel) =>
      implicit val channelId: String = ctx

      connectListener(channelId, ctx.channel())
      channel.writeAndFlush(Message.connectMessageTemplate)

    case None => ctx.close()
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    implicit val channelId: String = ctx

    if (disconnectListener(channelId).isDefined) ClientCatch.remoteChannelOption.foreach {
      _.writeAndFlush(Message.disconnectMessageTemplate)
    }
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = ClientCatch.remoteChannelOption.foreach {
    import proxy.common.Convert.ByteBufConvert.byteBufToByteArray
    val data = Message.dataMessageTemplate(msg)(ctx)
    _.writeAndFlush(data)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
