package proxy.common

import java.nio.charset.StandardCharsets
import java.security.SecureRandom

import javax.crypto.{Cipher, KeyGenerator}

class RC4(password: String) {

  private val secureRandom = SecureRandom.getInstance("SHA1PRNG")
  secureRandom.setSeed(password.getBytes(StandardCharsets.UTF_8))

  private val keyGenerator = KeyGenerator.getInstance("RC4")
  keyGenerator.init(40, secureRandom)
  private val key = keyGenerator.generateKey()

  def getEncryptF: Array[Byte] => Array[Byte] = {
    val rc4Encrypt = Cipher.getInstance("RC4")
    rc4Encrypt.init(Cipher.ENCRYPT_MODE, key)
    rc4Encrypt.doFinal
  }

  def getDecryptF: Array[Byte] => Array[Byte] = {
    val rc4Decrypt = Cipher.getInstance("RC4")
    rc4Decrypt.init(Cipher.DECRYPT_MODE, key)
    rc4Decrypt.doFinal
  }
}
