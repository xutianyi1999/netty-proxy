package proxy.server.handler.socks5

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.socksx.v5.{DefaultSocks5CommandResponse, _}
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.concurrent.GenericFutureListener
import proxy.Factory
import proxy.common.Commons

@Sharable
object Socks5CommandRequestHandler extends SimpleChannelInboundHandler[DefaultSocks5CommandRequest] {

  val success = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4)
  val failure = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4)
  val unsupported = new DefaultSocks5CommandResponse(Socks5CommandStatus.COMMAND_UNSUPPORTED, Socks5AddressType.IPv4)

  override def channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5CommandRequest): Unit =
    if (msg.`type`().equals(Socks5CommandType.CONNECT)) {
      val connectListener: GenericFutureListener[ChannelFuture] = future => {
        if (future.isSuccess) {
          ctx.pipeline().addLast(new InboundHandler(future.channel()))
          ctx.writeAndFlush(success)
        } else {
          ctx.writeAndFlush(failure).addListener(ChannelFutureListener.CLOSE)
        }
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

      Factory.createTcpBootstrap(ctx.channel().eventLoop())
        .handler(tcpInitializer)
        .connect(msg.dstAddr(), msg.dstPort())
        .addListener(connectListener)
    } else {
      ctx.writeAndFlush(unsupported).addListener(ChannelFutureListener.CLOSE)
    }
}

class InboundHandler(dst: Channel) extends ChannelInboundHandlerAdapter {

  import proxy.common.Convert.ChannelImplicit

  override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = dst.writeAndFlush(msg)

  override def channelInactive(ctx: ChannelHandlerContext): Unit = dst.safeClose()
}
