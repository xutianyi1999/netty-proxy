package proxy.common

import java.nio.charset.StandardCharsets

import io.netty.buffer.ByteBuf

object Message {

  val connect: Byte = 1
  val disconnect: Byte = 2
  val data: Byte = 3

  val delimiter: Array[Byte] = "¤☀₪".getBytes(StandardCharsets.UTF_8)

  def connectMessageTemplate(emptyMsg: ByteBuf)(implicit channelId: String): ByteBuf = {
    val msg = emptyMsg
    msg.writeByte(connect)
      .writeCharSequence(channelId, StandardCharsets.UTF_8)
    msg.writeBytes(delimiter)
  }

  def disconnectMessageTemplate(emptyMsg: ByteBuf)(implicit channelId: String): ByteBuf = {
    val msg = emptyMsg
    msg.writeByte(disconnect)
      .writeCharSequence(channelId, StandardCharsets.UTF_8)
    msg.writeBytes(delimiter)
  }

  def dataMessageTemplate(emptyMsg: ByteBuf, data: Array[Byte])(implicit channelId: String): ByteBuf = {
    val msg = emptyMsg
    msg.writeByte(Message.data)
      .writeCharSequence(channelId, StandardCharsets.UTF_8)

    msg.writeBytes(data)
      .writeBytes(delimiter)
  }
}
