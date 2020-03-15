package proxy.client

import com.alibaba.fastjson.JSONObject
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.bytes.ByteArrayEncoder
import proxy.Factory
import proxy.client.handler.ClientProxyHandler
import proxy.common._

import scala.collection.JavaConverters._
import scala.util.Random

object Client {
  def start(listen: Int, remote: JSONObject): Unit = {
    startClientProxy(listen, remote)
  }

  private def startClientProxy(listen: Int, remote: JSONObject): Unit = {
    val clientMuxChannelSeq = distribution(remote)

    def getClientMuxChannel: ClientMuxChannel = {
      val seq = clientMuxChannelSeq.filter(_.isActive)

      if (seq.nonEmpty)
        seq(Random.nextInt(seq.length))
      else
        throw new Exception("Connection pool is empty")
    }

    val initializer: ChannelInitializer[SocketChannel] = socketChannel => {
      socketChannel.pipeline()
        .addLast(new ByteArrayEncoder)
        .addLast(new ClientProxyHandler(() => getClientMuxChannel))
    }

    Factory.createTcpServerBootstrap
      .childHandler(initializer)
      .bind(listen)
      .sync()

    Commons.log.info(s"Listen: $listen")
  }

  private def distribution(jsonObject: JSONObject): Seq[ClientMuxChannel] = {
    val f: ((String, AnyRef)) => Seq[ClientMuxChannel] = { tuple =>
      val json = tuple._2.asInstanceOf[JSONObject]

      val count = json.getIntValue("connections")
      val host = json.getString("host")
      val port = json.getIntValue("port")
      val rc4 = new RC4(json.getString("key"))

      (1 to count).map(i => new ClientMuxChannel(s"${tuple._1}-$i", host, port, rc4))
    }

    jsonObject.asScala.flatMap(f).toSeq
  }
}
