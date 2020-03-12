package proxy.common

import java.nio.charset.StandardCharsets

import io.netty.buffer.{ByteBuf, ByteBufUtil, Unpooled}
import io.netty.channel.{Channel, ChannelHandlerContext}

object Convert {

  implicit def channelToChannelId(channel: Channel): String = channel.id().asShortText()

  implicit def channelToChannelId(ctx: ChannelHandlerContext): String = ctx.channel()

  implicit def byteBufToByteArray(byteBuf: ByteBuf): Array[Byte] = ByteBufUtil.getBytes(byteBuf)

  implicit def byteArrayToByteBuf(bytes: Array[Byte]): ByteBuf = Unpooled.wrappedBuffer(bytes)

  implicit class ArrayConvert(array: Array[Byte]) {

    def -(str: String): Array[Byte] = array ++ str.getBytes(StandardCharsets.UTF_8)
  }

  implicit class ByteBufConvert(msg: ByteBuf) {

    def getMessageType: Byte = msg.getByte(0)

    def getChannelId: String = msg.getCharSequence(1, 8, StandardCharsets.UTF_8).toString

    def getData: Array[Byte] = ByteBufUtil.getBytes(msg, 9, msg.capacity - 9)
  }

}
