package proxy.server

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelInitializer, ChannelOption, WriteBufferWaterMark}
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.bytes.ByteArrayDecoder
import io.netty.handler.timeout.ReadTimeoutHandler
import proxy.Factory
import proxy.common.Commons
import proxy.common.crypto.RC4
import proxy.common.handler.{DecryptHandler, EncryptHandler}
import proxy.server.handler.ServerMuxHandler

object Server {

  def start(listen: Int, key: String, readTimeOut: Int): Unit = {
    Commons.readTimeOut = readTimeOut
    val rc4 = new RC4(key)

    val tcpInitializer: ChannelInitializer[SocketChannel] = socketChannel => socketChannel.pipeline()
      .addLast(new ReadTimeoutHandler(readTimeOut))
      .addLast(Commons.lengthFieldPrepender)
      .addLast(new LengthFieldBasedFrameDecoder(0xffffffff, 0, 4, 0, 4))
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
