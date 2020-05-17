package proxy.common

import java.nio.charset.StandardCharsets

import proxy.common.`case`.{MessageCase, MessageConnect, MessageData, MessageDisconnect}

object Message {

  val connect: Byte = 1
  val disconnect: Byte = 2
  val data: Byte = 3
  val heartbeat: Byte = 4

  val delimiter: Array[Byte] = "🍔".getBytes(StandardCharsets.UTF_8)
  val heartbeatTemplate: Array[Byte] = Array[Byte](heartbeat)

  def connectMessageTemplate(implicit channelId: String): Array[Byte] = {
    connect +: channelId.getBytes(StandardCharsets.UTF_8)
  }

  def disconnectMessageTemplate(implicit channelId: String): Array[Byte] = {
    disconnect +: channelId.getBytes(StandardCharsets.UTF_8)
  }

  def dataMessageTemplate(bytes: Array[Byte])(implicit channelId: String): Array[Byte] = {
    (data +: channelId.getBytes(StandardCharsets.UTF_8)) ++ bytes
  }

  def messageMatch(msg: Array[Byte])(fun: String => MessageCase => Unit): Unit = {
    val messageType = msg(0)

    if (messageType != heartbeat) {
      val f2 = fun(new String(msg.slice(1, 9), StandardCharsets.UTF_8))

      messageType match {
        case Message.connect => f2(MessageConnect)
        case Message.disconnect => f2(MessageDisconnect)
        case Message.data => f2(MessageData(() => msg.slice(9, msg.length)))
      }
    }
  }
}
