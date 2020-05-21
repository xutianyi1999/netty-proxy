package proxy.client.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.handler.codec.socksx.v5._
import proxy.client.ClientMuxChannel
import proxy.common.Commons

@Sharable
class ClientProxyHandler(getClientMuxChannel: () => Option[ClientMuxChannel]) extends SimpleChannelInboundHandler[DefaultSocks5CommandRequest] {

  val success = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4)
  val failure = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4)
  val unsupported = new DefaultSocks5CommandResponse(Socks5CommandStatus.COMMAND_UNSUPPORTED, Socks5AddressType.IPv4)

  override def channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5CommandRequest): Unit = {
    val res = msg.decoderResult()

    if (res.isSuccess)
      if (msg.`type`().equals(Socks5CommandType.CONNECT)) {
        def fun(): Unit = getClientMuxChannel() match {
          case Some(clientMuxChannel) =>
            val f: Boolean => Unit = if (_) {
              ctx.pipeline().addLast(new InboundHandler(clientMuxChannel))
              ctx.writeAndFlush(success)
            } else {
              fun()
            }
            clientMuxChannel.register(ctx.channel(), msg.dstAddr(), msg.dstPort(), f)

          case None =>
            ctx.writeAndFlush(failure).addListener(ChannelFutureListener.CLOSE)
            Commons.log.error("Connection pool is empty")
        }

        fun()
      } else
        ctx.writeAndFlush(unsupported).addListener(ChannelFutureListener.CLOSE)
    else
      ctx.writeAndFlush(failure).addListener(ChannelFutureListener.CLOSE)
  }
}

class InboundHandler(clientMuxChannel: ClientMuxChannel) extends SimpleChannelInboundHandler[ByteBuf] {

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    import proxy.common.Convert.ChannelIdConvert.channelToChannelId
    clientMuxChannel.remove(ctx)
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    import proxy.common.Convert.ByteBufConvert.byteBufToByteArray
    clientMuxChannel.writeToRemote(msg, ctx.channel())
  }
}
