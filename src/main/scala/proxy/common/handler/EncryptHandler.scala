package proxy.common.handler

import java.util

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder
import proxy.common.Message
import proxy.common.crypto.CipherTrait

class EncryptHandler(cipher: CipherTrait) extends MessageToMessageEncoder[Array[Byte]] {

  val encrypt: Array[Byte] => Array[Byte] = cipher.getEncryptF

  override def encode(ctx: ChannelHandlerContext, msg: Array[Byte], out: util.List[Object]): Unit = {
    out.add(encrypt(msg) ++ Message.delimiter)
  }
}
