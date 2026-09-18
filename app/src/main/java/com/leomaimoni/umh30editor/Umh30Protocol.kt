package com.leomaimoni.umh30editor

object Umh30Protocol {
    private const val PHYSICAL = 0x00
    private const val USB_HOST = 0x01
    private const val CONNECT = 0x01

    val saveCommand = byteArrayOf(
        0xF0.toByte(), 0x00, 0x21, 0x5E,
        0x02, 0x02, 0x00, 0xF7.toByte()
    )

    fun routingCommand(
        sourceType: Int,
        sourceIndex: Int,
        destinationType: Int,
        destinationIndex: Int,
        connected: Boolean = true
    ): ByteArray = byteArrayOf(
        0xF0.toByte(), 0x00, 0x21, 0x5E,
        0x02, 0x01,
        sourceType.toByte(), sourceIndex.toByte(),
        destinationType.toByte(), destinationIndex.toByte(),
        if (connected) CONNECT.toByte() else 0x00,
        0xF7.toByte()
    )

    /**
     * USB-host device identification reported by the UMH-30.
     *
     * Observed pattern:
     * F0 00 21 5E 02 06 01 SLOT LENGTH ASCII... F7
     *
     * Example:
     * F0 00 21 5E 02 06 01 01 07 57 2D 46 41 44 45 52 F7
     * -> USB 1 = W-FADER
     */
    data class UsbHostName(val slot: Int, val name: String)

    fun parseUsbHostName(data: ByteArray): UsbHostName? {
        if (data.size < 11) return null
        val b = data.map { it.toInt() and 0xFF }
        if (b[0] != 0xF0 || b[1] != 0x00 || b[2] != 0x21 ||
            b[3] != 0x5E || b[4] != 0x02 || b[5] != 0x06 ||
            b[6] != 0x01 || b.last() != 0xF7
        ) return null

        val slot = b[7]
        val length = b[8]
        if (slot !in 1..8) return null
        if (length < 0 || 9 + length >= b.size) return null

        val chars = b.subList(9, 9 + length)
        if (chars.any { it !in 0x20..0x7E }) return null
        return UsbHostName(slot, chars.map { it.toChar() }.joinToString(""))
    }

    fun hex(bytes: ByteArray): String =
        bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}
