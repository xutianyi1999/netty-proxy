package proxy.common

import java.util.logging.Logger

import io.netty.channel.local.LocalAddress

object Commons {

  val localAddress = new LocalAddress("local")
  val log: Logger = Logger.getGlobal
}
