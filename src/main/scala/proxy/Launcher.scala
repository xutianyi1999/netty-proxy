package proxy

import java.io.FileInputStream
import java.nio.charset.StandardCharsets

import com.alibaba.fastjson.{JSON, JSONObject}
import io.netty.channel.WriteBufferWaterMark
import proxy.client.Client
import proxy.common.Commons
import proxy.server.Server

object Launcher extends App {

  val config = Commons.autoClose(new FileInputStream(args(1))) {
    JSON.parseObject[JSONObject](_, StandardCharsets.UTF_8, classOf[JSONObject])
  }

  val trafficShaping = config.getJSONObject("trafficShaping")

  Commons.isTrafficShapingEnable = trafficShaping.getBoolean("isEnable")

  if (Commons.isTrafficShapingEnable) {
    Commons.delay = trafficShaping.getIntValue("delay")

    Commons.waterMark = new WriteBufferWaterMark(
      trafficShaping.getIntValue("lowWaterMark"),
      trafficShaping.getIntValue("highWaterMark")
    )
  }

  args(0) match {
    case "server" => server()
    case "client" => client()
  }

  def server(): Unit = Server.start(
    config.getIntValue("listen"),
    config.getString("key"),
    config.getIntValue("readTimeout")
  )

  def client(): Unit = Client.start(
    config.getIntValue("listen"),
    config.getJSONObject("remote"),
    config.getBoolean("printHost")
  )
}
