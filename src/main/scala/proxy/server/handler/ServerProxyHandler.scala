package proxy.server.handler

import java.nio.charset.StandardCharsets

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import proxy.Message

class ServerProxyHandler(getBootstrap: () => Bootstrap) extends SimpleChannelInboundHandler[ByteBuf] {

  var childChannelHandlerOption = Option.empty[ServerChildChannelHandler]

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    childChannelHandlerOption = Option(new ServerChildChannelHandler(getBootstrap(), ctx.channel()))
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = childChannelHandlerOption.foreach(_.disconnectAll())

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    val capacity = msg.capacity()

    if (capacity > 0) childChannelHandlerOption.foreach(childChannelHandler => {
      val messageType = msg.getByte(0)
      val remoteChannelId = msg.getCharSequence(1, 8, StandardCharsets.UTF_8).toString

      messageType match {
        case Message.connect => childChannelHandler.connect(remoteChannelId)
        case Message.disconnect => childChannelHandler.activeDisconnect(remoteChannelId)
        case Message.data =>
          val buf = ctx.alloc().buffer(capacity - 9)
          msg.getBytes(9, buf)
          childChannelHandler.writeToChild(remoteChannelId, buf)
      }
    })
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
