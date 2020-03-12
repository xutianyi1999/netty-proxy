package proxy

import java.nio.charset.StandardCharsets

import com.alibaba.fastjson.{JSON, JSONObject}
import proxy.client.Client
import proxy.server.Server

object Launcher extends App {

  def autoClose[A <: AutoCloseable, B](closeable: A)(fun: A ⇒ B): B = {
    try {
      fun(closeable)
    } finally {
      closeable.close()
    }
  }

  val config = autoClose(this.getClass.getResourceAsStream("/config.json")) {
    JSON.parseObject[JSONObject](_, StandardCharsets.UTF_8, classOf[JSONObject])
  }

  val key = config.getString("key")

  args(0) match {
    case "server" => server(config.getJSONObject("server"))
    case "client" => client(config.getJSONObject("client"))
  }

  def server(jsonObject: JSONObject): Unit = Server.start(jsonObject.getIntValue("listen"), key)

  def client(jsonObject: JSONObject): Unit = {
    val remote = jsonObject.getJSONObject("remote")

    Client.start(
      jsonObject.getIntValue("listen"),
      remote.getString("host"),
      remote.getIntValue("port"),
      key
    )
  }
}
