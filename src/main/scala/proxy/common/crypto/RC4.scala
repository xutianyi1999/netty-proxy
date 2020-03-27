package proxy.common.crypto

import java.nio.charset.StandardCharsets
import java.security.SecureRandom

import javax.crypto.{Cipher, KeyGenerator}

class RC4(password: String) extends Cipher {

  override val keySize: Int = 40
  override val name: String = "RC4"

  private val secureRandom = SecureRandom.getInstance("SHA1PRNG")
  secureRandom.setSeed(password.getBytes(StandardCharsets.UTF_8))

  private val keyGenerator = KeyGenerator.getInstance(name)
  keyGenerator.init(keySize, secureRandom)
  private val key = keyGenerator.generateKey()

  override def getEncryptF: Array[Byte] => Array[Byte] = init(Cipher.ENCRYPT_MODE)

  override def getDecryptF: Array[Byte] => Array[Byte] = init(Cipher.DECRYPT_MODE)

  private def init(mode: Int): Array[Byte] => Array[Byte] = {
    val rc4Crypt = Cipher.getInstance(name)
    rc4Crypt.init(mode, key)
    rc4Crypt.doFinal
  }
}
