package proxy.common

import java.nio.charset.StandardCharsets

import proxy.common.`case`.{MessageCase, MessageConnect, MessageData, MessageDisconnect}

object Message {

  val connect: Byte = 1
  val disconnect: Byte = 2
  val data: Byte = 3
  val heartbeat: Byte = 4

  val delimiter: Array[Byte] = "🍔".getBytes(StandardCharsets.UTF_8)

  val connectBytes: Array[Byte] = Array[Byte](connect)
  val disconnectBytes: Array[Byte] = Array[Byte](disconnect)
  val dataMessageBytes: Array[Byte] = Array[Byte](data)
  val heartbeatTemplate: Array[Byte] = Array[Byte](heartbeat)

  import proxy.common.Convert.MessageConvert

  def connectMessageTemplate(implicit channelId: String): Array[Byte] = connectBytes - channelId

  def disconnectMessageTemplate(implicit channelId: String): Array[Byte] = disconnectBytes - channelId

  def dataMessageTemplate(bytes: Array[Byte])(implicit channelId: String): Array[Byte] = dataMessageBytes - channelId ++ bytes

  def messageMatch(msg: Array[Byte])(fun: String => MessageCase => Unit): Unit = {
    val messageType = msg.getMessageType

    if (messageType != heartbeat) {
      val f2 = fun(msg.getChannelId)

      messageType match {
        case Message.connect => f2(MessageConnect)
        case Message.disconnect => f2(MessageDisconnect)
        case Message.data => f2(MessageData(() => msg.getData))
      }
    }
  }
}
