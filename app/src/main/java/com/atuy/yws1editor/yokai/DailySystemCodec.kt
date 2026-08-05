package com.atuy.yws1editor.yokai

import java.io.IOException

enum class DailySimpleFlag(
    val displayName: String,
    val description: String,
    val bitIndex: Int,
) {
    Nfc(
        displayName = "高レア妖怪アーク（NFC）",
        description = "1日1回の高レア妖怪アーク利用権",
        bitIndex = 78,
    ),
    Wanted(
        displayName = "指名手配妖怪",
        description = "1日1回の指名手配発見",
        bitIndex = 80,
    ),
    GoldEgg(
        displayName = "金の卵",
        description = "1日1回の金の卵イベント",
        bitIndex = 81,
    ),
    MoneyOffering(
        displayName = "お賽銭",
        description = "1日1回のお賽銭",
        bitIndex = 82,
    ),
}

data class DailySimpleFlagEntry(
    val definition: DailySimpleFlag,
    val usedToday: Boolean,
)

data class DailySystemState(
    val battles: List<DailyBattleEntry>,
    val gashaRewardClaimed: Boolean,
    val gashaUseCount: Int,
    val sasuraiRewardDrawn: Boolean,
    val sasuraiRewardCount: Int,
    val simpleFlags: List<DailySimpleFlagEntry>,
) {
    companion object {
        val EMPTY = DailySystemState(
            battles = DailyBattleCodec.DEFINITIONS.map { DailyBattleEntry(it, foughtToday = false) },
            gashaRewardClaimed = false,
            gashaUseCount = 0,
            sasuraiRewardDrawn = false,
            sasuraiRewardCount = 0,
            simpleFlags = DailySimpleFlag.entries.map { DailySimpleFlagEntry(it, usedToday = false) },
        )
    }
}

/**
 * Reads and writes the global daily-use fields serialized by ywFlagStatus.
 *
 * Only confirmed daily bits and counters are changed. Gasha RNG state,
 * wandering resident data, friendship flags, and event-progress flags are preserved.
 */
class DailySystemCodec {
    companion object {
        const val GLOBAL_FLAG_DATA_OFFSET = 0xF4
        const val GLOBAL_FLAG_DATA_SIZE = 0x500

        private const val GLOBAL_BYTE_OBJECT_OFFSET = 0x148
        private const val SERIALIZED_OBJECT_OFFSET = 0x08
        const val GLOBAL_BYTE_DATA_OFFSET =
            GLOBAL_FLAG_DATA_OFFSET + GLOBAL_BYTE_OBJECT_OFFSET - SERIALIZED_OBJECT_OFFSET // 0x234

        const val SASURAI_REWARD_BIT = 72
        const val GASHA_REWARD_BIT = 87

        const val GASHA_USE_COUNT_BYTE = 16
        const val SASURAI_REWARD_COUNT_BYTE = 33
    }

    private val battleCodec = DailyBattleCodec()

    init {
        check(GLOBAL_BYTE_DATA_OFFSET == 0x234)
        check(DailySimpleFlag.entries.map { it.bitIndex }.distinct().size == DailySimpleFlag.entries.size)
    }

    fun decode(gameData: ByteArray): DailySystemState {
        requireFlagRegion(gameData)
        return DailySystemState(
            battles = battleCodec.decode(gameData),
            gashaRewardClaimed = readBit(gameData, GASHA_REWARD_BIT),
            gashaUseCount = readByte(gameData, GASHA_USE_COUNT_BYTE),
            sasuraiRewardDrawn = readBit(gameData, SASURAI_REWARD_BIT),
            sasuraiRewardCount = readByte(gameData, SASURAI_REWARD_COUNT_BYTE),
            simpleFlags = DailySimpleFlag.entries.map { definition ->
                DailySimpleFlagEntry(definition, readBit(gameData, definition.bitIndex))
            },
        )
    }

    fun apply(gameData: ByteArray, state: DailySystemState): ByteArray {
        requireFlagRegion(gameData)
        var output = battleCodec.apply(gameData, state.battles)
        output = writeBit(output, GASHA_REWARD_BIT, state.gashaRewardClaimed)
        output = writeByte(output, GASHA_USE_COUNT_BYTE, state.gashaUseCount)
        output = writeBit(output, SASURAI_REWARD_BIT, state.sasuraiRewardDrawn)
        output = writeByte(output, SASURAI_REWARD_COUNT_BYTE, state.sasuraiRewardCount)
        state.simpleFlags.forEach { entry ->
            output = writeBit(output, entry.definition.bitIndex, entry.usedToday)
        }
        return output
    }

    fun setBattle(state: DailySystemState, flagIndex: Int, foughtToday: Boolean): DailySystemState {
        if (DailyBattleCodec.DEFINITIONS.none { it.flagIndex == flagIndex }) {
            throw IOException("未対応の日次戦闘フラグです: $flagIndex")
        }
        return state.copy(
            battles = state.battles.map { entry ->
                if (entry.definition.flagIndex == flagIndex) entry.copy(foughtToday = foughtToday) else entry
            },
        )
    }

    fun setAllBattles(state: DailySystemState, foughtToday: Boolean): DailySystemState =
        state.copy(battles = battleCodec.setAll(state.battles, foughtToday))

    fun resetGasha(state: DailySystemState): DailySystemState = state.copy(
        gashaRewardClaimed = false,
        gashaUseCount = 0,
    )

    fun resetSasurai(state: DailySystemState): DailySystemState = state.copy(
        sasuraiRewardDrawn = false,
        sasuraiRewardCount = 0,
    )

    fun setSimpleFlag(
        state: DailySystemState,
        definition: DailySimpleFlag,
        usedToday: Boolean,
    ): DailySystemState = state.copy(
        simpleFlags = state.simpleFlags.map { entry ->
            if (entry.definition == definition) entry.copy(usedToday = usedToday) else entry
        },
    )

    fun resetAll(state: DailySystemState): DailySystemState = DailySystemState(
        battles = battleCodec.setAll(state.battles, foughtToday = false),
        gashaRewardClaimed = false,
        gashaUseCount = 0,
        sasuraiRewardDrawn = false,
        sasuraiRewardCount = 0,
        simpleFlags = state.simpleFlags.map { it.copy(usedToday = false) },
    )

    private fun readBit(data: ByteArray, bitIndex: Int): Boolean {
        val offset = GLOBAL_FLAG_DATA_OFFSET + bitIndex / Byte.SIZE_BITS
        val mask = 1 shl (bitIndex and 7)
        return data[offset].toInt() and mask != 0
    }

    private fun writeBit(data: ByteArray, bitIndex: Int, enabled: Boolean): ByteArray {
        val output = data.copyOf()
        val offset = GLOBAL_FLAG_DATA_OFFSET + bitIndex / Byte.SIZE_BITS
        val mask = 1 shl (bitIndex and 7)
        val oldValue = output[offset].toInt() and 0xff
        output[offset] = if (enabled) {
            (oldValue or mask).toByte()
        } else {
            (oldValue and mask.inv()).toByte()
        }
        return output
    }

    private fun readByte(data: ByteArray, byteIndex: Int): Int =
        data[GLOBAL_BYTE_DATA_OFFSET + byteIndex].toInt() and 0xff

    private fun writeByte(data: ByteArray, byteIndex: Int, value: Int): ByteArray {
        if (value !in 0..0xff) throw IOException("日次カウンターがu8範囲外です: $value")
        return data.copyOf().also { output ->
            output[GLOBAL_BYTE_DATA_OFFSET + byteIndex] = value.toByte()
        }
    }

    private fun requireFlagRegion(gameData: ByteArray) {
        SaveDataBinary.requireRange(
            gameData,
            GLOBAL_FLAG_DATA_OFFSET,
            GLOBAL_FLAG_DATA_SIZE,
            "グローバルフラグ領域",
        )
    }
}
