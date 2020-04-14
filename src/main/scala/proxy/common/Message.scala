package proxy.common

import java.nio.charset.StandardCharsets

import proxy.common.`case`.{MessageCase, MessageConnect, MessageData, MessageDisconnect}

object Message {

  val connect: Byte = 1
  val disconnect: Byte = 2
  val data: Byte = 3
  val heartbeat: Byte = 4
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

  val heartbeatTemplate: Array[Byte] = Array[Byte](heartbeat)

  def messageMatch(msg: Array[Byte])(fun: MessageCase => Unit): Unit = {
    val messageType = msg.getMessageType

    if (messageType != heartbeat) {
      val remoteChannelId: String = msg.getChannelId

      messageType match {
        case Message.connect => fun(MessageConnect(remoteChannelId))
        case Message.disconnect => fun(MessageDisconnect(remoteChannelId))
        case Message.data => fun(MessageData(remoteChannelId, () => msg.getData))
      }
    }
  }
}
