package proxy.common.handler

import java.util

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import proxy.common.crypto.Cipher

class DecryptHandler(cipher: Cipher) extends MessageToMessageDecoder[Array[Byte]] {

  val decrypt: Array[Byte] => Array[Byte] = cipher.getDecryptF

  override def decode(ctx: ChannelHandlerContext, msg: Array[Byte], out: util.List[Object]): Unit = {
    out.add(decrypt(msg))
  }
}
