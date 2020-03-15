package proxy

import java.nio.charset.StandardCharsets

import com.alibaba.fastjson.{JSON, JSONObject}
import proxy.client.Client
import proxy.server.Server

object Launcher extends App {

  import proxy.common.Commons.autoClose

  def getConfig(path: String): JSONObject = autoClose(this.getClass.getResourceAsStream(path)) {
    JSON.parseObject[JSONObject](_, StandardCharsets.UTF_8, classOf[JSONObject])
  }

  args(0) match {
    case "server" => server(getConfig("/server-config.json"))
    case "client" => client(getConfig("/client-config.json"))
  }

  def server(jsonObject: JSONObject): Unit = Server.start(
    jsonObject.getIntValue("listen"),
    jsonObject.getString("key")
  )

  def client(jsonObject: JSONObject): Unit = Client.start(
    jsonObject.getIntValue("listen"),
    jsonObject.getJSONObject("remote")
  )
}
