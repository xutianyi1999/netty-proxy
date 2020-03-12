package proxy.common

sealed trait CloseInfo

case class CloseOne(channelId: String) extends CloseInfo

case object CloseAll extends CloseInfo
