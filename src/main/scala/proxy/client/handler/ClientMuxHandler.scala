package proxy.client.handler

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.common._

class ClientMuxHandler(disconnectListener: () => Unit,
                       write: (String, => Array[Byte]) => Unit,
                       close: CloseInfo => Unit) extends SimpleChannelInboundHandler[Array[Byte]] {

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    disconnectListener()
    close(CloseAll)
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: Array[Byte]): Unit = {
    import proxy.common.Convert.MessageConvert

    val messageType = msg.getMessageType
    val channelId = msg.getChannelId

    messageType match {
      case Message.disconnect => close(CloseOne(channelId))
      case Message.data => write(channelId, msg.getData)
    }
  }
}
