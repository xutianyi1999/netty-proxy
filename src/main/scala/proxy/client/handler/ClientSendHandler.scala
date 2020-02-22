package proxy.client.handler

import java.nio.charset.StandardCharsets

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.Message
import proxy.client.ClientCacheFactory

@Sharable
class ClientSendHandler(connect: () => Unit) extends SimpleChannelInboundHandler[ByteBuf] {

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    ClientCacheFactory.mainChannelOption = Option.empty

    val channels = ClientCacheFactory.channelMap.values()
    ClientCacheFactory.channelMap.clear()
    channels.forEach(_.close(): Unit)

    connect()
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    val messageType = msg.getByte(0)
    val channelId = msg.getCharSequence(1, 9, StandardCharsets.UTF_8).toString

    val channel = ClientCacheFactory.channelMap.get(channelId)

    if (channel != null) messageType match {
      case Message.disconnect =>
        ClientCacheFactory.channelMap.remove(channelId)
        channel.close

      case Message.data => channel.writeAndFlush(msg.slice(9, msg.capacity()))
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
