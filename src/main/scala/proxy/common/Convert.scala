package proxy.common

import io.netty.buffer.{ByteBuf, ByteBufUtil, Unpooled}
import io.netty.channel.{Channel, ChannelHandlerContext}

object Convert {

  implicit def channelToChannelId(channel: Channel): String = channel.id().asShortText()

  implicit def channelToChannelId(ctx: ChannelHandlerContext): String = ctx.channel()

  implicit def byteBufToByteArray(byteBuf: ByteBuf): Array[Byte] = ByteBufUtil.getBytes(byteBuf)

  implicit def byteArrayToByteBuf(bytes: Array[Byte]): ByteBuf = Unpooled.wrappedBuffer(bytes)

  implicit class MapConvert[K, V](map: java.util.Map[K, V]) {

    def getOption(key: K): Option[V] = Option(map.get(key))
  }

}
