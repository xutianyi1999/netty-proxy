package proxy.common

import java.util.logging.Logger

import io.netty.channel.local.LocalAddress

object Commons {

  val localAddress = new LocalAddress("local")
  val log: Logger = Logger.getGlobal

  def autoClose[A <: AutoCloseable, B](closeable: A)(fun: A ⇒ B): B = {
    try {
      fun(closeable)
    } finally {
      closeable.close()
    }
  }
}
