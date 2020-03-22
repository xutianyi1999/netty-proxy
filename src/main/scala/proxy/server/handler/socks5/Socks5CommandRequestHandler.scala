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

        val res = if (future.isSuccess) {
          ctx.pipeline().addLast(localInbound(future.channel()))
          Socks5CommandStatus.SUCCESS
        } else {
          Socks5CommandStatus.FAILURE
        }

        ctx.writeAndFlush(new DefaultSocks5CommandResponse(res, Socks5AddressType.IPv4))
      }

      val tcpInitializer: ChannelInitializer[SocketChannel] = socketChannel => socketChannel.pipeline()
        .addLast(new ReadTimeoutHandler(Commons.readTimeOut))
        .addLast(remoteInbound(ctx.channel()))

      Factory.createTcpBootstrap
        .handler(tcpInitializer)
        .connect(msg.dstAddr(), msg.dstPort())
        .addListener(connectListener)
    } else {
      ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.COMMAND_UNSUPPORTED, Socks5AddressType.IPv4))
    }

  def localInbound(dst: Channel): ChannelInboundHandlerAdapter = new ChannelInboundHandlerAdapter {
    override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
      dst.writeAndFlush(msg)
    }

    override def channelInactive(ctx: ChannelHandlerContext): Unit = dst.close()

    override def channelWritabilityChanged(ctx: ChannelHandlerContext): Unit = {
      dst.config().setAutoRead(ctx.channel().isWritable)
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = Commons.log.severe(cause.getMessage)
  }

  def remoteInbound(dst: Channel): ChannelInboundHandlerAdapter = new ChannelInboundHandlerAdapter {
    override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
      dst.writeAndFlush(msg)
      ctx.channel().config().setAutoRead(dst.isWritable)
    }

    override def channelInactive(ctx: ChannelHandlerContext): Unit = dst.close()

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = Commons.log.severe(cause.getMessage)
  }
}
