package proxy.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.{DuplexChannel, SocketChannel}
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.bytes.{ByteArrayDecoder, ByteArrayEncoder}
import io.netty.handler.codec.socksx.v5.{Socks5CommandRequestDecoder, Socks5InitialRequestDecoder, Socks5ServerEncoder}
import io.netty.handler.timeout.ReadTimeoutHandler
import proxy.Factory
import proxy.common.crypto.RC4
import proxy.common.handler.{DecryptHandler, EncryptHandler}
import proxy.common.{Commons, Message}
import proxy.server.handler.ServerMuxHandler
import proxy.server.handler.socks5.{Socks5CommandRequestHandler, Socks5InitialRequestHandler}

object Server {

  def start(listen: Int, key: String, readTimeOut: Int): Unit = {
    Commons.readTimeOut = readTimeOut

    val localInitializer: ChannelInitializer[DuplexChannel] = socketChannel => socketChannel.pipeline()
      .addLast(Socks5ServerEncoder.DEFAULT)
      .addLast(new Socks5InitialRequestDecoder)
      .addLast(Socks5InitialRequestHandler)
      .addLast(new Socks5CommandRequestDecoder)
      .addLast(Socks5CommandRequestHandler)

    Factory.createLocalServerBootstrap
      .childHandler(localInitializer)
      .bind(Commons.localAddress)
      .sync()

    import proxy.common.Convert.ByteBufConvert.byteArrayToByteBuf

    val rc4 = new RC4(key)

    val tcpInitializer: ChannelInitializer[SocketChannel] = socketChannel => socketChannel.pipeline()
      .addLast(new ReadTimeoutHandler(readTimeOut))
      .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, Message.delimiter))
      .addLast(new ByteArrayEncoder)
      .addLast(new ByteArrayDecoder)
      .addLast(new EncryptHandler(rc4))
      .addLast(new DecryptHandler(rc4))
      .addLast(new ServerMuxHandler) // 多路处理器

    Factory.createTcpServerBootstrap
      .childHandler(tcpInitializer)
      .bind(listen)
      .sync()

    Commons.log.info(s"Listen: $listen")
  }
}
