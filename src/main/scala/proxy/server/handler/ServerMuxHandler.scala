package proxy.server.handler

import java.util.concurrent.ConcurrentHashMap

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.common.Convert._
import proxy.common.{Message, RC4}
import proxy.server.ServerChildChannel

class ServerMuxHandler(rc4: RC4) extends SimpleChannelInboundHandler[ByteBuf] {

  private val map: java.util.Map[String, ServerChildChannel] = new ConcurrentHashMap

  override def channelInactive(ctx: ChannelHandlerContext): Unit = map.values().forEach(_.close())

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    val allocator = ctx.alloc()

    val messageType = Message.getMessageType(msg)
    implicit val remoteChannelId: String = Message.getChannelId(msg)

    messageType match {
      case Message.connect =>
        val write: ByteBuf => Unit = byteBuf => {
          val data = Message.dataMessageTemplate(allocator.buffer(), rc4.encrypt(byteBuf))
          ctx.writeAndFlush(data)
        }

        def sendDisconnectMessage(): Unit = {
          val data = Message.disconnectMessageTemplate(allocator.buffer())
          ctx.writeAndFlush(data)
        }

        val disconnectListener = () => {
          map.remove(remoteChannelId)
          sendDisconnectMessage()
        }

        val childChannel = new ServerChildChannel(write, disconnectListener)
        map.put(remoteChannelId, childChannel)

      case Message.disconnect =>
        val childChannel = map.remove(remoteChannelId)
        if (childChannel != null) childChannel.close()

      case Message.data => map.getOption(remoteChannelId).foreach {
        val data = rc4.decrypt(Message.getData(msg))
        _.writeToLocal(allocator.buffer().writeBytes(data))
      }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
