package proxy.common.handler

import java.util

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder

@Sharable
object ByteArrayToByteBufDecoder extends MessageToMessageDecoder[Array[Byte]] {

  override def decode(ctx: ChannelHandlerContext, msg: Array[Byte], out: util.List[Object]): Unit = {
    out.add(Unpooled.wrappedBuffer(msg))
  }
}
