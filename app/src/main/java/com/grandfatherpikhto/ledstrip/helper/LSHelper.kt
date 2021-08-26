package com.grandfatherpikhto.ledstrip.helper

import java.lang.StringBuilder

object LSHelper {
    const val btPrefs:String   = "BT_PREFS"
    const val btName:String    = "BT_NAME"
    const val btAddress:String = "BT_ADDRESS"
    const val btBound:String   = "BT_BOUND"
    const val scanPeriod:Long  = 10000

    /**
     * Int to byte array
     *
     * @param num
     * @return
     */
    fun intToByteArray(num:Int):ByteArray {
        val byteArray = ByteArray(Int.SIZE_BYTES)
        for(i in byteArray.indices) {
            byteArray[i] = num.shr(i * 8).and(0xFF).toByte()
        }
        return byteArray
    }

    /**
     * Byte array to int
     *
     * @param byteArray
     * @return
     */
    fun byteArrayToInt(byteArray: ByteArray):Int {
        var result = 0
        for(i in byteArray.indices) {
            result = result.or(byteArray[i].toInt().shl(i * 8).and(0xFF.shl(i * 8)))
        }
        return result
    }

    /**
     * Byte array to hex string
     *
     * @param byteArray
     * @return
     */
    fun byteArrayToHexString(byteArray: ByteArray): String {
        val out = StringBuilder()
        byteArray.forEach { bt ->
            out.insert(0, String.format("%02x", bt))
        }
        return out.toString()
    }
}