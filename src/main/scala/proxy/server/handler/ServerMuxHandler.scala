package proxy.server.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.common.`case`.{MessageConnect, MessageData, MessageDisconnect}
import proxy.common.{Commons, Message}
import proxy.server.ServerChildChannel

import scala.collection.mutable

class ServerMuxHandler extends SimpleChannelInboundHandler[Array[Byte]] {

  private val map: mutable.Map[String, ServerChildChannel] = mutable.Map.empty

  override def channelInactive(ctx: ChannelHandlerContext): Unit = map.values.foreach(_.close())

  override def channelRead0(ctx: ChannelHandlerContext, msg: Array[Byte]): Unit = Message.messageMatch(msg) {
    case MessageConnect(remoteChannelId) =>
      val write: (ByteBuf, Channel) => Unit = (byteBuf, readChannel) => {
        import proxy.common.Convert.ByteBufConvert.byteBufToByteArray

        val data = Message.dataMessageTemplate(byteBuf)(remoteChannelId)
        ctx.writeAndFlush(data)
        Commons.trafficShaping(ctx.channel, readChannel)
      }

      val disconnectListener: () => Unit = () => {
        map.remove(remoteChannelId)
        ctx.writeAndFlush(Message.disconnectMessageTemplate(remoteChannelId))
      }

      val childChannel = new ServerChildChannel(write, disconnectListener, ctx.channel().eventLoop())
      map.put(remoteChannelId, childChannel)

    case MessageDisconnect(remoteChannelId) => map.remove(remoteChannelId).foreach(_.close())

    case MessageData(remoteChannelId, f) => map.get(remoteChannelId).foreach(_.writeToLocal(f()))
  }
}
