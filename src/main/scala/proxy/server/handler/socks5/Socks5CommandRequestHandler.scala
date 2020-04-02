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
          ctx.pipeline().addLast(new InboundHandler(future.channel()))
          Socks5CommandStatus.SUCCESS
        } else {
          Socks5CommandStatus.FAILURE
        }

        ctx.writeAndFlush(new DefaultSocks5CommandResponse(res, Socks5AddressType.IPv4))
      }

      val tcpInitializer: ChannelInitializer[SocketChannel] = socketChannel => {
        val dst = ctx.channel()

        val handler = new InboundHandler(dst) {
          override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
            dst.writeAndFlush(msg)
            Commons.trafficShaping(dst, ctx.channel())
          }
        }

        socketChannel.pipeline()
          .addLast(new ReadTimeoutHandler(Commons.readTimeOut))
          .addLast(handler)
      }

      Factory.createTcpBootstrap
        .handler(tcpInitializer)
        .connect(msg.dstAddr(), msg.dstPort())
        .addListener(connectListener)
    } else {
      ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.COMMAND_UNSUPPORTED, Socks5AddressType.IPv4))
    }

  class InboundHandler(dst: Channel) extends ChannelInboundHandlerAdapter {

    import proxy.common.Convert.ChannelImplicit

    override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = dst.writeAndFlush(msg)

    override def channelInactive(ctx: ChannelHandlerContext): Unit = dst.safeClose()

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = Commons.printError(cause)
  }

}
