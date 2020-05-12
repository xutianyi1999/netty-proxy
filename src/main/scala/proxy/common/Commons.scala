package proxy.common

import java.net.SocketAddress
import java.util.concurrent.TimeUnit

import io.netty.channel.{Channel, WriteBufferWaterMark}
import io.netty.handler.codec.bytes.ByteArrayEncoder
import io.netty.util.internal.StringUtil
import org.slf4j.{Logger, LoggerFactory}

object Commons {

  val byteArrayEncoder = new ByteArrayEncoder

  var waterMark: WriteBufferWaterMark = WriteBufferWaterMark.DEFAULT
  var localAddress: SocketAddress = _
  val log: Logger = LoggerFactory.getLogger("netty-proxy")

  def printError(cause: Throwable): Unit = {
    val msg = cause.getMessage
    if (!StringUtil.isNullOrEmpty(msg)) log.error(msg)
  }

  def autoClose[A <: AutoCloseable, B](closeable: A)(fun: A => B): B = {
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
