package proxy.common

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

import proxy.common.`case`.{MessageCase, MessageConnect, MessageData, MessageDisconnect}

object Message {

  val connect: Byte = 1
  val disconnect: Byte = 2
  val data: Byte = 3
  val heartbeat: Byte = 4

  val heartbeatTemplate: Array[Byte] = Array[Byte](heartbeat)

  def connectMessageTemplate(address: String, port: Int)(implicit channelId: String): Array[Byte] = {
    connect +: (channelId + address + ':' + port).getBytes(StandardCharsets.UTF_8)
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
      val channelId = new String(msg.slice(1, 9), StandardCharsets.UTF_8)
      val f2 = fun(channelId)

      messageType match {
        case Message.connect =>
          val address = new String(msg.slice(9, msg.length), StandardCharsets.UTF_8).split(':')
          f2(MessageConnect(new InetSocketAddress(address(0), address(1).toInt)))

        case Message.disconnect => f2(MessageDisconnect)
        case Message.data => f2(MessageData(() => msg.slice(9, msg.length)))
      }
    }
  }
}
