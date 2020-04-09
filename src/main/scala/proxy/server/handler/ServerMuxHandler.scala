package proxy.server.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.common.{Commons, Message}
import proxy.server.ServerChildChannel

import scala.collection.mutable

class ServerMuxHandler extends SimpleChannelInboundHandler[Array[Byte]] {

  private val map: mutable.Map[String, ServerChildChannel] = mutable.Map.empty

  override def channelInactive(ctx: ChannelHandlerContext): Unit = map.values.foreach(_.close())

  override def channelRead0(ctx: ChannelHandlerContext, msg: Array[Byte]): Unit = {
    import proxy.common.Convert.ByteBufConvert.byteBufToByteArray
    import proxy.common.Convert.MessageConvert

    val messageType = msg.getMessageType
    implicit val remoteChannelId: String = msg.getChannelId

    messageType match {
      case Message.connect =>
        val write: (ByteBuf, Channel) => Unit = (byteBuf, readChannel) => {
          val data = Message.dataMessageTemplate(byteBuf)
          ctx.writeAndFlush(data)
          Commons.trafficShaping(ctx.channel(), readChannel)
        }

        val disconnectListener: () => Unit = () => {
          map.remove(remoteChannelId)
          ctx.writeAndFlush(Message.disconnectMessageTemplate)
        }

        val childChannel = new ServerChildChannel(write, disconnectListener, ctx.channel().eventLoop())
        map.put(remoteChannelId, childChannel)

      case Message.disconnect => map.remove(remoteChannelId).foreach(_.close())

      case Message.data => map.get(remoteChannelId).foreach(_.writeToLocal(msg.getData))
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = Commons.printError(cause)
}
