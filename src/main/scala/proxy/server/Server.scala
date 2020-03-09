package proxy.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.local.LocalChannel
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.socksx.v5.{Socks5CommandRequestDecoder, Socks5InitialRequestDecoder, Socks5ServerEncoder}
import proxy.Factory
import proxy.common.{Commons, Message, RC4}
import proxy.server.handler.ServerMuxHandler
import proxy.server.handler.socks5.{Socks5CommandRequestHandler, Socks5InitialRequestHandler}

object Server {

  def start(listen: Int, key: String): Unit = {
    val localInitializer: ChannelInitializer[LocalChannel] = localChannel => localChannel.pipeline()
      .addLast(Socks5ServerEncoder.DEFAULT)
      .addLast(new Socks5InitialRequestDecoder)
      .addLast(Socks5InitialRequestHandler)
      .addLast(new Socks5CommandRequestDecoder)
      .addLast(Socks5CommandRequestHandler)

    Factory.createLocalServerBootstrap
      .childHandler(localInitializer)
      .bind(Commons.localAddress)
      .sync()

    val tcpInitializer: ChannelInitializer[SocketChannel] = socketChannel => socketChannel.pipeline()
      .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, socketChannel.alloc().buffer().writeBytes(Message.delimiter)))
      .addLast(new ServerMuxHandler(new RC4(key))) // 多路处理器

    Factory.createTcpServerBootstrap
      .childHandler(tcpInitializer)
      .bind(listen)
      .sync()
  }
}
