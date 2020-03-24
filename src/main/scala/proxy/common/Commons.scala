package proxy.common

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import io.netty.channel.Channel
import io.netty.channel.local.LocalAddress
import io.netty.util.concurrent.ScheduledFuture

object Commons {

  val localAddress = new LocalAddress("local")
  val log: Logger = Logger.getGlobal

  def autoClose[A <: AutoCloseable, B](closeable: A)(fun: A ⇒ B): B = {
    try fun(closeable)
    finally closeable.close()
  }

  // server
  var readTimeOut: Int = _

  def trafficShaping(writeChannel: Channel, readChannel: Channel, delayF: (Runnable, Long, TimeUnit) => ScheduledFuture[_]): Unit = {
    val isWriteable = writeChannel.isWritable

    if (!isWriteable) {
      delayF(() => trafficShaping(writeChannel, readChannel, delayF), 500, TimeUnit.MILLISECONDS)
    }
    readChannel.config().setAutoRead(isWriteable)
  }
}
