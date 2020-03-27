package proxy.common.crypto

trait CipherTrait {

  val name: String
  val keySize: Int

  def getEncryptF: Array[Byte] => Array[Byte]

  def getDecryptF: Array[Byte] => Array[Byte]
}
