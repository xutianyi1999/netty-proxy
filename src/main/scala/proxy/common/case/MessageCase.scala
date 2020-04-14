package proxy.common.`case`

sealed trait MessageCase

case class MessageConnect(channelId: String) extends MessageCase

case class MessageDisconnect(channelId: String) extends MessageCase

case class MessageData(channelId: String, bytes: () => Array[Byte]) extends MessageCase
