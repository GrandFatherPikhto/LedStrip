package com.grandfatherpikhto.ledstrip.model

enum class Regime(val value: Int) {
    Off(0x00),
    Color(0x01),
    Tag(0x02),
    Water(0x03),
    Tail(0x04),
    Blink(0x05);

    val enabled:Boolean get() {
        if(this == Off) {
            return false
        }

        return true
    }

    companion object {
        private val VALUES = Regime.values()
        fun getByValue(value: Int) = VALUES.firstOrNull { it.value == value }
        fun toStringList(): List<String> {
            return values().map {
                it.toString()
            }
        }
    }
}