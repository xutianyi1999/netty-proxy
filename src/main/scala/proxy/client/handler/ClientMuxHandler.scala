package proxy.client.handler

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.timeout.IdleStateEvent
import proxy.common._
import proxy.common.`case`._

class ClientMuxHandler(disconnectListener: () => Unit,
                       write: (String, => Array[Byte]) => Unit,
                       close: CloseCase => Unit) extends SimpleChannelInboundHandler[Array[Byte]] {

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    disconnectListener()
    close(CloseAll)
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: Array[Byte]): Unit = Message.messageMatch(msg) {
    case MessageDisconnect(channelId) => close(CloseOne(channelId))
    case MessageData(channelId, f) => write(channelId, f())
    case _ =>
  }

  override def userEventTriggered(ctx: ChannelHandlerContext, evt: Object): Unit =
    if (evt.isInstanceOf[IdleStateEvent]) {
      ctx.writeAndFlush(Message.heartbeatTemplate)
    }
}
