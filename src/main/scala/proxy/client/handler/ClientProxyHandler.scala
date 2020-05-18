package proxy.client.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.handler.codec.socksx.v5._
import proxy.client.ClientMuxChannel

@Sharable
class ClientProxyHandler(getClientMuxChannel: () => ClientMuxChannel) extends SimpleChannelInboundHandler[DefaultSocks5CommandRequest] {

  val success = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4)
  val failure = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4)
  val unsupported = new DefaultSocks5CommandResponse(Socks5CommandStatus.COMMAND_UNSUPPORTED, Socks5AddressType.IPv4)

  override def channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5CommandRequest): Unit = {
    val res = msg.decoderResult()

    if (res.isSuccess)
      if (msg.`type`().equals(Socks5CommandType.CONNECT)) {
        val clientMuxChannel = getClientMuxChannel()

        val f: Boolean => Unit = if (_) {
          ctx.pipeline().addLast(new InboundHandler(clientMuxChannel))
          ctx.writeAndFlush(success)
        } else {
          ctx.writeAndFlush(failure).addListener(ChannelFutureListener.CLOSE)
        }

        clientMuxChannel.register(ctx.channel(), msg.dstAddr(), msg.dstPort(), f)
      } else
        ctx.writeAndFlush(unsupported).addListener(ChannelFutureListener.CLOSE)
    else
      ctx.writeAndFlush(failure).addListener(ChannelFutureListener.CLOSE)
  }
}

class InboundHandler(clientMuxChannel: ClientMuxChannel) extends ChannelInboundHandlerAdapter {

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    import proxy.common.Convert.ChannelIdConvert.channelToChannelId
    clientMuxChannel.remove(ctx)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
    import proxy.common.Convert.ByteBufConvert.byteBufToByteArray
    val data = msg.asInstanceOf[ByteBuf]
    clientMuxChannel.writeToRemote(data, ctx.channel())
  }
}
