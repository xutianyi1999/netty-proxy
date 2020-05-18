package proxy.server

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelInitializer, ChannelOption, WriteBufferWaterMark}
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.bytes.ByteArrayDecoder
import io.netty.handler.timeout.ReadTimeoutHandler
import proxy.Factory
import proxy.common.crypto.RC4
import proxy.common.handler.{DecryptHandler, EncryptHandler}
import proxy.common.{Commons, Message}
import proxy.server.handler.ServerMuxHandler

object Server {

  def start(listen: Int, key: String, readTimeOut: Int): Unit = {
    Commons.readTimeOut = readTimeOut
    import proxy.common.Convert.ByteBufConvert.byteArrayToByteBuf

    val rc4 = new RC4(key)

    val tcpInitializer: ChannelInitializer[SocketChannel] = socketChannel => socketChannel.pipeline()
      .addLast(new ReadTimeoutHandler(readTimeOut))
      .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, Message.delimiter))
      .addLast(Commons.byteArrayEncoder)
      .addLast(new ByteArrayDecoder)
      .addLast(new EncryptHandler(rc4))
      .addLast(new DecryptHandler(rc4))
      .addLast(new ServerMuxHandler) // 多路处理器

    Factory.createServerBootstrap
      .childOption[WriteBufferWaterMark](ChannelOption.WRITE_BUFFER_WATER_MARK, Commons.waterMark)
      .childHandler(tcpInitializer)
      .bind(listen)
      .sync()

    Commons.log.info(s"Listen: $listen")
  }
}
