package proxy.common.`case`

sealed trait CloseCase

final case class CloseOne(channelId: String) extends CloseCase

case object CloseAll extends CloseCase
