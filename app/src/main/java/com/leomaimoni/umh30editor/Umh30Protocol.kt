package com.leomaimoni.umh30editor

object Umh30Protocol {
    val saveCommand = byteArrayOf(
        0xF0.toByte(), 0x00, 0x21, 0x5E,
        0x02, 0x02, 0x00, 0xF7.toByte()
    )

    fun hex(bytes: ByteArray): String =
        bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}
