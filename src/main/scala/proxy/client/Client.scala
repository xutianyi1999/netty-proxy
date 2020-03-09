package proxy.client

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import proxy.common.Message

object Client {

  def start(host: String, port: Int, key: String): Unit = {
    val clientInitializer: ChannelInitializer[SocketChannel] = socketChannel => {
      socketChannel.pipeline()
        .addLast(new DelimiterBasedFrameDecoder(Int.MaxValue, socketChannel.alloc().buffer().writeBytes(Message.delimiter)))
    }

  }
}
