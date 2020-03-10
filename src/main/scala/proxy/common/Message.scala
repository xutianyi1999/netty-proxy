package proxy.common

import java.nio.charset.StandardCharsets

import io.netty.buffer.{ByteBuf, ByteBufUtil, Unpooled}

import scala.collection.IterableOnce
import scala.collection.mutable.ArrayBuffer

object Message {

  val connect: Byte = 1
  val disconnect: Byte = 2
  val data: Byte = 3

  val delimiter: Array[Byte] = "¤☀₪".getBytes(StandardCharsets.UTF_8)

  implicit def stringToBytes(str: String): IterableOnce[Byte] = str.getBytes(StandardCharsets.UTF_8)

  def connectMessageTemplate(implicit channelId: String): Array[Byte] = ArrayBuffer()[Byte]
    .addOne(connect)
    .addAll(channelId)
    .addAll(delimiter)
    .toArray

  def disconnectMessageTemplate(implicit channelId: String): ByteBuf = {
    val msg = Unpooled.buffer()

    msg.writeByte(disconnect)
      .writeCharSequence(channelId, StandardCharsets.UTF_8)

    msg.writeBytes(delimiter)
  }

  def dataMessageTemplate(data: Array[Byte])(implicit channelId: String): ByteBuf = {
    val msg = Unpooled.buffer()

    msg.writeByte(Message.data)
      .writeCharSequence(channelId, StandardCharsets.UTF_8)

    msg.writeBytes(data)
      .writeBytes(delimiter)
  }

  def getMessageType(msg: ByteBuf): Byte = msg.getByte(0)

  def getChannelId(msg: ByteBuf): String = msg.getCharSequence(1, 8, StandardCharsets.UTF_8).toString

  def getData(msg: ByteBuf): Array[Byte] = ByteBufUtil.getBytes(msg, 9, msg.capacity - 9)
}
