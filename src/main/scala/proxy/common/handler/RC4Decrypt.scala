package proxy.common.handler

import java.util

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import proxy.common.RC4

class RC4Decrypt(rc4: RC4) extends MessageToMessageDecoder[Array[Byte]] {

  val decrypt: Array[Byte] => Array[Byte] = rc4.getDecryptF

  override def decode(ctx: ChannelHandlerContext, msg: Array[Byte], out: util.List[Object]): Unit = {
    out.add(decrypt(msg))
  }
}
