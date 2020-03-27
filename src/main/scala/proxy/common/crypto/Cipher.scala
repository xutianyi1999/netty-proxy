package proxy.common.crypto

trait Cipher {

  val keySize: Int
  val name: String

  def getEncryptF: Array[Byte] => Array[Byte]

  def getDecryptF: Array[Byte] => Array[Byte]
}
