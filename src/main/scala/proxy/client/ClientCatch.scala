package proxy.client

import io.netty.channel.Channel

object ClientCatch {

  var remoteChannelOption: Option[Channel] = None[Channel]
}
