package proxy.common

import io.netty.buffer.{ByteBuf, ByteBufUtil}

object Convert {

  implicit class MessageConvert(rc4: RC4) {

    def encryptMessage(f: Array[Byte] => Unit): ByteBuf => Unit = {
      plaintext => f(rc4.encrypt(ByteBufUtil.getBytes(plaintext)))
    }

    def decryptMessage(ciphertext: ByteBuf): Array[Byte] = {
      rc4.decrypt(ByteBufUtil.getBytes(ciphertext))
    }
  }

  implicit class MapConvert[K, V](map: java.util.Map[K, V]) {

    def ifPresent(key: K)(f: V => Unit): Unit = {
      val v = map.get(key)
      if (v != null) f(v)
    }
  }

}
