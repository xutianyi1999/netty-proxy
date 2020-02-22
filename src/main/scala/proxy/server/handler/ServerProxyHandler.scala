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

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = childChannelHandlerOption.foreach(childChannelHandler => {
    val messageType = msg.getByte(0)
    val remoteChannelId = msg.getCharSequence(1, 8, StandardCharsets.UTF_8).toString

    messageType match {
      case Message.connect => childChannelHandler.connect(remoteChannelId)
      case Message.disconnect => childChannelHandler.activeDisconnect(remoteChannelId)
      case Message.data => childChannelHandler.writeToChild(remoteChannelId, msg.slice(9, msg.capacity() - 9))
    }
  })

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = cause.printStackTrace()
}
