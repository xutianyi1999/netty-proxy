package proxy.client.handler

import java.nio.charset.StandardCharsets

import io.netty.buffer.{ByteBuf, ByteBufUtil}
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.client.ClientCacheFactory
import proxy.core.{Factory, Message}

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
    val capacity = msg.capacity()

    if (capacity > 0) {
      val messageType = msg.getByte(0)
      val channelId = msg.getCharSequence(1, 8, StandardCharsets.UTF_8).toString

      val channel = ClientCacheFactory.channelMap.get(channelId)

      if (channel != null) messageType match {
        case Message.disconnect =>
          ClientCacheFactory.channelMap.remove(channelId)
          channel.close

        case Message.data =>
          val data = Factory.cipher.decrypt(ByteBufUtil.getBytes(msg, 9, capacity - 9))
          val buf = ctx.alloc().buffer().writeBytes(data)
          channel.writeAndFlush(buf)
      }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
