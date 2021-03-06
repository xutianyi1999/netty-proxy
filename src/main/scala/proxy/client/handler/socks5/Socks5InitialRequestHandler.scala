package proxy.client.handler.socks5

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.socksx.v5.{DefaultSocks5InitialRequest, DefaultSocks5InitialResponse, Socks5AuthMethod}
import proxy.common.Commons

@Sharable
object Socks5InitialRequestHandler extends SimpleChannelInboundHandler[DefaultSocks5InitialRequest] {

  private val initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH)

  override def channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5InitialRequest): Unit = {
    val res = msg.decoderResult()

    if (res.isSuccess) {
      ctx.writeAndFlush(initialResponse)
    } else {
      ctx.close()
      Commons.printError(res.cause)
    }
  }
}
