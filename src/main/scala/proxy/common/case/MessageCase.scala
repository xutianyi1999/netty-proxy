package proxy.common.`case`

sealed trait MessageCase

case object MessageConnect extends MessageCase

case object MessageDisconnect extends MessageCase

final case class MessageData(bytes: () => Array[Byte]) extends MessageCase
