package proxy.client.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.client.ClientMuxChannel
import proxy.common.{Commons, Message}

class ClientProxyHandler(getClientMuxChannel: () => ClientMuxChannel) extends SimpleChannelInboundHandler[ByteBuf] {

  import proxy.common.Convert.ChannelIdConvert._

  private val clientMuxChannel = getClientMuxChannel()

  override def channelActive(ctx: ChannelHandlerContext): Unit = clientMuxChannel.register(ctx.channel())

  override def channelInactive(ctx: ChannelHandlerContext): Unit = clientMuxChannel.remove(ctx)

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    import proxy.common.Convert.ByteBufConvert.byteBufToByteArray
    clientMuxChannel.writeToRemote(Message.dataMessageTemplate(msg)(ctx), ctx.channel())
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = Commons.log.severe(cause.getMessage)
}
