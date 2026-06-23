package com.atuy.yws1editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atuy.yws1editor.yokai.MainBinCodec
import com.atuy.yws1editor.yokai.MainBinBackupInfo
import com.atuy.yws1editor.yokai.MainBinDecoded
import com.atuy.yws1editor.yokai.InventoryCodec
import com.atuy.yws1editor.yokai.InventoryItemEntry
import com.atuy.yws1editor.yokai.EquipmentEntry
import com.atuy.yws1editor.yokai.KeyItemEntry
import com.atuy.yws1editor.yokai.GashaStateCodec
import com.atuy.yws1editor.yokai.GashaStateEntry
import com.atuy.yws1editor.yokai.SasuraiCodec
import com.atuy.yws1editor.yokai.SasuraiEncounterOption
import com.atuy.yws1editor.yokai.SasuraiResident
import com.atuy.yws1editor.yokai.SaveDomainMasterData
import com.atuy.yws1editor.yokai.SaveInfo
import com.atuy.yws1editor.yokai.SaveInfoCodec
import com.atuy.yws1editor.yokai.Stat5
import com.atuy.yws1editor.yokai.YokaiAttitude
import com.atuy.yws1editor.yokai.StatGroup
import com.atuy.yws1editor.yokai.YokaiEntry
import com.atuy.yws1editor.yokai.YokaiMasterData
import com.atuy.yws1editor.yokai.YokaiParser
import com.atuy.yws1editor.yokai.ShizukuFileGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class AppScreen {
    Startup,
    Editor,
}

enum class EditorTopTab(val label: String) {
    Yokai("妖怪"),
    Item("どうぐ"),
    Equipment("そうび"),
    KeyItem("だいじなもの"),
    Gasha("ガシャ"),
    Sasurai("さすらい荘"),
    Info("情報"),
    Encyclopedia("妖怪大辞典"),
}

data class SaveSlotCard(
    val sectionName: String,
    val title: String,
    val subtitle: String,
    val hasData: Boolean = true,
    val displayName: String? = null,
    val playTimeText: String? = null,
    val saveDateText: String? = null,
    val yokaiCount: Int? = null,
)

data class YokaiOption(
    val id: Long,
    val name: String,
)

private val DEFAULT_STARTUP_SLOTS = listOf(
    SaveSlotCard(sectionName = "game0.yw", title = "オートセーブ", subtitle = "game0.yw"),
    SaveSlotCard(sectionName = "game1.yw", title = "にっき1", subtitle = "game1.yw"),
    SaveSlotCard(sectionName = "game2.yw", title = "にっき2", subtitle = "game2.yw"),
    SaveSlotCard(sectionName = "game3.yw", title = "にっき3", subtitle = "game3.yw"),
)

data class EditorUiState(
    val currentScreen: AppScreen = AppScreen.Startup,
    val selectedTopTab: EditorTopTab = EditorTopTab.Yokai,
    val expandedYokaiSlot: Int? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val message: String = "",
    val entries: List<YokaiEntry> = emptyList(),
    val selectedSlot: Int? = null,
    val selectedSection: String = "game0.yw",
    val loaded: Boolean = false,
    val attitudes: List<YokaiAttitude> = emptyList(),
    val yokaiOptions: List<YokaiOption> = emptyList(),
    val startupSaveSlots: List<SaveSlotCard> = DEFAULT_STARTUP_SLOTS,
    val backupItems: List<MainBinBackupInfo> = emptyList(),
    val backupsLoading: Boolean = false,
    val backupsCreating: Boolean = false,
    val backupsRestoring: Boolean = false,
    val playHours: Int = 0,
    val playMinutes: Int = 0,
    val money: Int = 0,
    val playerName: String = "",
    val playerNameError: String? = null,
    val saveYear: Int = 2026,
    val saveMonth: Int = 1,
    val saveDay: Int = 1,
    val saveHour: Int = 0,
    val saveMinute: Int = 0,
    val startupSlotsLoaded: Boolean = false,
    val isCheatMode: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val inventoryItems: List<InventoryItemEntry> = emptyList(),
    val equipmentItems: List<EquipmentEntry> = emptyList(),
    val keyItems: List<KeyItemEntry> = emptyList(),
    val gashaStates: List<GashaStateEntry> = emptyList(),
    val sasuraiResidents: List<SasuraiResident> = emptyList(),
    val sasuraiEncounterOptions: List<SasuraiEncounterOption> = emptyList(),
    val itemNames: Map<Long, String> = emptyMap(),
    val equipmentNames: Map<Long, String> = emptyMap(),
    val keyItemNames: Map<Long, String> = emptyMap(),
    val safeItemQuantityCeilings: Map<Int, Int> = emptyMap(),
)

class MainViewModel : ViewModel() {

    companion object {
        private const val CHEAT_STAT_MAX = 255
        private const val NORMAL_LEVEL_MAX = 99
        private const val NORMAL_IVA_TOTAL_MAX = 8
        private const val NORMAL_IVB1_TOTAL_MAX = 10
        private const val NORMAL_CB_TOTAL_MAX = 20
        private const val NORMAL_IVB_MAX = 15

        // H, A, M, D, S
        private val IVA_EDITABLE_BY_CLASS: Map<Int, List<Boolean>> = mapOf(
            0 to listOf(false, false, false, false, false),
            1 to listOf(false, true, false, false, false),
            2 to listOf(false, false, true, false, false),
            3 to listOf(false, false, false, true, false),
            4 to listOf(false, false, false, false, true),
            5 to listOf(false, false, true, true, false),
            6 to listOf(true, false, false, false, true),
            7 to listOf(false, true, true, false, false),
            8 to listOf(true, false, false, false, false),
        )
    }

    val editableSections = listOf("game0.yw", "game1.yw", "game2.yw", "game3.yw")

    private val gateway = ShizukuFileGateway()
    private val codec = MainBinCodec()
    private val inventoryCodec = InventoryCodec()
    private val gashaCodec = GashaStateCodec()
    private val sasuraiCodec = SasuraiCodec()
    private var masterData = YokaiMasterData.EMPTY
    private var saveDomainMasterData = SaveDomainMasterData.EMPTY
    private var parser = YokaiParser(masterData)

    private var decodedMainBin: MainBinDecoded? = null
    private val fileOperationMutex = Mutex()
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun setSaveDomainMasterData(data: SaveDomainMasterData) {
        saveDomainMasterData = data
        _uiState.update {
            it.copy(
                itemNames = data.itemNames,
                equipmentNames = data.equipmentNames,
                keyItemNames = data.keyItemNames,
                sasuraiEncounterOptions = data.sasuraiEncounterOptions,
            )
        }
    }

    fun setMasterData(data: YokaiMasterData) {
        viewModelScope.launch {
            fileOperationMutex.withLock {
                masterData = data
                parser = YokaiParser(masterData)

                _uiState.update {
                    it.copy(
                        attitudes = masterData.attitudes,
                        yokaiOptions = buildYokaiOptions(masterData),
                    )
                }

                val decoded = decodedMainBin ?: return@withLock
                val sectionName = _uiState.value.selectedSection
                val section = decoded.sections[sectionName] ?: return@withLock
                val currentSlot = _uiState.value.selectedSlot
                val currentEntries = _uiState.value.entries
                val refreshedEntries = if (currentEntries.isEmpty()) {
                    withContext(Dispatchers.Default) { parser.parse(section.decryptedData) }
                } else {
                    currentEntries.map { entry ->
                        val detail = masterData.detailById[entry.id]
                        entry.copy(
                            name = masterData.nameById[entry.id] ?: entry.name,
                            baseStats = detail?.baseStats,
                            growPattern = detail?.growPattern,
                            yokaiClass = detail?.yokaiClass,
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        entries = refreshedEntries,
                        attitudes = masterData.attitudes,
                        selectedSlot = currentSlot?.takeIf { slot ->
                            refreshedEntries.any { entry -> entry.slot == slot }
                        } ?: refreshedEntries.firstOrNull()?.slot,
                    )
                }
            }
        }
    }

    fun load(path: String) {
        val sectionName = _uiState.value.selectedSection
        viewModelScope.launch {
            fileOperationMutex.withLock {
                _uiState.update { it.copy(loading = true, message = "main.bin 読み込み中...") }
                runCatching {
                    withContext(Dispatchers.IO) {
                        val raw = gateway.readBytes(path)
                        buildLoadedData(codec.decode(raw), sectionName, path)
                    }
                }.onSuccess { loadedData ->
                    decodedMainBin = loadedData.decoded
                    applyLoadedData(loadedData, message = "")
                }.onFailure { e ->
                    decodedMainBin = null
                    _uiState.update {
                        it.copy(
                            loading = false,
                            loaded = false,
                            entries = emptyList(),
                            inventoryItems = emptyList(),
                            equipmentItems = emptyList(),
                            keyItems = emptyList(),
                            gashaStates = emptyList(),
                            sasuraiResidents = emptyList(),
                            safeItemQuantityCeilings = emptyMap(),
                            expandedYokaiSlot = null,
                            selectedSlot = null,
                            startupSaveSlots = DEFAULT_STARTUP_SLOTS,
                            backupItems = emptyList(),
                            playHours = 0,
                            playMinutes = 0,
                            money = 0,
                            playerName = "",
                            playerNameError = null,
                            saveYear = 2026,
                            saveMonth = 1,
                            saveDay = 1,
                            saveHour = 0,
                            saveMinute = 0,
                            startupSlotsLoaded = false,
                            message = "読み込み失敗: ${e.message}",
                        )
                    }
                }
            }
        }
    }

    fun loadStartupSlots(path: String) {
        if (_uiState.value.loading) return

        viewModelScope.launch {
            fileOperationMutex.withLock {
                _uiState.update { it.copy(loading = true, message = "セーブ情報を読み込み中...") }
                val selectedSection = _uiState.value.selectedSection
                runCatching {
                    withContext(Dispatchers.IO) {
                        val decoded = codec.decode(gateway.readBytes(path))
                        val startupSlots = buildStartupSlots(decoded)
                        val headData = decoded.sections["head.yw"]?.decryptedData
                        val selectedInfo = decoded.sections[selectedSection]?.let { section ->
                            if (headData == null) null else {
                                SaveInfoCodec.parse(section.decryptedData, headData, selectedSection)
                            }
                        }
                        StartupLoadData(decoded, startupSlots, selectedInfo)
                    }
                }.onSuccess { loadedData ->
                    decodedMainBin = loadedData.decoded
                    _uiState.update {
                        it.copy(
                            loading = false,
                            startupSaveSlots = loadedData.startupSlots,
                            playHours = loadedData.selectedInfo?.playHours ?: it.playHours,
                            playMinutes = loadedData.selectedInfo?.playMinutes ?: it.playMinutes,
                            money = loadedData.selectedInfo?.money ?: it.money,
                            playerName = loadedData.selectedInfo?.playerName ?: it.playerName,
                            playerNameError = null,
                            saveYear = loadedData.selectedInfo?.saveYear ?: it.saveYear,
                            saveMonth = loadedData.selectedInfo?.saveMonth ?: it.saveMonth,
                            saveDay = loadedData.selectedInfo?.saveDay ?: it.saveDay,
                            saveHour = loadedData.selectedInfo?.saveHour ?: it.saveHour,
                            saveMinute = loadedData.selectedInfo?.saveMinute ?: it.saveMinute,
                            startupSlotsLoaded = true,
                            message = "セーブ情報を更新しました",
                        )
                    }
                }.onFailure { e ->
                    decodedMainBin = null
                    _uiState.update {
                        it.copy(
                            loading = false,
                            loaded = false,
                            startupSlotsLoaded = false,
                            message = "セーブ情報の読み込み失敗: ${e.message}",
                        )
                    }
                }
            }
        }
    }

    fun save(path: String) {
        viewModelScope.launch {
            fileOperationMutex.withLock {
                val decoded = decodedMainBin ?: run {
                    _uiState.update { it.copy(message = "先に main.bin を読み込んでください") }
                    return@withLock
                }
                val snapshot = _uiState.value
                val sectionName = snapshot.selectedSection
                val section = decoded.sections[sectionName] ?: run {
                    _uiState.update { it.copy(message = "$sectionName が見つかりません") }
                    return@withLock
                }
                val headSection = decoded.sections["head.yw"] ?: run {
                    _uiState.update { it.copy(message = "head.yw が見つかりません") }
                    return@withLock
                }

                _uiState.update { it.copy(saving = true, message = "保存中...") }
                runCatching {
                    withContext(Dispatchers.IO) {
                        val saveInfo = SaveInfo(
                            playHours = snapshot.playHours,
                            playMinutes = snapshot.playMinutes,
                            money = snapshot.money,
                            playerName = snapshot.playerName,
                            saveYear = snapshot.saveYear,
                            saveMonth = snapshot.saveMonth,
                            saveDay = snapshot.saveDay,
                            saveHour = snapshot.saveHour,
                            saveMinute = snapshot.saveMinute,
                        )
                        val withYokai = parser.applyEntries(section.decryptedData, snapshot.entries)
                        val withInventory = snapshot.inventoryItems.fold(withYokai) { current, item ->
                            val originalQuantity = item.rawEntry.getOrNull(8)?.toInt()
                            if (!item.isUsed || item.quantity == originalQuantity) {
                                current
                            } else {
                                inventoryCodec.replaceItemQuantity(
                                    gameData = current,
                                    entryIndex = item.index,
                                    newQuantity = item.quantity,
                                    maximumQuantity = snapshot.safeItemQuantityCeilings[item.index]
                                        ?: error("どうぐ[${item.index}]の安全上限がありません"),
                                )
                            }
                        }
                        val withSasurai = snapshot.sasuraiResidents.fold(withInventory) { current, resident ->
                            if (!resident.isUsed) {
                                current
                            } else {
                                sasuraiCodec.replaceResidentEntry(
                                    gameData = current,
                                    index = resident.index,
                                    rawEntry = resident.rawEntry,
                                )
                            }
                        }
                        val writeResult = SaveInfoCodec.apply(
                            baseGameData = withSasurai,
                            baseHeadData = headSection.decryptedData,
                            sectionName = sectionName,
                            info = saveInfo,
                        )
                        val withGamePatched = codec.replaceSection(decoded, sectionName, writeResult.gameData)
                        val decodedWithGame = codec.decode(withGamePatched)
                        val updatedMainBin = codec.replaceSection(decodedWithGame, "head.yw", writeResult.headData)
                        val verified = codec.decode(updatedMainBin)
                        val loadedData = buildLoadedData(verified, sectionName, path)

                        gateway.backup(path)
                        gateway.writeBytes(path, updatedMainBin)
                        loadedData
                    }
                }.onSuccess { loadedData ->
                    decodedMainBin = loadedData.decoded
                    applyLoadedData(
                        loadedData,
                        message = "$sectionName を保存しました（バックアップ作成済み）",
                    )
                }.onFailure { e ->
                    _uiState.update { it.copy(saving = false, message = "保存失敗: ${e.message}") }
                }
            }
        }
    }

    fun setSection(sectionName: String) {
        if (sectionName !in editableSections) return

        val decoded = decodedMainBin
        if (decoded == null) {
            _uiState.update { it.copy(selectedSection = sectionName) }
            return
        }

        val target = decoded.sections[sectionName]
        if (target == null) {
            _uiState.update {
                it.copy(
                    selectedSection = sectionName,
                    loaded = false,
                    entries = emptyList(),
                    inventoryItems = emptyList(),
                    equipmentItems = emptyList(),
                    keyItems = emptyList(),
                    gashaStates = emptyList(),
                    sasuraiResidents = emptyList(),
                    safeItemQuantityCeilings = emptyMap(),
                    expandedYokaiSlot = null,
                    selectedSlot = null,
                    message = "$sectionName は main.bin 内に見つかりません",
                )
            }
            return
        }

        val entries = parser.parse(target.decryptedData)
        val domains = parseSaveDomains(target.decryptedData)
        val headSection = decoded.sections["head.yw"]
        val saveInfo = headSection?.let {
            SaveInfoCodec.parse(target.decryptedData, it.decryptedData, sectionName)
        }
        _uiState.update {
            it.copy(
                selectedSection = sectionName,
                loaded = true,
                entries = entries,
                inventoryItems = domains.inventoryItems,
                equipmentItems = domains.equipmentItems,
                keyItems = domains.keyItems,
                gashaStates = domains.gashaStates,
                sasuraiResidents = domains.sasuraiResidents,
                safeItemQuantityCeilings = safeQuantityCeilings(domains.inventoryItems),
                attitudes = masterData.attitudes,
                expandedYokaiSlot = null,
                selectedSlot = entries.firstOrNull()?.slot,
                playHours = saveInfo?.playHours ?: it.playHours,
                playMinutes = saveInfo?.playMinutes ?: it.playMinutes,
                money = saveInfo?.money ?: it.money,
                playerName = saveInfo?.playerName ?: it.playerName,
                playerNameError = null,
                saveYear = saveInfo?.saveYear ?: it.saveYear,
                saveMonth = saveInfo?.saveMonth ?: it.saveMonth,
                saveDay = saveInfo?.saveDay ?: it.saveDay,
                saveHour = saveInfo?.saveHour ?: it.saveHour,
                saveMinute = saveInfo?.saveMinute ?: it.saveMinute,
                message = "$sectionName に切り替えました",
                hasUnsavedChanges = false,
            )
        }
    }

    fun openEditorForSection(path: String, sectionName: String) {
        if (sectionName !in editableSections) return
        setSection(sectionName)
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.Editor,
                selectedTopTab = EditorTopTab.Yokai,
                expandedYokaiSlot = null,
                hasUnsavedChanges = false,
            )
        }
        load(path)
    }

    fun backToStartup() {
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.Startup,
                startupSlotsLoaded = false,
                hasUnsavedChanges = false,
            )
        }
    }

    fun refreshBackups(path: String) {
        viewModelScope.launch {
            fileOperationMutex.withLock {
                _uiState.update { it.copy(backupsLoading = true, message = "バックアップ一覧を取得中...") }
                runCatching {
                    withContext(Dispatchers.IO) { gateway.listManagedBackups(path) }
                }.onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            backupsLoading = false,
                            backupItems = items,
                            message = "バックアップ一覧を更新しました",
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            backupsLoading = false,
                            message = "バックアップ一覧取得失敗: ${e.message}",
                        )
                    }
                }
            }
        }
    }

    fun createBackup(path: String, backupName: String, backupEpochMillis: Long) {
        viewModelScope.launch {
            fileOperationMutex.withLock {
                _uiState.update { it.copy(backupsCreating = true, message = "バックアップ作成中...") }
                runCatching {
                    withContext(Dispatchers.IO) {
                        gateway.createManagedBackup(path, backupEpochMillis, backupName)
                        gateway.listManagedBackups(path)
                    }
                }.onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            backupsCreating = false,
                            backupItems = items,
                            message = "バックアップを作成しました",
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            backupsCreating = false,
                            message = "バックアップ作成失敗: ${e.message}",
                        )
                    }
                }
            }
        }
    }

    fun restoreBackup(path: String, backupFileName: String) {
        val sectionName = _uiState.value.selectedSection
        if (decodedMainBin == null) {
            _uiState.update { it.copy(message = "先に main.bin を読み込んでください") }
            return
        }

        viewModelScope.launch {
            fileOperationMutex.withLock {
                _uiState.update { it.copy(backupsRestoring = true, message = "バックアップを復元中...") }
                runCatching {
                    withContext(Dispatchers.IO) {
                        val backupBytes = gateway.readManagedBackup(path, backupFileName)
                        val decoded = codec.decode(backupBytes)
                        val loadedData = buildLoadedData(decoded, sectionName, path)
                        gateway.backup(path)
                        gateway.writeBytes(path, backupBytes)
                        loadedData
                    }
                }.onSuccess { loadedData ->
                    decodedMainBin = loadedData.decoded
                    applyLoadedData(loadedData, message = "バックアップを復元しました")
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            backupsRestoring = false,
                            message = "バックアップ復元失敗: ${e.message}",
                        )
                    }
                }
            }
        }
    }

    fun selectTopTab(tab: EditorTopTab) {
        _uiState.update {
            it.copy(
                selectedTopTab = tab,
                expandedYokaiSlot = if (tab == EditorTopTab.Yokai) it.expandedYokaiSlot else null,
            )
        }
    }

    fun toggleYokaiExpanded(slot: Int) {
        _uiState.update {
            it.copy(expandedYokaiSlot = if (it.expandedYokaiSlot == slot) null else slot)
        }
    }

    fun select(slot: Int) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun updateItemQuantity(entryIndex: Int, quantity: Int) {
        if (isFileOperationBusy()) return
        _uiState.update { state ->
            val ceiling = state.safeItemQuantityCeilings[entryIndex] ?: return@update state
            if (ceiling < 1) return@update state
            val normalized = quantity.coerceIn(1, ceiling)
            val updated = state.inventoryItems.map { item ->
                if (item.index == entryIndex && item.isUsed) item.copy(quantity = normalized) else item
            }
            state.copy(
                inventoryItems = updated,
                hasUnsavedChanges = state.hasUnsavedChanges || updated != state.inventoryItems,
            )
        }
    }

    fun updateSasuraiEncounter(residentIndex: Int, encounterId: Long) {
        if (isFileOperationBusy()) return
        _uiState.update { state ->
            val option = state.sasuraiEncounterOptions.firstOrNull { it.encounterId == encounterId }
                ?: return@update state
            val updated = state.sasuraiResidents.map { resident ->
                if (resident.index == residentIndex && resident.isUsed) {
                    sasuraiCodec.replaceEncounter(resident, option)
                } else {
                    resident
                }
            }
            state.copy(
                sasuraiResidents = updated,
                hasUnsavedChanges = state.hasUnsavedChanges ||
                    updated.map { it.encounterId } != state.sasuraiResidents.map { it.encounterId },
            )
        }
    }

    fun setCheatMode(enabled: Boolean) {
        if (isFileOperationBusy()) return
        _uiState.update { state ->
            val normalizedEntries = if (enabled) {
                state.entries
            } else {
                state.entries.map { normalizeForNormalMode(it) }
            }
            state.copy(
                isCheatMode = enabled,
                entries = normalizedEntries,
                hasUnsavedChanges = state.hasUnsavedChanges || normalizedEntries != state.entries,
            )
        }
    }

    fun updateLevel(slot: Int, level: Int) {
        val max = if (_uiState.value.isCheatMode) CHEAT_STAT_MAX else NORMAL_LEVEL_MAX
        updateEntry(slot) { it.copy(level = clampToRange(level, max)) }
    }

    fun updateAttackLevel(slot: Int, attackLevel: Int) {
        updateEntry(slot) { it.copy(attackLevel = clampToRange(attackLevel, 99)) }
    }

    fun updateTechniqueLevel(slot: Int, techniqueLevel: Int) {
        updateEntry(slot) { it.copy(techniqueLevel = clampToRange(techniqueLevel, 99)) }
    }

    fun updateSoultimateLevel(slot: Int, soultimateLevel: Int) {
        updateEntry(slot) { it.copy(soultimateLevel = clampToRange(soultimateLevel, 99)) }
    }

    fun updateAttitude(slot: Int, attitudeId: Int) {
        updateEntry(slot) { entry ->
            entry.copy(attitudeId = clampToRange(attitudeId, 255))
        }
    }

    fun updateYokai(slot: Int, yokaiId: Long) {
        updateEntry(slot) { entry ->
            val detail = masterData.detailById[yokaiId]
            entry.copy(
                id = yokaiId,
                name = masterData.nameById[yokaiId] ?: entry.name,
                baseStats = detail?.baseStats,
                growPattern = detail?.growPattern,
                yokaiClass = detail?.yokaiClass,
            )
        }
    }

    fun updateMajimeCorrection(slot: Int, correction: Int) {
        updateEntry(slot) { entry ->
            entry.copy(majimeCorrection = clampToRange(correction, 255))
        }
    }

    fun setStateFlag(slot: Int, mask: Int, enabled: Boolean) {
        updateEntry(slot) { entry ->
            val current = entry.stateFlags and 0xFF
            val normalizedMask = mask and 0xFF
            val updated = if (enabled) {
                current or normalizedMask
            } else {
                current and normalizedMask.inv()
            }
            entry.copy(stateFlags = updated and 0xFF)
        }
    }

    fun updateStat(slot: Int, group: StatGroup, index: Int, value: Int) {
        val isCheatMode = _uiState.value.isCheatMode
        updateEntry(slot) { entry ->
            when (group) {
                StatGroup.IVA -> {
                    if (isCheatMode) {
                        val updated = applyStatUpdate(
                            stat = entry.iva,
                            index = index,
                            requested = value,
                            cellMax = CHEAT_STAT_MAX,
                            totalMax = null,
                        )
                        return@updateEntry entry.copy(iva = updated)
                    }

                    val editableMask = ivaEditableMask(entry.yokaiClass)
                    val masked = applyIvaMask(entry.iva, editableMask)
                    if (!editableMask.getOrElse(index) { false }) {
                        return@updateEntry entry.copy(iva = masked)
                    }
                    val updated = applyStatUpdate(
                        stat = masked,
                        index = index,
                        requested = value,
                        cellMax = CHEAT_STAT_MAX,
                        totalMax = NORMAL_IVA_TOTAL_MAX,
                    )
                    entry.copy(iva = applyIvaMask(updated, editableMask))
                }

                StatGroup.IVB1 -> {
                    val updated = applyStatUpdate(
                        stat = entry.ivb1,
                        index = index,
                        requested = value,
                        cellMax = NORMAL_IVB_MAX,
                        totalMax = if (isCheatMode) null else NORMAL_IVB1_TOTAL_MAX,
                    )
                    entry.copy(ivb1 = updated)
                }

                StatGroup.IVB2 -> {
                    val updated = applyStatUpdate(
                        stat = entry.ivb2,
                        index = index,
                        requested = value,
                        cellMax = NORMAL_IVB_MAX,
                        totalMax = null,
                    )
                    entry.copy(ivb2 = updated)
                }

                StatGroup.CB -> {
                    val updated = applyStatUpdate(
                        stat = entry.cb,
                        index = index,
                        requested = value,
                        cellMax = CHEAT_STAT_MAX,
                        totalMax = if (isCheatMode) null else NORMAL_CB_TOTAL_MAX,
                    )
                    entry.copy(cb = updated)
                }
            }
        }
    }

    fun updatePlayHours(value: Int) {
        if (isFileOperationBusy()) return
        _uiState.update { state ->
            val updated = value.coerceAtLeast(0)
            state.copy(
                playHours = updated,
                hasUnsavedChanges = state.hasUnsavedChanges || updated != state.playHours,
            )
        }
    }

    fun updatePlayMinutes(value: Int) {
        if (isFileOperationBusy()) return
        _uiState.update { state ->
            val updated = value.coerceIn(0, 59)
            state.copy(
                playMinutes = updated,
                hasUnsavedChanges = state.hasUnsavedChanges || updated != state.playMinutes,
            )
        }
    }

    fun updateMoney(value: Int) {
        if (isFileOperationBusy()) return
        _uiState.update { state ->
            val updated = value.coerceIn(0, SaveInfoCodec.MONEY_MAX)
            state.copy(
                money = updated,
                hasUnsavedChanges = state.hasUnsavedChanges || updated != state.money,
            )
        }
    }

    fun updatePlayerName(value: String) {
        if (isFileOperationBusy()) return
        val truncated = SaveInfoCodec.truncatePlayerName(value)
        val limited = truncated == value
        _uiState.update { state ->
            state.copy(
                playerName = truncated,
                playerNameError = if (limited) null else "プレイヤー名はUTF-8で最大23バイトです",
                hasUnsavedChanges = state.hasUnsavedChanges || truncated != state.playerName,
            )
        }
    }

    fun updateSaveYear(value: Int) {
        if (isFileOperationBusy()) return
        _uiState.update { state -> state.withSaveYear(value.coerceIn(0, 9999)) }
    }

    fun updateSaveMonth(value: Int) {
        if (isFileOperationBusy()) return
        _uiState.update { state -> state.withSaveMonth(value.coerceIn(1, 12)) }
    }

    fun updateSaveDay(value: Int) {
        if (isFileOperationBusy()) return
        _uiState.update { state -> state.withSaveDay(value.coerceIn(1, 31)) }
    }

    fun updateSaveHour(value: Int) {
        if (isFileOperationBusy()) return
        _uiState.update { state -> state.withSaveHour(value.coerceIn(0, 23)) }
    }

    fun updateSaveMinute(value: Int) {
        if (isFileOperationBusy()) return
        _uiState.update { state -> state.withSaveMinute(value.coerceIn(0, 59)) }
    }

    private fun updateEntry(slot: Int, updater: (YokaiEntry) -> YokaiEntry) {
        if (isFileOperationBusy()) return
        _uiState.update { state ->
            val updated = state.entries.map { if (it.slot == slot) updater(it) else it }
            state.copy(
                entries = updated,
                hasUnsavedChanges = state.hasUnsavedChanges || updated != state.entries,
            )
        }
    }

    private fun clampToRange(value: Int, max: Int): Int {
        return when {
            value < 0 -> 0
            value > max -> max
            else -> value
        }
    }

    private fun isFileOperationBusy(): Boolean {
        return _uiState.value.let {
            it.loading ||
                it.saving ||
                it.backupsLoading ||
                it.backupsCreating ||
                it.backupsRestoring
        }
    }

    private fun applyStatUpdate(
        stat: Stat5,
        index: Int,
        requested: Int,
        cellMax: Int,
        totalMax: Int?,
    ): Stat5 {
        if (index !in 0..4) return stat
        val clampedRequested = clampToRange(requested, cellMax)
        if (totalMax == null) return stat.update(index, clampedRequested)

        val currentValues = stat.values()
        val otherTotal = currentValues
            .filterIndexed { i, _ -> i != index }
            .sum()
        val allowed = (totalMax - otherTotal).coerceIn(0, cellMax)
        return stat.update(index, clampedRequested.coerceAtMost(allowed))
    }

    private fun normalizeForNormalMode(entry: YokaiEntry): YokaiEntry {
        val ivaMask = ivaEditableMask(entry.yokaiClass)
        val normalizedIva = normalizeStatForNormal(
            applyIvaMask(entry.iva, ivaMask),
            CHEAT_STAT_MAX,
            NORMAL_IVA_TOTAL_MAX,
        )
        val normalizedIvb1 = normalizeStatForNormal(entry.ivb1, NORMAL_IVB_MAX, NORMAL_IVB1_TOTAL_MAX)
        val normalizedIvb2 = normalizeStatForNormal(entry.ivb2, NORMAL_IVB_MAX, totalMax = null)
        val normalizedCb = normalizeStatForNormal(entry.cb, CHEAT_STAT_MAX, NORMAL_CB_TOTAL_MAX)

        return entry.copy(
            level = clampToRange(entry.level, NORMAL_LEVEL_MAX),
            iva = normalizedIva,
            ivb1 = normalizedIvb1,
            ivb2 = normalizedIvb2,
            cb = normalizedCb,
        )
    }

    private fun normalizeStatForNormal(stat: Stat5, cellMax: Int, totalMax: Int?): Stat5 {
        val values = stat.values().map { clampToRange(it, cellMax) }
        if (totalMax == null) {
            return statFromValues(values)
        }

        var remaining = totalMax
        val normalized = values.map { value ->
            val allowed = value.coerceAtMost(remaining)
            remaining -= allowed
            allowed
        }
        return statFromValues(normalized)
    }

    private fun statFromValues(values: List<Int>): Stat5 {
        return Stat5(
            hp = values.getOrElse(0) { 0 },
            power = values.getOrElse(1) { 0 },
            spirit = values.getOrElse(2) { 0 },
            defense = values.getOrElse(3) { 0 },
            speed = values.getOrElse(4) { 0 },
        )
    }

    private fun ivaEditableMask(yokaiClass: Int?): List<Boolean> {
        return IVA_EDITABLE_BY_CLASS[yokaiClass] ?: listOf(true, true, true, true, true)
    }

    private fun applyIvaMask(stat: Stat5, editableMask: List<Boolean>): Stat5 {
        val masked = stat.values().mapIndexed { index, value ->
            if (editableMask.getOrElse(index) { false }) value else 0
        }
        return statFromValues(masked)
    }

    private fun buildLoadedData(
        decoded: MainBinDecoded,
        sectionName: String,
        path: String,
    ): LoadedData {
        val section = decoded.sections[sectionName] ?: error("$sectionName が見つかりません")
        val headSection = decoded.sections["head.yw"] ?: error("head.yw が見つかりません")
        val entries = parser.parse(section.decryptedData)
        val domains = parseSaveDomains(section.decryptedData)
        val saveInfo = SaveInfoCodec.parse(
            section.decryptedData,
            headSection.decryptedData,
            sectionName,
        )
        return LoadedData(
            decoded = decoded,
            entries = entries,
            domains = domains,
            saveInfo = saveInfo,
            startupSlots = buildStartupSlots(decoded),
            backupItems = gateway.listManagedBackups(path),
        )
    }

    private fun applyLoadedData(loadedData: LoadedData, message: String) {
        _uiState.update {
            it.copy(
                loading = false,
                saving = false,
                backupsRestoring = false,
                loaded = true,
                entries = loadedData.entries,
                inventoryItems = loadedData.domains.inventoryItems,
                equipmentItems = loadedData.domains.equipmentItems,
                keyItems = loadedData.domains.keyItems,
                gashaStates = loadedData.domains.gashaStates,
                sasuraiResidents = loadedData.domains.sasuraiResidents,
                sasuraiEncounterOptions = saveDomainMasterData.sasuraiEncounterOptions,
                itemNames = saveDomainMasterData.itemNames,
                equipmentNames = saveDomainMasterData.equipmentNames,
                keyItemNames = saveDomainMasterData.keyItemNames,
                safeItemQuantityCeilings = safeQuantityCeilings(loadedData.domains.inventoryItems),
                attitudes = masterData.attitudes,
                expandedYokaiSlot = null,
                selectedSlot = loadedData.entries.firstOrNull()?.slot,
                startupSaveSlots = loadedData.startupSlots,
                backupItems = loadedData.backupItems,
                playHours = loadedData.saveInfo.playHours,
                playMinutes = loadedData.saveInfo.playMinutes,
                money = loadedData.saveInfo.money,
                playerName = loadedData.saveInfo.playerName,
                playerNameError = null,
                saveYear = loadedData.saveInfo.saveYear,
                saveMonth = loadedData.saveInfo.saveMonth,
                saveDay = loadedData.saveInfo.saveDay,
                saveHour = loadedData.saveInfo.saveHour,
                saveMinute = loadedData.saveInfo.saveMinute,
                message = message,
                hasUnsavedChanges = false,
            )
        }
    }

    private fun EditorUiState.withSaveYear(value: Int) = copy(
        saveYear = value,
        hasUnsavedChanges = hasUnsavedChanges || value != saveYear,
    )

    private fun EditorUiState.withSaveMonth(value: Int) = copy(
        saveMonth = value,
        hasUnsavedChanges = hasUnsavedChanges || value != saveMonth,
    )

    private fun EditorUiState.withSaveDay(value: Int) = copy(
        saveDay = value,
        hasUnsavedChanges = hasUnsavedChanges || value != saveDay,
    )

    private fun EditorUiState.withSaveHour(value: Int) = copy(
        saveHour = value,
        hasUnsavedChanges = hasUnsavedChanges || value != saveHour,
    )

    private fun EditorUiState.withSaveMinute(value: Int) = copy(
        saveMinute = value,
        hasUnsavedChanges = hasUnsavedChanges || value != saveMinute,
    )

    private fun buildYokaiOptions(data: YokaiMasterData): List<YokaiOption> {
        return data.nameById
            .map { (id, name) -> YokaiOption(id = id, name = name) }
    }

    private fun parseSaveDomains(gameData: ByteArray): SaveDomains {
        val inventory = inventoryCodec.decode(gameData)
        return SaveDomains(
            inventoryItems = inventory.items,
            equipmentItems = inventory.equipment,
            keyItems = inventory.keyItems,
            gashaStates = gashaCodec.decode(gameData),
            sasuraiResidents = sasuraiCodec.decode(gameData),
        )
    }

    private fun safeQuantityCeilings(items: List<InventoryItemEntry>): Map<Int, Int> {
        return items.asSequence().filter { it.isUsed }.associate { it.index to it.quantity }
    }

    private fun buildStartupSlots(decoded: MainBinDecoded): List<SaveSlotCard> {
        return DEFAULT_STARTUP_SLOTS.map { base ->
            val section = decoded.sections[base.sectionName]
            if (section == null) {
                base.copy(
                    hasData = false,
                    displayName = "データがありません",
                    playTimeText = "データがありません",
                    saveDateText = "データがありません",
                    yokaiCount = null,
                )
            } else {
                val entries = parser.parse(section.decryptedData)
                val headData = decoded.sections["head.yw"]?.decryptedData
                val saveInfo = headData?.let { SaveInfoCodec.parse(section.decryptedData, it, base.sectionName) }
                val playTimeText = saveInfo?.let { "%d時間%02d分".format(it.playHours, it.playMinutes) }
                    ?: "未解析"
                val saveDateText = saveInfo?.let {
                    "%04d/%02d/%02d %02d:%02d".format(
                        it.saveYear,
                        it.saveMonth,
                        it.saveDay,
                        it.saveHour,
                        it.saveMinute,
                    )
                } ?: "未解析"
                base.copy(
                    hasData = true,
                    displayName = saveInfo?.playerName?.ifBlank { null },
                    playTimeText = playTimeText,
                    saveDateText = saveDateText,
                    yokaiCount = entries.size,
                )
            }
        }
    }
}

private data class LoadedData(
    val decoded: MainBinDecoded,
    val entries: List<YokaiEntry>,
    val domains: SaveDomains,
    val saveInfo: SaveInfo,
    val startupSlots: List<SaveSlotCard>,
    val backupItems: List<MainBinBackupInfo>,
)

private data class SaveDomains(
    val inventoryItems: List<InventoryItemEntry>,
    val equipmentItems: List<EquipmentEntry>,
    val keyItems: List<KeyItemEntry>,
    val gashaStates: List<GashaStateEntry>,
    val sasuraiResidents: List<SasuraiResident>,
)

private data class StartupLoadData(
    val decoded: MainBinDecoded,
    val startupSlots: List<SaveSlotCard>,
    val selectedInfo: SaveInfo?,
)
