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

  override def channelRead0(ctx: ChannelHandlerContext, msg: Array[Byte]): Unit =
    Message.messageMatch(msg)(remoteChannelId => {
      case MessageConnect(address) =>
        val write: (ByteBuf, Channel) => Unit = (byteBuf, readChannel) => {
          import proxy.common.Convert.ByteBufConvert.byteBufToByteArray

          val data = Message.dataMessageTemplate(byteBuf)(remoteChannelId)
          ctx.writeAndFlush(data)
          Commons.trafficShaping(ctx.channel, readChannel)
        }

        val closeListener: () => Unit = () => {
          map.remove(remoteChannelId)
          ctx.writeAndFlush(Message.disconnectMessageTemplate(remoteChannelId))
        }

        val childChannel = new ServerChildChannel(address, write, closeListener, ctx.channel().eventLoop())
        map.put(remoteChannelId, childChannel)

      case MessageDisconnect => map.remove(remoteChannelId).foreach(_.close())

      case MessageData(f) => map.get(remoteChannelId).foreach(_.writeToRemote(f()))
    })
}
