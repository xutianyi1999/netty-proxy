package proxy.common

import io.netty.buffer.{ByteBuf, ByteBufUtil}
import io.netty.channel.{Channel, ChannelHandlerContext}

object Convert {

  object ByteBufConvert {
    implicit def byteBufToByteArray(byteBuf: ByteBuf): Array[Byte] = ByteBufUtil.getBytes(byteBuf)
  }

  object ChannelIdConvert {
    implicit def channelToChannelId(channel: Channel): String = channel.id().asShortText()

    implicit def channelToChannelId(ctx: ChannelHandlerContext): String = ctx.channel()
  }

  implicit class ChannelImplicit(channel: Channel) {

    def safeClose(): Unit = if (channel.isOpen) channel.close()
  }

}
