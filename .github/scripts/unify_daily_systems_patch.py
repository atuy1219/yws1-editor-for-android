from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected 1 occurrence, found {count}")
    return text.replace(old, new, 1)


def replace_count(text: str, old: str, new: str, expected: int, label: str) -> str:
    count = text.count(old)
    if count != expected:
        raise RuntimeError(f"{label}: expected {expected} occurrences, found {count}")
    return text.replace(old, new)


root = Path(__file__).resolve().parents[2]
vm_path = root / "app/src/main/java/com/atuy/yws1editor/MainViewModel.kt"
activity_path = root / "app/src/main/java/com/atuy/yws1editor/MainActivity.kt"
manifest_path = root / "app/src/main/AndroidManifest.xml"

vm = vm_path.read_text(encoding="utf-8")
vm = replace_once(
    vm,
    "import com.atuy.yws1editor.yokai.GashaPrizeEntry\n",
    "import com.atuy.yws1editor.yokai.GashaPrizeEntry\n"
    "import com.atuy.yws1editor.yokai.DailySimpleFlag\n"
    "import com.atuy.yws1editor.yokai.DailySystemCodec\n"
    "import com.atuy.yws1editor.yokai.DailySystemState\n",
    "MainViewModel imports",
)
vm = replace_once(
    vm,
    '    Sasurai("さすらい荘"),\n    Info("情報"),',
    '    Sasurai("さすらい荘"),\n    Daily("一日一回系"),\n    Info("情報"),',
    "EditorTopTab.Daily",
)
vm = replace_once(
    vm,
    "    val sasuraiResidents: List<SasuraiResident> = emptyList(),\n"
    "    val sasuraiEncounterOptions: List<SasuraiEncounterOption> = emptyList(),",
    "    val sasuraiResidents: List<SasuraiResident> = emptyList(),\n"
    "    val sasuraiEncounterOptions: List<SasuraiEncounterOption> = emptyList(),\n"
    "    val dailySystems: DailySystemState = DailySystemState.EMPTY,",
    "EditorUiState.dailySystems",
)
vm = replace_once(
    vm,
    "    private val sasuraiCodec = SasuraiCodec()\n"
    "    private val encyclopediaCodec = YokaiEncyclopediaCodec()",
    "    private val sasuraiCodec = SasuraiCodec()\n"
    "    private val dailySystemCodec = DailySystemCodec()\n"
    "    private val encyclopediaCodec = YokaiEncyclopediaCodec()",
    "dailySystemCodec field",
)
vm = replace_count(
    vm,
    "                            sasuraiResidents = emptyList(),\n"
    "                            encyclopediaEntries = emptyList(),",
    "                            sasuraiResidents = emptyList(),\n"
    "                            dailySystems = DailySystemState.EMPTY,\n"
    "                            encyclopediaEntries = emptyList(),",
    2,
    "clear daily state",
)
vm = replace_once(
    vm,
    "                        val withEncyclopedia = encyclopediaCodec.applyEntries(\n"
    "                            gameData = withSasurai,\n"
    "                            entries = snapshot.encyclopediaEntries,\n"
    "                        )",
    "                        val withDailySystems = dailySystemCodec.apply(\n"
    "                            gameData = withSasurai,\n"
    "                            state = snapshot.dailySystems,\n"
    "                        )\n"
    "                        val withEncyclopedia = encyclopediaCodec.applyEntries(\n"
    "                            gameData = withDailySystems,\n"
    "                            entries = snapshot.encyclopediaEntries,\n"
    "                        )",
    "save daily systems",
)
vm = replace_count(
    vm,
    "                sasuraiResidents = domains.sasuraiResidents,\n"
    "                encyclopediaEntries = domains.encyclopediaEntries,",
    "                sasuraiResidents = domains.sasuraiResidents,\n"
    "                dailySystems = domains.dailySystems,\n"
    "                encyclopediaEntries = domains.encyclopediaEntries,",
    1,
    "setSection daily state",
)
vm = replace_count(
    vm,
    "                sasuraiResidents = loadedData.domains.sasuraiResidents,\n"
    "                encyclopediaEntries = loadedData.domains.encyclopediaEntries,",
    "                sasuraiResidents = loadedData.domains.sasuraiResidents,\n"
    "                dailySystems = loadedData.domains.dailySystems,\n"
    "                encyclopediaEntries = loadedData.domains.encyclopediaEntries,",
    1,
    "applyLoadedData daily state",
)
vm = replace_once(
    vm,
    "            sasuraiResidents = sasuraiCodec.decode(gameData),\n"
    "            encyclopediaEntries = encyclopediaCodec.decode(gameData, masterData),",
    "            sasuraiResidents = sasuraiCodec.decode(gameData),\n"
    "            dailySystems = dailySystemCodec.decode(gameData),\n"
    "            encyclopediaEntries = encyclopediaCodec.decode(gameData, masterData),",
    "parseSaveDomains daily state",
)
vm = replace_once(
    vm,
    "    val sasuraiResidents: List<SasuraiResident>,\n"
    "    val encyclopediaEntries: List<YokaiEncyclopediaEntry>,",
    "    val sasuraiResidents: List<SasuraiResident>,\n"
    "    val dailySystems: DailySystemState,\n"
    "    val encyclopediaEntries: List<YokaiEncyclopediaEntry>,",
    "SaveDomains.dailySystems",
)

method_anchor = "    fun updateEncyclopediaMet(yokaiId: Long, met: Boolean) {\n"
daily_methods = '''    fun updateDailyBattle(flagIndex: Int, foughtToday: Boolean) {
        updateDailySystems { state -> dailySystemCodec.setBattle(state, flagIndex, foughtToday) }
    }

    fun setAllDailyBattles(foughtToday: Boolean) {
        updateDailySystems { state -> dailySystemCodec.setAllBattles(state, foughtToday) }
    }

    fun resetDailyGasha() {
        updateDailySystems(
            message = "妖怪ガシャの日次使用回数と報酬フラグをリセットしました",
        ) { state -> dailySystemCodec.resetGasha(state) }
    }

    fun resetDailySasurai() {
        updateDailySystems(
            message = "さすらい荘の日次抽選と報酬数をリセットしました",
        ) { state -> dailySystemCodec.resetSasurai(state) }
    }

    fun updateDailySimpleFlag(definition: DailySimpleFlag, usedToday: Boolean) {
        updateDailySystems { state -> dailySystemCodec.setSimpleFlag(state, definition, usedToday) }
    }

    fun resetAllDailySystems() {
        updateDailySystems(message = "すべての日次制限を解除しました") { state ->
            dailySystemCodec.resetAll(state)
        }
    }

    private fun updateDailySystems(
        message: String? = null,
        updater: (DailySystemState) -> DailySystemState,
    ) {
        if (isFileOperationBusy()) return
        _uiState.update { state ->
            val updated = updater(state.dailySystems)
            state.copy(
                dailySystems = updated,
                hasUnsavedChanges = state.hasUnsavedChanges || updated != state.dailySystems,
                message = message ?: state.message,
            )
        }
    }

'''
vm = replace_once(vm, method_anchor, daily_methods + method_anchor, "daily system mutators")
vm_path.write_text(vm, encoding="utf-8")

activity = activity_path.read_text(encoding="utf-8")
activity = replace_once(
    activity,
    "import com.atuy.yws1editor.yokai.GashaPrizeEntry\n",
    "import com.atuy.yws1editor.yokai.GashaPrizeEntry\n"
    "import com.atuy.yws1editor.yokai.DailySimpleFlag\n",
    "MainActivity imports",
)
activity = replace_once(
    activity,
    "            onSasuraiEncounterChange = mainViewModel::updateSasuraiEncounter,\n"
    "            onEncyclopediaMetChange = mainViewModel::updateEncyclopediaMet,",
    "            onSasuraiEncounterChange = mainViewModel::updateSasuraiEncounter,\n"
    "            onDailyBattleChange = mainViewModel::updateDailyBattle,\n"
    "            onSetAllDailyBattles = mainViewModel::setAllDailyBattles,\n"
    "            onResetDailyGasha = mainViewModel::resetDailyGasha,\n"
    "            onResetDailySasurai = mainViewModel::resetDailySasurai,\n"
    "            onDailySimpleFlagChange = mainViewModel::updateDailySimpleFlag,\n"
    "            onResetAllDailySystems = mainViewModel::resetAllDailySystems,\n"
    "            onEncyclopediaMetChange = mainViewModel::updateEncyclopediaMet,",
    "AppScreen daily callbacks",
)
activity = replace_once(
    activity,
    "    onSasuraiEncounterChange: (Int, Long) -> Unit,\n"
    "    onEncyclopediaMetChange: (Long, Boolean) -> Unit,",
    "    onSasuraiEncounterChange: (Int, Long) -> Unit,\n"
    "    onDailyBattleChange: (Int, Boolean) -> Unit,\n"
    "    onSetAllDailyBattles: (Boolean) -> Unit,\n"
    "    onResetDailyGasha: () -> Unit,\n"
    "    onResetDailySasurai: () -> Unit,\n"
    "    onDailySimpleFlagChange: (DailySimpleFlag, Boolean) -> Unit,\n"
    "    onResetAllDailySystems: () -> Unit,\n"
    "    onEncyclopediaMetChange: (Long, Boolean) -> Unit,",
    "EditorScreen daily callback signature",
)
activity = replace_once(
    activity,
    "                EditorTopTab.Encyclopedia -> EncyclopediaTabContent(",
    "                EditorTopTab.Daily -> DailySystemsTabContent(\n"
    "                    state = state.dailySystems,\n"
    "                    enabled = !fileOperationBusy,\n"
    "                    onBattleChange = onDailyBattleChange,\n"
    "                    onSetAllBattles = onSetAllDailyBattles,\n"
    "                    onResetGasha = onResetDailyGasha,\n"
    "                    onResetSasurai = onResetDailySasurai,\n"
    "                    onSimpleFlagChange = onDailySimpleFlagChange,\n"
    "                    onResetAll = onResetAllDailySystems,\n"
    "                    modifier = Modifier.fillMaxSize(),\n"
    "                )\n\n"
    "                EditorTopTab.Encyclopedia -> EncyclopediaTabContent(",
    "Daily tab branch",
)
activity_path.write_text(activity, encoding="utf-8")

manifest = manifest_path.read_text(encoding="utf-8")
manifest = replace_once(
    manifest,
    '''\n        <activity
            android:name=".DailyBattleActivity"
            android:exported="true"
            android:label="一日一回バトル編集"
            android:theme="@style/Theme.YwEditor">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>''',
    "",
    "remove DailyBattleActivity manifest entry",
)
manifest_path.write_text(manifest, encoding="utf-8")

print("Unified daily systems into the normal editor tab.")
