package proxy.common.`case`

import java.net.SocketAddress

sealed trait MessageCase

final case class MessageConnect(socketAddress: SocketAddress) extends MessageCase

case object MessageDisconnect extends MessageCase

final case class MessageData(bytes: () => Array[Byte]) extends MessageCase
