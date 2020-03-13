package proxy.client.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.common._

@Sharable
class ClientMuxHandler(disconnectListener: () => Unit,
                       write: (String, => Array[Byte]) => Unit,
                       close: CloseInfo => Unit) extends SimpleChannelInboundHandler[ByteBuf] {

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    close(CloseAll)
    disconnectListener()
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    import proxy.common.Convert.ByteBufConvert

    val messageType = msg.getMessageType
    val channelId = msg.getChannelId

    messageType match {
      case Message.disconnect => close(CloseOne(channelId))
      case Message.data => write(channelId, msg.getData)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
