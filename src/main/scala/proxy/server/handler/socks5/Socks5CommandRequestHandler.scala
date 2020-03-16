package proxy.server.handler.socks5

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.socksx.v5._
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.concurrent.GenericFutureListener
import proxy.Factory
import proxy.common.Commons

@Sharable
object Socks5CommandRequestHandler extends SimpleChannelInboundHandler[DefaultSocks5CommandRequest] {

  override def channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5CommandRequest): Unit =
    if (msg.`type`().equals(Socks5CommandType.CONNECT)) {
      val connectListener: GenericFutureListener[ChannelFuture] = future => {

        val commandResponse = if (future.isSuccess) {
          ctx.pipeline().addLast(getChannelInbound(future.channel()))
          new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4)
        } else {
          new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4)
        }
        ctx.writeAndFlush(commandResponse)
      }

      val tcpInitializer: ChannelInitializer[SocketChannel] = socketChannel => socketChannel.pipeline()
        .addLast(new ReadTimeoutHandler(Commons.readTimeOut))
        .addLast(getChannelInbound(ctx.channel()))

      Factory.createTcpBootstrap
        .handler(tcpInitializer)
        .connect(msg.dstAddr(), msg.dstPort())
        .addListener(connectListener)
    }

  def getChannelInbound(dst: Channel): ChannelInboundHandlerAdapter = new ChannelInboundHandlerAdapter {
    override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
      dst.writeAndFlush(msg)
    }

    override def channelInactive(ctx: ChannelHandlerContext): Unit = dst.close()

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
  }
}
