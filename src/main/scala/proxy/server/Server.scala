package proxy.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.local.LocalChannel
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.bytes.{ByteArrayDecoder, ByteArrayEncoder}
import io.netty.handler.codec.socksx.v5.{Socks5CommandRequestDecoder, Socks5InitialRequestDecoder, Socks5ServerEncoder}
import proxy.common.handler.{ByteArrayToByteBufDecoder, RC4Decrypt, RC4Encrypt}
import proxy.common.{Commons, Message, RC4}
import proxy.server.handler.ServerMuxHandler
import proxy.server.handler.socks5.{Socks5CommandRequestHandler, Socks5InitialRequestHandler}
import proxy.{Factory, LocalTransportFactory}

object Server {

  def start(listen: Int, key: String): Unit = {
    val localInitializer: ChannelInitializer[LocalChannel] = localChannel => localChannel.pipeline()
      .addLast(ByteArrayToByteBufDecoder)
      .addLast(Socks5ServerEncoder.DEFAULT)
      .addLast(new Socks5InitialRequestDecoder)
      .addLast(Socks5InitialRequestHandler)
      .addLast(new Socks5CommandRequestDecoder)
      .addLast(Socks5CommandRequestHandler)

    LocalTransportFactory.createLocalServerBootstrap
      .childHandler(localInitializer)
      .bind(Commons.localAddress)
      .sync()

    import proxy.common.Convert.byteArrayToByteBuf

    val rc4 = new RC4(key)

    val tcpInitializer: ChannelInitializer[SocketChannel] = socketChannel => socketChannel.pipeline()
      .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, Message.delimiter))
      .addLast(new ByteArrayEncoder)
      .addLast(new ByteArrayDecoder)
      .addLast(new RC4Encrypt(rc4))
      .addLast(new RC4Decrypt(rc4))
      .addLast(ByteArrayToByteBufDecoder)
      .addLast(new ServerMuxHandler) // 多路处理器

    Factory.createTcpServerBootstrap
      .childHandler(tcpInitializer)
      .bind(listen)
      .sync()
  }
}
