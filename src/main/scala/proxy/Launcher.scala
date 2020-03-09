package proxy

object Launcher extends App {
  val f1 = (a: Int) => a + 1
  val f2 = f1.compose((a: Int) => a * 2)
  println(f2(10))
}
