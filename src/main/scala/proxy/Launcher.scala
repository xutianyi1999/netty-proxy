package proxy

import proxy.core.Factory

object Launcher extends App {
  val startF = args(0) match {
    case "server" => Factory.createServer()
    case "client" => Factory.createClient()
  }
  startF()
}
