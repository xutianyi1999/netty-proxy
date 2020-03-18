package proxy.server.handler

import java.util.concurrent.ConcurrentHashMap

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.common.Message
import proxy.server.ServerChildChannel

import scala.collection.JavaConverters._
import scala.collection.mutable

class ServerMuxHandler extends SimpleChannelInboundHandler[Array[Byte]] {

  private val map: mutable.Map[String, ServerChildChannel] = new ConcurrentHashMap[String, ServerChildChannel].asScala

  override def channelInactive(ctx: ChannelHandlerContext): Unit = map.values.foreach(_.close())

  override def channelRead0(ctx: ChannelHandlerContext, msg: Array[Byte]): Unit = {
    import proxy.common.Convert.ByteBufConvert.byteBufToByteArray
    import proxy.common.Convert.MessageConvert

    val messageType = msg.getMessageType
    implicit val remoteChannelId: String = msg.getChannelId

    messageType match {
      case Message.connect =>
        val write: ByteBuf => Unit = byteBuf => {
          val data = Message.dataMessageTemplate(byteBuf)
          ctx.writeAndFlush(data)
        }

        val disconnectListener: () => Unit = () => {
          map.remove(remoteChannelId)
          ctx.writeAndFlush(Message.disconnectMessageTemplate)
        }

        val childChannel = new ServerChildChannel(ctx.channel().isWritable, write, disconnectListener)
        map.put(remoteChannelId, childChannel)

      case Message.disconnect =>
        val childChannel = map.remove(remoteChannelId)
        childChannel.foreach(_.close())

      case Message.data => map.get(remoteChannelId).foreach {
        _.writeToLocal(msg.getData)
      }
    }
  }

  override def channelWritabilityChanged(ctx: ChannelHandlerContext): Unit = {
    map.foreach(_._2.setAutoRead(ctx.channel().isWritable))
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
