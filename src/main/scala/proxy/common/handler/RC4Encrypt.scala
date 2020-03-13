package proxy.common.handler

import java.util

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder
import proxy.common.{Message, RC4}

class RC4Encrypt(rc4: RC4) extends MessageToMessageEncoder[Array[Byte]] {

  val encrypt: Array[Byte] => Array[Byte] = rc4.getEncryptF

  override def encode(ctx: ChannelHandlerContext, msg: Array[Byte], out: util.List[Object]): Unit = {
    out.add(encrypt(msg) ++ Message.delimiter)
  }
}
