package proxy.common

import java.nio.charset.StandardCharsets

import io.netty.buffer.{ByteBuf, ByteBufUtil, Unpooled}
import io.netty.channel.{Channel, ChannelHandlerContext}

object Convert {

  object ByteBufConvert {
    implicit def byteBufToByteArray(byteBuf: ByteBuf): Array[Byte] = ByteBufUtil.getBytes(byteBuf)

    implicit def byteArrayToByteBuf(bytes: Array[Byte]): ByteBuf = Unpooled.wrappedBuffer(bytes)
  }

  object ChannelIdConvert {
    implicit def channelToChannelId(channel: Channel): String = channel.id().asShortText()

    implicit def channelToChannelId(ctx: ChannelHandlerContext): String = ctx.channel()
  }

  implicit class ArrayConvert(array: Array[Byte]) {

    def -(str: String): Array[Byte] = array ++ str.getBytes(StandardCharsets.UTF_8)
  }

  implicit class MessageConvert(msg: Array[Byte]) {

    def getMessageType: Byte = msg(0)

    def getChannelId: String = new String(msg.slice(1, 9), StandardCharsets.UTF_8)

    def getData: Array[Byte] = msg.slice(9, msg.length)
  }

}
