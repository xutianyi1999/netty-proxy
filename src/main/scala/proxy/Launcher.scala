package proxy

import java.io.FileInputStream
import java.nio.charset.StandardCharsets

import com.alibaba.fastjson.{JSON, JSONObject}
import proxy.client.Client
import proxy.server.Server

object Launcher extends App {

  import proxy.common.Commons.autoClose

  val config = autoClose(new FileInputStream(args(1))) {
    JSON.parseObject[JSONObject](_, StandardCharsets.UTF_8, classOf[JSONObject])
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
    config.getJSONObject("remote")
  )
}
