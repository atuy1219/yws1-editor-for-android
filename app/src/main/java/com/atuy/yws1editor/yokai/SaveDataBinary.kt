package com.atuy.yws1editor.yokai

import java.io.IOException

internal object SaveDataBinary {
    fun requireRange(data: ByteArray, offset: Int, size: Int, label: String) {
        if (offset < 0 || size < 0 || offset.toLong() + size.toLong() > data.size.toLong()) {
            throw IOException("$label がデータ範囲外です")
        }
    }

    fun readUInt16Le(data: ByteArray, offset: Int): Int {
        requireRange(data, offset, 2, "u16")
        return (data[offset].toInt() and 0xff) or
            ((data[offset + 1].toInt() and 0xff) shl 8)
    }

    fun readUInt32Le(data: ByteArray, offset: Int): Long {
        requireRange(data, offset, 4, "u32")
        return (data[offset].toLong() and 0xffL) or
            ((data[offset + 1].toLong() and 0xffL) shl 8) or
            ((data[offset + 2].toLong() and 0xffL) shl 16) or
            ((data[offset + 3].toLong() and 0xffL) shl 24)
    }
}
