package proxy.common

import java.nio.charset.StandardCharsets

object Message {

  val connect: Byte = 1
  val disconnect: Byte = 2
  val data: Byte = 3

  val delimiter: Array[Byte] = "🍔🍟".getBytes(StandardCharsets.UTF_8)

  import proxy.common.Convert.MessageConvert

  def connectMessageTemplate(implicit channelId: String): Array[Byte] = {
    Array[Byte](connect) - channelId
  }

  def disconnectMessageTemplate(implicit channelId: String): Array[Byte] = {
    Array[Byte](disconnect) - channelId
  }

  def dataMessageTemplate(bytes: Array[Byte])(implicit channelId: String): Array[Byte] = {
    Array[Byte](data) - channelId ++ bytes
  }
}
