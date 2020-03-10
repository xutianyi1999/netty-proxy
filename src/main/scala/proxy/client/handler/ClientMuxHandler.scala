package proxy.client.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.common.{Message, RC4}

@Sharable
class ClientMuxHandler(rc4: RC4,
                       disconnectListener: () => Unit,
                       write: (String, => Array[Byte]) => Unit,
                       close: String => Unit) extends SimpleChannelInboundHandler[ByteBuf] {

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    close("all")
    disconnectListener()
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    val messageType = Message.getMessageType(msg)
    val channelId = Message.getChannelId(msg)

    messageType match {
      case Message.disconnect => close(channelId)
      case Message.data =>
        lazy val data = rc4.decrypt(Message.getData(msg))
        write(channelId, data)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
