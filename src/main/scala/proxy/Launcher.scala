package proxy

import scala.util.{Failure, Success, Try}

object Launcher extends App {
  val t = Try(throw new IllegalArgumentException)

  t match {
    case Failure(exception) => println("eee")
    case Success(value) => println("success")
  }

  t match {
    case Failure(exception) => println("eee")
    case Success(value) => println("success")
  }
}
