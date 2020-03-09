package proxy.server.handler

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

import io.netty.buffer.{ByteBuf, ByteBufUtil}
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.common.Convert._
import proxy.common.{Message, RC4}
import proxy.server.ServerChildChannel

class ServerMuxHandler(rc4: RC4) extends SimpleChannelInboundHandler[ByteBuf] {

  private val map: java.util.Map[String, ServerChildChannel] = new ConcurrentHashMap

  override def channelInactive(ctx: ChannelHandlerContext): Unit = map.values().forEach(_.close())

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    val allocator = ctx.alloc()
    val capacity = msg.capacity()

    val messageType = msg.getByte(0)
    implicit val remoteChannelId: String = msg.getCharSequence(1, 8, StandardCharsets.UTF_8).toString

    messageType match {
      case Message.connect =>
        val write = rc4.encryptMessage { ciphertext =>
          val data = Message.dataMessageTemplate(allocator.buffer(), ciphertext)
          ctx.writeAndFlush(data)
        }

        def sendDisconnectMessage(): Unit = {
          val data = Message.disconnectMessageTemplate(allocator.buffer())
          ctx.writeAndFlush(data)
        }

        val childChannel = new ServerChildChannel(write, () => {
          map.remove(remoteChannelId)
          sendDisconnectMessage()
        })

        map.put(remoteChannelId, childChannel)

      case Message.disconnect =>
        val childChannel = map.remove(remoteChannelId)
        if (childChannel != null) childChannel.close()

      case Message.data => map.ifPresent(remoteChannelId) { childChannel =>
        val data = rc4.decrypt(ByteBufUtil.getBytes(msg, 9, capacity - 9))
        childChannel.writeToLocal(allocator.buffer().writeBytes(data))
      }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
