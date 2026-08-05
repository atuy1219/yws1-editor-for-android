package com.atuy.yws1editor

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.atuy.yws1editor.shizuku.ShizukuFileServiceClient
import com.atuy.yws1editor.ui.theme.YwEditorTheme
import com.atuy.yws1editor.yokai.DailyBattleCodec
import com.atuy.yws1editor.yokai.DailyBattleEntry
import com.atuy.yws1editor.yokai.MainBinCodec
import com.atuy.yws1editor.yokai.ShizukuFileGateway
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

internal data class DailyBattleUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val loaded: Boolean = false,
    val message: String = "",
    val sections: List<String> = emptyList(),
    val selectedSection: String = "game1.yw",
    val entries: List<DailyBattleEntry> = emptyList(),
    val mirrorGame0: Boolean = true,
    val dirty: Boolean = false,
)

private data class DailyBattleLoadResult(
    val rawMainBin: ByteArray,
    val sections: List<String>,
    val selectedSection: String,
    val entries: List<DailyBattleEntry>,
)

class DailyBattleActivity : ComponentActivity() {
    private val gateway = ShizukuFileGateway()
    private val mainBinCodec = MainBinCodec()
    private val dailyBattleCodec = DailyBattleCodec()

    private var shizukuGranted by mutableStateOf(false)
    private var shizukuStatusMessage by mutableStateOf("Shizukuへ接続していません")
    private var editorState by mutableStateOf(DailyBattleUiState())
    private var rawMainBin: ByteArray? = null
    private var permissionRequestPending = false

    private val mainBinPath: String by lazy {
        val currentUserDataRoot = requireNotNull(File(applicationInfo.dataDir).parentFile) {
            "現在ユーザーのデータディレクトリを解決できません"
        }
        File(currentUserDataRoot, "jp.co.level5.yws1/files/save/main.bin").path
    }

    private val requestCode = 2001

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        requestShizukuPermissionIfNeeded()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        permissionRequestPending = false
        shizukuGranted = false
        shizukuStatusMessage = "Shizukuとの接続が切れました"
        ShizukuFileServiceClient.reset()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode != this.requestCode) return@OnRequestPermissionResultListener
        runOnUiThread {
            permissionRequestPending = false
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                connectFileServiceIfRoot()
            } else {
                shizukuGranted = false
                shizukuStatusMessage = "Shizukuの許可が拒否されました"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        enableEdgeToEdge()
        setContent {
            YwEditorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DailyBattleScreen(
                        state = editorState,
                        shizukuGranted = shizukuGranted,
                        shizukuStatusMessage = shizukuStatusMessage,
                        onClose = ::finish,
                        onRetryShizuku = ::requestShizukuPermissionIfNeeded,
                        onReload = ::loadDailyBattleData,
                        onSelectSection = ::selectSection,
                        onMirrorGame0Change = { checked ->
                            editorState = editorState.copy(mirrorGame0 = checked)
                        },
                        onToggle = ::toggleEntry,
                        onSetAllAvailable = { setAll(foughtToday = false) },
                        onSetAllFought = { setAll(foughtToday = true) },
                        onSave = ::saveDailyBattleData,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestShizukuPermissionIfNeeded()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        ShizukuFileServiceClient.setStateListener(null)
        super.onDestroy()
    }

    private fun connectFileServiceIfRoot() {
        val uid = gateway.serverUid()
        if (uid != 0) {
            shizukuGranted = false
            shizukuStatusMessage = if (uid == 2000) {
                "ShizukuはADBモードです。この編集にはrootモードが必要です"
            } else {
                "Shizukuの実行権限を確認できません"
            }
            ShizukuFileServiceClient.reset()
            return
        }

        shizukuStatusMessage = "Shizukuファイルサービスへ接続中..."
        ShizukuFileServiceClient.setStateListener { ready ->
            runOnUiThread {
                shizukuGranted = ready
                shizukuStatusMessage = if (ready) "" else "Shizukuファイルサービスへ接続できません"
                if (ready && !editorState.loaded && !editorState.loading) loadDailyBattleData()
            }
        }
        runCatching {
            ShizukuFileServiceClient.bind(applicationContext)
        }.onFailure { error ->
            shizukuGranted = false
            shizukuStatusMessage = "Shizukuファイルサービス接続失敗: ${error.message}"
        }
    }

    private fun requestShizukuPermissionIfNeeded() {
        if (!gateway.isShizukuRunning()) {
            permissionRequestPending = false
            shizukuGranted = false
            shizukuStatusMessage = "Shizukuを起動してください"
            return
        }
        if (gateway.isPreV11()) {
            shizukuGranted = false
            shizukuStatusMessage = "このShizuku APIバージョンには対応していません"
            return
        }
        if (gateway.hasPermission()) {
            permissionRequestPending = false
            connectFileServiceIfRoot()
            return
        }
        shizukuGranted = false
        if (gateway.shouldShowRequestPermissionRationale()) {
            permissionRequestPending = false
            shizukuStatusMessage = "Shizukuアプリの認可済みアプリ画面から許可してください"
            return
        }
        if (!permissionRequestPending) {
            shizukuStatusMessage = "Shizukuの許可を待っています..."
            permissionRequestPending = gateway.requestPermission(requestCode)
            if (!permissionRequestPending) {
                shizukuStatusMessage = "Shizukuの許可要求を開始できませんでした"
            }
        }
    }

    private fun loadDailyBattleData() {
        if (!shizukuGranted || editorState.loading || editorState.saving) return
        editorState = editorState.copy(loading = true, message = "main.bin 読み込み中...")
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val raw = gateway.readBytes(mainBinPath)
                    val decoded = mainBinCodec.decode(raw)
                    val sections = listOf("game0.yw", "game1.yw", "game2.yw", "game3.yw")
                        .filter { it in decoded.sections }
                    if (sections.isEmpty()) error("編集可能なgame*.ywがありません")
                    val preferred = editorState.selectedSection.takeIf { it in sections }
                        ?: sections.firstOrNull { it == "game1.yw" }
                        ?: sections.first()
                    val gameData = requireNotNull(decoded.sections[preferred]).decryptedData
                    DailyBattleLoadResult(raw, sections, preferred, dailyBattleCodec.decode(gameData))
                }
            }.onSuccess { result ->
                rawMainBin = result.rawMainBin
                editorState = editorState.copy(
                    loading = false,
                    loaded = true,
                    message = "",
                    sections = result.sections,
                    selectedSection = result.selectedSection,
                    entries = result.entries,
                    dirty = false,
                )
            }.onFailure { error ->
                rawMainBin = null
                editorState = editorState.copy(
                    loading = false,
                    loaded = false,
                    message = "読み込み失敗: ${error.message}",
                    sections = emptyList(),
                    entries = emptyList(),
                    dirty = false,
                )
            }
        }
    }

    private fun selectSection(sectionName: String) {
        if (sectionName == editorState.selectedSection || sectionName !in editorState.sections) return
        if (editorState.dirty) {
            editorState = editorState.copy(message = "スロットを切り替える前に保存または再読み込みしてください")
            return
        }
        val raw = rawMainBin ?: return
        editorState = editorState.copy(loading = true, message = "$sectionName を読み込み中...")
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    val section = mainBinCodec.decode(raw).sections[sectionName]
                        ?: error("$sectionName がありません")
                    dailyBattleCodec.decode(section.decryptedData)
                }
            }.onSuccess { entries ->
                editorState = editorState.copy(
                    loading = false,
                    selectedSection = sectionName,
                    entries = entries,
                    message = "",
                )
            }.onFailure { error ->
                editorState = editorState.copy(loading = false, message = "切り替え失敗: ${error.message}")
            }
        }
    }

    private fun toggleEntry(flagIndex: Int, foughtToday: Boolean) {
        if (editorState.loading || editorState.saving) return
        val updated = editorState.entries.map { entry ->
            if (entry.definition.flagIndex == flagIndex) entry.copy(foughtToday = foughtToday) else entry
        }
        editorState = editorState.copy(
            entries = updated,
            dirty = editorState.dirty || updated != editorState.entries,
            message = "",
        )
    }

    private fun setAll(foughtToday: Boolean) {
        if (editorState.loading || editorState.saving || !editorState.loaded) return
        val updated = dailyBattleCodec.setAll(editorState.entries, foughtToday)
        editorState = editorState.copy(
            entries = updated,
            dirty = editorState.dirty || updated != editorState.entries,
            message = "",
        )
    }

    private fun saveDailyBattleData() {
        val source = rawMainBin ?: return
        if (!shizukuGranted || !editorState.loaded || editorState.loading || editorState.saving) return
        val selectedSection = editorState.selectedSection
        val entries = editorState.entries
        val mirrorGame0 = editorState.mirrorGame0 && selectedSection != "game0.yw"
        editorState = editorState.copy(saving = true, message = "保存中...")

        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val decoded = mainBinCodec.decode(source)
                    val selected = decoded.sections[selectedSection]
                        ?: error("$selectedSection がありません")
                    val selectedGame = dailyBattleCodec.apply(selected.decryptedData, entries)
                    var updated = mainBinCodec.replaceSection(decoded, selectedSection, selectedGame)

                    if (mirrorGame0) {
                        val withSelected = mainBinCodec.decode(updated)
                        val game0 = withSelected.sections["game0.yw"]
                        if (game0 != null) {
                            val mirroredGame0 = dailyBattleCodec.apply(game0.decryptedData, entries)
                            updated = mainBinCodec.replaceSection(withSelected, "game0.yw", mirroredGame0)
                        }
                    }

                    val verified = mainBinCodec.decode(updated)
                    val verifiedEntries = dailyBattleCodec.decode(
                        requireNotNull(verified.sections[selectedSection]).decryptedData,
                    )
                    if (verifiedEntries != entries) error("保存後の再検証に失敗しました")
                    gateway.backup(mainBinPath)
                    gateway.writeBytes(mainBinPath, updated)
                    updated to verifiedEntries
                }
            }.onSuccess { (updated, verifiedEntries) ->
                rawMainBin = updated
                editorState = editorState.copy(
                    saving = false,
                    entries = verifiedEntries,
                    dirty = false,
                    message = buildString {
                        append("${selectedSection} を保存しました（バックアップ作成済み）")
                        if (mirrorGame0) append(" / game0.ywにも反映")
                    },
                )
            }.onFailure { error ->
                editorState = editorState.copy(saving = false, message = "保存失敗: ${error.message}")
            }
        }
    }
}
