package proxy.client

import com.alibaba.fastjson.JSONObject
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.socksx.v5.{Socks5CommandRequestDecoder, Socks5InitialRequestDecoder, Socks5ServerEncoder}
import proxy.Factory
import proxy.client.handler.ClientProxyHandler
import proxy.client.handler.socks5.Socks5InitialRequestHandler
import proxy.common._
import proxy.common.crypto.RC4

import scala.collection.JavaConverters._
import scala.util.Random

object Client {

  def start(listen: Int, remote: JSONObject, printHost: Boolean): Unit = {
    val clientMuxChannelSeq = distribution(remote, printHost)

    val getClientMuxChannel: () => Option[ClientMuxChannel] = { () =>
      val seq = clientMuxChannelSeq.filter(_.isActive)

      if (seq.nonEmpty) Option(seq(Random.nextInt(seq.length)))
      else Option.empty
    }

    val clientProxyHandler = new ClientProxyHandler(getClientMuxChannel)

    val initializer: ChannelInitializer[SocketChannel] = socketChannel => socketChannel.pipeline()
      .addLast(Socks5ServerEncoder.DEFAULT)
      .addLast(new Socks5InitialRequestDecoder)
      .addLast(Socks5InitialRequestHandler)
      .addLast(new Socks5CommandRequestDecoder)
      .addLast(clientProxyHandler)
      .addLast(Commons.byteArrayEncoder)

    Factory.createServerBootstrap
      .childHandler(initializer)
      .bind(listen)
      .sync()

    Commons.log.info(s"Listen: $listen")
  }

  private def distribution(jsonObject: JSONObject, printHost: Boolean): Seq[ClientMuxChannel] = {
    val l = for {
      tuple <- jsonObject.asScala
      json = tuple._2.asInstanceOf[JSONObject]

      count = json.getIntValue("connections")
      host = json.getString("host")
      port = json.getIntValue("port")
      heartbeatInterval = json.getIntValue("heartbeatInterval")
      rc4 = new RC4(json.getString("key"))

      id <- 1 to count
    } yield new ClientMuxChannel(s"${tuple._1}-$id", host, port, rc4, heartbeatInterval, printHost)

    l.toIndexedSeq
  }
}
