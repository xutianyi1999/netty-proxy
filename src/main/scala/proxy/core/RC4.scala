package proxy.core

import java.nio.charset.StandardCharsets
import java.security.SecureRandom

import io.netty.buffer.{ByteBuf, ByteBufUtil}
import javax.crypto.{Cipher, KeyGenerator}

class RC4(password: String) {

  private val secureRandom = SecureRandom.getInstance("SHA1PRNG")
  secureRandom.setSeed(password.getBytes(StandardCharsets.UTF_8))

  private val keyGenerator = KeyGenerator.getInstance("RC4")
  keyGenerator.init(40, secureRandom)
  private val key = keyGenerator.generateKey()

  private val rc4Encrypt = Cipher.getInstance("RC4")
  rc4Encrypt.init(Cipher.ENCRYPT_MODE, key)

  private val rc4Decrypt = Cipher.getInstance("RC4")
  rc4Decrypt.init(Cipher.DECRYPT_MODE, key)

  def encrypt(data: ByteBuf): Array[Byte] = encrypt(ByteBufUtil.getBytes(data))

  def encrypt(data: Array[Byte]): Array[Byte] = rc4Encrypt.doFinal(data)

  def decrypt(data: ByteBuf): Array[Byte] = decrypt(ByteBufUtil.getBytes(data))

  def decrypt(data: Array[Byte]): Array[Byte] = rc4Decrypt.doFinal(data)
}
