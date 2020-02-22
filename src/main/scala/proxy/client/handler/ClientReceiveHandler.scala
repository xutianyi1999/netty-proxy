package proxy.client.handler

import java.nio.charset.StandardCharsets

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.Message
import proxy.client.ClientCacheFactory
import proxy.core.Factory

@Sharable
class ClientReceiveHandler extends SimpleChannelInboundHandler[ByteBuf] {

  override def channelActive(ctx: ChannelHandlerContext): Unit = if (ClientCacheFactory.mainChannelOption.isDefined) {
    val channel = ctx.channel()
    val channelId = channel.id().asShortText()

    ClientCacheFactory.channelMap.put(channelId, channel)
    val message = ctx.alloc().buffer()

    message
      .writeByte(Message.connect)
      .writeCharSequence(channelId, StandardCharsets.UTF_8)

    message.writeBytes(Factory.delimiter)

    ClientCacheFactory.mainChannelOption.get.writeAndFlush(message)
  } else {
    ctx.close()
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = ClientCacheFactory.mainChannelOption.foreach {
    val message = ctx.alloc().buffer()

    message
      .writeByte(Message.data)
      .writeCharSequence(ctx.channel().id().asShortText(), StandardCharsets.UTF_8)

    message
      .writeBytes(msg)
      .writeBytes(Factory.delimiter)

    _.writeAndFlush(message)
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    val channelId = ctx.channel().id().asShortText()
    val channel = ClientCacheFactory.channelMap.remove(channelId)

    if (channel != null) ClientCacheFactory.mainChannelOption.foreach {
      val message = ctx.alloc().buffer()

      message
        .writeByte(Message.disconnect)
        .writeCharSequence(channelId, StandardCharsets.UTF_8)

      message.writeBytes(Factory.delimiter)
      _.writeAndFlush(message)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
