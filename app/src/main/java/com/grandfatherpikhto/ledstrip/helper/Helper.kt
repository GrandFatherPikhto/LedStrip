package com.grandfatherpikhto.ledstrip.helper

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Network Byte Order
 * https://docs.microsoft.com/ru-ru/windows/win32/api/winsock/nf-winsock-htonl
 **/

private fun ByteArray.toInt(order: ByteOrder = ByteOrder.BIG_ENDIAN):Int {
    return ByteBuffer.wrap(this).order(order).getInt(0)
}

private fun ByteArray.toFloat(order: ByteOrder = ByteOrder.BIG_ENDIAN):Float {
    return ByteBuffer.wrap(this).order(order).getFloat(0)
}

/**
 * Преобразует число с плавающей запятой в байтовую последовательность для
 * передачи по сети (по стандарту BigEndian) https://en.wikipedia.org/wiki/Endianness
 */
fun Float.toByteArray(order: ByteOrder = ByteOrder.BIG_ENDIAN):ByteArray {
    return ByteBuffer.allocate(Float.SIZE_BYTES).order(order).putFloat(this).array();
}

/**
 * Преобразует целое число в байтовую последовательность для
 * передачи по сети (по стандарту BigEndian) https://en.wikipedia.org/wiki/Endianness
 */
fun Int.toByteArray(order: ByteOrder = ByteOrder.BIG_ENDIAN):ByteArray {
    return ByteBuffer.allocate(Int.SIZE_BYTES).order(order).putInt(this).array()
}

/**
 * Преобразует целое число в строку в виде 16-ричного беззнакового
 */
fun Int.toHex():String {
    return this.toUInt().toString(16)
}
