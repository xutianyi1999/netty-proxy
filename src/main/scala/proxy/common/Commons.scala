package proxy.common

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import io.netty.channel.Channel
import io.netty.channel.local.LocalAddress
import io.netty.handler.timeout.ReadTimeoutException

object Commons {

  val localAddress = new LocalAddress("local")
  val log: Logger = Logger.getGlobal

  def printError(cause: Throwable): Unit = if (!cause.isInstanceOf[ReadTimeoutException]) {
    log.severe(cause.getMessage)
  }

  def autoClose[A <: AutoCloseable, B](closeable: A)(fun: A ⇒ B): B = {
    try fun(closeable)
    finally closeable.close()
  }

  // server
  var readTimeOut: Int = _

  var isTrafficShapingEnable: Boolean = _
  var delay: Int = _

  def trafficShaping(writeChannel: Channel, readChannel: Channel): Unit =
    if (isTrafficShapingEnable && writeChannel.isActive && readChannel.isActive) {
      val isWriteable = writeChannel.isWritable

      if (!isWriteable) {
        val runnable: Runnable = () => trafficShaping(writeChannel, readChannel)
        readChannel.eventLoop().schedule(runnable, delay, TimeUnit.MILLISECONDS)
      }
      readChannel.config().setAutoRead(isWriteable)
    }
}
