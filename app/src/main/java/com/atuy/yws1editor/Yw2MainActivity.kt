package com.atuy.yws1editor

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.atuy.yws1editor.shizuku.ShizukuFileServiceClient
import com.atuy.yws1editor.ui.theme.YwEditorTheme
import com.atuy.yws1editor.yokai.ShizukuFileGateway
import com.atuy.yws1editor.yw2.Yw2Crypto
import com.atuy.yws1editor.yw2.Yw2InventoryEntry
import com.atuy.yws1editor.yw2.Yw2InventoryKind
import com.atuy.yws1editor.yw2.Yw2MasterData
import com.atuy.yws1editor.yw2.Yw2NamedId
import com.atuy.yws1editor.yw2.Yw2SaveCodec
import com.atuy.yws1editor.yw2.Yw2SaveFile
import com.atuy.yws1editor.yw2.Yw2SaveStorage
import com.atuy.yws1editor.yw2.Yw2Stats
import com.atuy.yws1editor.yw2.Yw2Yokai
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class Yw2MainActivity : ComponentActivity() {
    private val gateway = ShizukuFileGateway()
    private val requestCode = 2002
    private var shizukuGranted by mutableStateOf(false)
    private var shizukuStatusMessage by mutableStateOf("Shizukuへ接続中...")
    private var permissionRequestPending = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        requestShizukuPermissionIfNeeded()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        permissionRequestPending = false
        shizukuGranted = false
        shizukuStatusMessage = "Shizukuとの接続が切れました"
        ShizukuFileServiceClient.reset()
    }

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
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

        setContent {
            YwEditorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Yw2EditorScreen(
                        gateway = gateway,
                        shizukuGranted = shizukuGranted,
                        shizukuStatusMessage = shizukuStatusMessage,
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
                "ShizukuはADBモードです。Citraセーブの直接編集にはrootモードが必要です"
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
                shizukuStatusMessage =
                    if (ready) "" else "Shizukuファイルサービスへ接続できません"
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
}

private data class Yw2Session(
    val uri: Uri? = null,
    val directFile: Yw2SaveFile? = null,
    val fileName: String,
    val originalEncrypted: ByteArray,
    val decoded: ByteArray,
    val keyMode: Yw2Crypto.KeyMode,
    val headBytes: ByteArray? = null,
)

private enum class Yw2Tab(val label: String) {
    YOKAI("妖怪"),
    ITEM("どうぐ"),
    EQUIPMENT("そうび"),
    IMPORTANT("だいじ"),
    SOUL("魂"),
    INFO("セーブ"),
}

@Composable
private fun Yw2EditorScreen(
    gateway: ShizukuFileGateway,
    shizukuGranted: Boolean,
    shizukuStatusMessage: String,
) {
    val context = LocalContext.current
    val master = remember { Yw2MasterData(context) }
    val scope = rememberCoroutineScope()
    val directStorage = remember(gateway) { Yw2SaveStorage(gateway) }

    var session by remember { mutableStateOf<Yw2Session?>(null) }
    var headBytes by remember { mutableStateOf<ByteArray?>(null) }
    var headName by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("CitraのYW2セーブを自動検索します") }
    var busy by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var discoveredGames by remember { mutableStateOf<List<Yw2SaveFile>>(emptyList()) }
    var directHeadPath by remember { mutableStateOf<String?>(null) }

    val openDirectSave: (Yw2SaveFile) -> Unit = { file ->
        scope.launch {
            busy = true
            runCatching {
                withContext(Dispatchers.IO) {
                    directStorage.load(file, directHeadPath)
                }
            }.onSuccess { loaded ->
                headBytes = loaded.headBytes
                headName = if (loaded.headBytes != null) "head.yw (自動)" else null
                session = Yw2Session(
                    directFile = loaded.file,
                    fileName = loaded.file.fileName,
                    originalEncrypted = loaded.encrypted,
                    decoded = loaded.decoded,
                    keyMode = loaded.keyMode,
                    headBytes = loaded.headBytes,
                )
                status = loaded.file.fileName + " / " + loaded.keyMode.label + " / 直接編集"
                selectedTab = 0
            }.onFailure {
                status = "自動読込失敗: " + (it.message ?: it::class.java.simpleName)
            }
            busy = false
        }
    }

    val headPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("head.yw を読み込めません")
            }.onSuccess {
                headBytes = it
                headName = displayName(uri)
                status = "head.yw 読込: " + (headName ?: uri.lastPathSegment.orEmpty())
            }.onFailure {
                status = "head.yw 読込失敗: " + (it.message ?: it::class.java.simpleName)
            }
        }
    }

    val gamePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                val encrypted = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("game*.yw を読み込めません")
                val decoded = Yw2Crypto.decrypt(encrypted, headBytes)
                // Cryptographic validation already happened in decrypt().
                // Parse the core yokai area here; inventory sections are loaded
                // independently by their tabs.
                Yw2SaveCodec.parseYokai(decoded.data)
                Yw2Session(
                    uri = uri,
                    fileName = displayName(uri) ?: uri.lastPathSegment ?: "game.yw",
                    originalEncrypted = encrypted,
                    decoded = decoded.data,
                    keyMode = decoded.keyMode,
                    headBytes = headBytes,
                )
            }.onSuccess {
                session = it
                status = it.fileName + " / " + it.keyMode.label
                selectedTab = 0
            }.onFailure {
                status = "読込失敗: " + (it.message ?: it::class.java.simpleName)
            }
            busy = false
        }
    }

    LaunchedEffect(shizukuGranted) {
        if (!shizukuGranted) return@LaunchedEffect

        busy = true
        runCatching {
            withContext(Dispatchers.IO) { directStorage.discover() }
        }.onSuccess { discovery ->
            discoveredGames = discovery.gameFiles
            directHeadPath = discovery.headPath
            when (discovery.gameFiles.size) {
                0 -> {
                    status = "指定パスに game*.yw が見つかりません: " + discovery.directory
                }
                1 -> {
                    val file = discovery.gameFiles.single()
                    runCatching {
                        withContext(Dispatchers.IO) {
                            directStorage.load(file, discovery.headPath)
                        }
                    }.onSuccess { loaded ->
                        headBytes = loaded.headBytes
                        headName = if (loaded.headBytes != null) "head.yw (自動)" else null
                        session = Yw2Session(
                            directFile = loaded.file,
                            fileName = loaded.file.fileName,
                            originalEncrypted = loaded.encrypted,
                            decoded = loaded.decoded,
                            keyMode = loaded.keyMode,
                            headBytes = loaded.headBytes,
                        )
                        status = loaded.file.fileName + " / " + loaded.keyMode.label + " / 自動読込"
                        selectedTab = 0
                    }.onFailure {
                        status = "自動読込失敗: " + (it.message ?: it::class.java.simpleName)
                    }
                }
                else -> {
                    status = "game*.yw を " + discovery.gameFiles.size + " 件検出しました。編集するセーブを選択してください"
                }
            }
        }.onFailure {
            discoveredGames = emptyList()
            directHeadPath = null
            status = "自動検索失敗: " + (it.message ?: it::class.java.simpleName)
        }
        busy = false
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("妖怪ウォッチ2 真打 セーブエディタ", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { gamePicker.launch(arrayOf("*/*")) },
                    enabled = !busy,
                ) {
                    Text("game*.ywを開く")
                }
                OutlinedButton(
                    onClick = { headPicker.launch(arrayOf("*/*")) },
                    enabled = !busy,
                ) {
                    Text(if (headName == null) "head.yw" else "head: " + headName)
                }
                if (session != null) {
                    Button(
                        onClick = {
                            val current = session ?: return@Button
                            scope.launch {
                                busy = true
                                runCatching {
                                    val directFile = current.directFile
                                    if (directFile != null) {
                                        val result = withContext(Dispatchers.IO) {
                                            directStorage.save(
                                                file = directFile,
                                                decoded = current.decoded,
                                                keyMode = current.keyMode,
                                                headBytes = current.headBytes,
                                            )
                                        }
                                        result.encrypted to result.backupPath
                                    } else {
                                        withContext(Dispatchers.IO) {
                                            val saveHead = current.headBytes ?: headBytes
                                            val originalDecoded =
                                                Yw2Crypto.decrypt(current.originalEncrypted, saveHead)
                                            val noEditRoundTrip = Yw2Crypto.encrypt(
                                                originalDecoded.data,
                                                originalDecoded.keyMode,
                                                saveHead,
                                            )
                                            check(noEditRoundTrip.contentEquals(current.originalEncrypted)) {
                                                "安全のため保存を中止しました: 無編集再暗号化が元のgame*.ywと一致しません"
                                            }

                                            val backupDir = File(context.filesDir, "yw2-backups").apply { mkdirs() }
                                            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                            val safeName =
                                                current.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                                            val backup = File(backupDir, safeName + "." + stamp + ".bak")
                                            backup.writeBytes(current.originalEncrypted)

                                            val encrypted =
                                                Yw2Crypto.encrypt(current.decoded, current.keyMode, saveHead)
                                            val verify = Yw2Crypto.decrypt(encrypted, saveHead)
                                            Yw2SaveCodec.parseYokai(verify.data)
                                            val uri = current.uri
                                                ?: error("保存先URIを取得できません")
                                            context.contentResolver.openOutputStream(uri, "wt")?.use {
                                                it.write(encrypted)
                                                it.flush()
                                            } ?: error("選択ファイルへ書き込めません")
                                            encrypted to backup.path
                                        }
                                    }
                                }.onSuccess { (encrypted, backupPath) ->
                                    session = current.copy(originalEncrypted = encrypted)
                                    status = "保存完了 / バックアップ: " + backupPath
                                }.onFailure {
                                    status = "保存失敗: " + (it.message ?: it::class.java.simpleName)
                                }
                                busy = false
                            }
                        },
                        enabled = !busy,
                    ) {
                        Text("保存")
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(status, style = MaterialTheme.typography.bodySmall)
            if (busy) {
                Spacer(Modifier.height(6.dp))
                CircularProgressIndicator()
            }

            val current = session
            if (current == null) {
                Spacer(Modifier.height(16.dp))
                if (!shizukuGranted) {
                    Text(
                        shizukuStatusMessage.ifBlank { "Shizuku(root)への接続が必要です" },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        "自動検出先:",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        Yw2SaveStorage.DEFAULT_SAVE_DIRECTORY,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (discoveredGames.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        discoveredGames.forEach { file ->
                            OutlinedButton(
                                onClick = { openDirectSave(file) },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(file.fileName)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "自動検出できない場合は上のファイル選択を使用できます。真打・元祖/本家1.xは game*.yw のみ、元祖/本家2.xは head.yw が必要です。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }

            Spacer(Modifier.height(8.dp))
            PrimaryScrollableTabRow(selectedTabIndex = selectedTab) {
                Yw2Tab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            when (Yw2Tab.entries[selectedTab]) {
                Yw2Tab.YOKAI -> YokaiList(
                    data = current.decoded,
                    master = master,
                    onUpdate = { edited ->
                        runCatching { Yw2SaveCodec.updateYokai(current.decoded, edited) }
                            .onSuccess {
                                session = current.copy(decoded = it)
                                status = "妖怪 #" + edited.slot + " を変更しました（未保存）"
                            }
                            .onFailure { status = "変更失敗: " + it.message }
                    },
                )
                Yw2Tab.ITEM -> InventoryList(
                    data = current.decoded,
                    kind = Yw2InventoryKind.ITEM,
                    master = master,
                    onUpdate = { entry ->
                        runCatching { Yw2SaveCodec.updateInventory(current.decoded, entry) }
                            .onSuccess {
                                session = current.copy(decoded = it)
                                status = "どうぐ #" + entry.slot + " を変更しました（未保存）"
                            }
                            .onFailure { status = "変更失敗: " + it.message }
                    },
                )
                Yw2Tab.EQUIPMENT -> InventoryList(
                    data = current.decoded,
                    kind = Yw2InventoryKind.EQUIPMENT,
                    master = master,
                    onUpdate = { entry ->
                        runCatching { Yw2SaveCodec.updateInventory(current.decoded, entry) }
                            .onSuccess {
                                session = current.copy(decoded = it)
                                status = "そうび #" + entry.slot + " を変更しました（未保存）"
                            }
                            .onFailure { status = "変更失敗: " + it.message }
                    },
                )
                Yw2Tab.IMPORTANT -> InventoryList(
                    data = current.decoded,
                    kind = Yw2InventoryKind.IMPORTANT,
                    master = master,
                    onUpdate = { entry ->
                        runCatching { Yw2SaveCodec.updateInventory(current.decoded, entry) }
                            .onSuccess {
                                session = current.copy(decoded = it)
                                status = "だいじなもの #" + entry.slot + " を変更しました（未保存）"
                            }
                            .onFailure { status = "変更失敗: " + it.message }
                    },
                )
                Yw2Tab.SOUL -> InventoryList(
                    data = current.decoded,
                    kind = Yw2InventoryKind.SOUL,
                    master = master,
                    onUpdate = { entry ->
                        runCatching { Yw2SaveCodec.updateInventory(current.decoded, entry) }
                            .onSuccess {
                                session = current.copy(decoded = it)
                                status = "魂 #" + entry.slot + " を変更しました（未保存）"
                            }
                            .onFailure { status = "変更失敗: " + it.message }
                    },
                )
                Yw2Tab.INFO -> SaveInfoPanel(
                    data = current.decoded,
                    keyMode = current.keyMode,
                    headName = headName,
                    onUpdateMoney = { money ->
                        runCatching { Yw2SaveCodec.updateMoney(current.decoded, money) }
                            .onSuccess {
                                session = current.copy(decoded = it)
                                status = "所持金を変更しました（未保存）"
                            }
                            .onFailure { status = "変更失敗: " + it.message }
                    },
                )
            }
        }
    }
}

@Composable
private fun YokaiList(
    data: ByteArray,
    master: Yw2MasterData,
    onUpdate: (Yw2Yokai) -> Unit,
) {
    val parseResult = remember(data) { runCatching { Yw2SaveCodec.parseYokai(data) } }
    val entries = parseResult.getOrDefault(emptyList())
    var editing by remember { mutableStateOf<Yw2Yokai?>(null) }
    var query by remember { mutableStateOf("") }

    val filteredEntries = remember(entries, query) {
        val term = query.trim()
        if (term.isEmpty()) {
            entries
        } else {
            entries.filter { entry ->
                val speciesName = master.yokaiName(entry.typeId)
                speciesName.contains(term, ignoreCase = true) ||
                    entry.nickname.contains(term, ignoreCase = true) ||
                    entry.typeId.toString().contains(term) ||
                    entry.typeId.toString(16).contains(term, ignoreCase = true) ||
                    entry.slot.toString() == term.removePrefix("#")
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            if (query.isBlank()) {
                "妖怪: " + entries.size + "体"
            } else {
                "妖怪: " + entries.size + "体 / 検索結果 " + filteredEntries.size + "体"
            },
            fontWeight = FontWeight.Bold,
        )
        parseResult.exceptionOrNull()?.let { error ->
            Text(
                "妖怪領域解析エラー: " + (error.message ?: error::class.java.simpleName),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("妖怪を検索") },
            placeholder = { Text("名前 / ニックネーム / ID / #スロット") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(filteredEntries, key = { it.slot }) { entry ->
                val displayName = if (entry.nickname.isNotBlank()) {
                    entry.nickname + " (" + master.yokaiName(entry.typeId) + ")"
                } else {
                    master.yokaiName(entry.typeId)
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { editing = entry },
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("#" + entry.slot + "  " + displayName, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Lv." + entry.level +
                                "  技=" + entry.attackLevel + "/" + entry.techniqueLevel + "/" + entry.soultimateLevel +
                                "  性格=" + entry.attitude,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "IV " + entry.iv.values().joinToString("/") +
                                "  EV " + entry.ev.values().joinToString("/"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    editing?.let { selected ->
        YokaiEditDialog(
            value = selected,
            master = master,
            onDismiss = { editing = null },
            onSave = {
                onUpdate(it)
                editing = null
            },
        )
    }
}

@Composable
private fun YokaiEditDialog(
    value: Yw2Yokai,
    master: Yw2MasterData,
    onDismiss: () -> Unit,
    onSave: (Yw2Yokai) -> Unit,
) {
    var typeId by remember(value) { mutableStateOf(value.typeId) }
    var nickname by remember(value) { mutableStateOf(value.nickname) }
    var level by remember(value) { mutableStateOf(value.level.toString()) }
    var attack by remember(value) { mutableStateOf(value.attackLevel.toString()) }
    var technique by remember(value) { mutableStateOf(value.techniqueLevel.toString()) }
    var soultimate by remember(value) { mutableStateOf(value.soultimateLevel.toString()) }
    var attitude by remember(value) { mutableStateOf(value.attitude.toString()) }
    var loaf by remember(value) { mutableStateOf(value.loafLevel.toString()) }
    var iv by remember(value) { mutableStateOf(value.iv) }
    var ev by remember(value) { mutableStateOf(value.ev) }
    var picker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("妖怪 #" + value.slot) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("種類: " + master.yokaiName(typeId))
                OutlinedButton(onClick = { picker = true }) { Text("妖怪を選択") }
                OutlinedTextField(nickname, { nickname = it }, label = { Text("ニックネーム") })
                NumberField("レベル", level) { level = it }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField("こうげき", attack, Modifier.weight(1f)) { attack = it }
                    NumberField("ようじゅつ", technique, Modifier.weight(1f)) { technique = it }
                    NumberField("ひっさつ", soultimate, Modifier.weight(1f)) { soultimate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField("性格ID", attitude, Modifier.weight(1f)) { attitude = it }
                    NumberField("まじめ度", loaf, Modifier.weight(1f)) { loaf = it }
                }
                Text("個体値 IV", fontWeight = FontWeight.SemiBold)
                StatsEditor(iv) { iv = it }
                Text("育成値 EV", fontWeight = FontWeight.SemiBold)
                StatsEditor(ev) { ev = it }
                Text("IV条件: HP/2 + 他4能力 = 40 / EV条件: <= 20", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    value.copy(
                        typeId = typeId,
                        nickname = nickname,
                        level = level.toIntOrNull() ?: value.level,
                        attackLevel = attack.toIntOrNull() ?: value.attackLevel,
                        techniqueLevel = technique.toIntOrNull() ?: value.techniqueLevel,
                        soultimateLevel = soultimate.toIntOrNull() ?: value.soultimateLevel,
                        attitude = attitude.toIntOrNull() ?: value.attitude,
                        loafLevel = loaf.toIntOrNull() ?: value.loafLevel,
                        iv = iv,
                        ev = ev,
                    )
                )
            }) { Text("反映") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )

    if (picker) {
        CatalogDialog(
            title = "妖怪を選択",
            entries = master.yokai,
            currentId = typeId,
            onDismiss = { picker = false },
            onSelect = {
                typeId = it.id
                picker = false
            },
        )
    }
}

@Composable
private fun InventoryList(
    data: ByteArray,
    kind: Yw2InventoryKind,
    master: Yw2MasterData,
    onUpdate: (Yw2InventoryEntry) -> Unit,
) {
    val parseResult = remember(data, kind) {
        runCatching { Yw2SaveCodec.parseInventory(data, kind) }
    }
    val entries = parseResult.getOrDefault(emptyList())
    var editing by remember { mutableStateOf<Yw2InventoryEntry?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text(kind.label + ": " + entries.size + "件", fontWeight = FontWeight.Bold)
        parseResult.exceptionOrNull()?.let { error ->
            Text(
                kind.label + "領域解析エラー: " + (error.message ?: error::class.java.simpleName),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(4.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(entries, key = { it.slot }) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { editing = entry },
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("#" + entry.slot + "  " + master.inventoryName(kind, entry.typeId))
                            Text("ID " + entry.typeId, style = MaterialTheme.typography.bodySmall)
                        }
                        when (kind) {
                            Yw2InventoryKind.ITEM -> Text("×" + entry.amount)
                            Yw2InventoryKind.EQUIPMENT -> Text("×" + entry.amount + " / 使用 " + entry.used)
                            Yw2InventoryKind.IMPORTANT -> Unit
                            Yw2InventoryKind.SOUL -> Text("Lv." + entry.level)
                        }
                    }
                }
            }
        }
    }

    editing?.let { selected ->
        InventoryEditDialog(
            value = selected,
            master = master,
            onDismiss = { editing = null },
            onSave = {
                onUpdate(it)
                editing = null
            },
        )
    }
}

@Composable
private fun InventoryEditDialog(
    value: Yw2InventoryEntry,
    master: Yw2MasterData,
    onDismiss: () -> Unit,
    onSave: (Yw2InventoryEntry) -> Unit,
) {
    var typeId by remember(value) { mutableStateOf(value.typeId) }
    var amount by remember(value) { mutableStateOf(value.amount.toString()) }
    var used by remember(value) { mutableStateOf(value.used.toString()) }
    var experience by remember(value) { mutableStateOf(value.experience.toString()) }
    var level by remember(value) { mutableStateOf(value.level.toString()) }
    var picker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(value.kind.label + " #" + value.slot) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("種類: " + master.inventoryName(value.kind, typeId))
                OutlinedButton(onClick = { picker = true }) { Text("種類を選択") }
                when (value.kind) {
                    Yw2InventoryKind.ITEM -> NumberField("個数", amount) { amount = it }
                    Yw2InventoryKind.EQUIPMENT -> {
                        NumberField("個数", amount) { amount = it }
                        NumberField("使用中個数", used) { used = it }
                    }
                    Yw2InventoryKind.IMPORTANT -> Text("種類のみ編集します")
                    Yw2InventoryKind.SOUL -> {
                        NumberField("魂レベル (1-10)", level) { level = it }
                        NumberField("経験値", experience) { experience = it }
                        NumberField("使用フラグ", used) { used = it }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    value.copy(
                        typeId = typeId,
                        amount = amount.toIntOrNull() ?: value.amount,
                        used = used.toIntOrNull() ?: value.used,
                        experience = experience.toIntOrNull() ?: value.experience,
                        level = level.toIntOrNull() ?: value.level,
                    )
                )
            }) { Text("反映") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )

    if (picker) {
        CatalogDialog(
            title = value.kind.label + "を選択",
            entries = master.entries(value.kind),
            currentId = typeId,
            onDismiss = { picker = false },
            onSelect = {
                typeId = it.id
                picker = false
            },
        )
    }
}

@Composable
private fun SaveInfoPanel(
    data: ByteArray,
    keyMode: Yw2Crypto.KeyMode,
    headName: String?,
    onUpdateMoney: (Long) -> Unit,
) {
    val moneyResult = remember(data) { runCatching { Yw2SaveCodec.readMoney(data) } }
    val currentMoney = moneyResult.getOrNull()
    var money by remember(data) { mutableStateOf(currentMoney?.toString().orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("暗号方式", fontWeight = FontWeight.Bold)
        Text(keyMode.label)
        if (keyMode == Yw2Crypto.KeyMode.HEAD_DERIVED) {
            Text("head.yw: " + (headName ?: "未選択"))
        }
        HorizontalDivider()
        moneyResult.exceptionOrNull()?.let { error ->
            Text(
                "所持金領域解析エラー: " + (error.message ?: error::class.java.simpleName),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        NumberField("所持金", money) { money = it }
        Button(
            onClick = {
                val fallback = currentMoney ?: return@Button
                onUpdateMoney(money.toLongOrNull() ?: fallback)
            },
            enabled = currentMoney != null,
        ) {
            Text("所持金を反映")
        }
        HorizontalDivider()
        Text("保存時は暗号化後に再復号・構造確認してから選択した game*.yw へ上書きします。")
    }
}

@Composable
private fun StatsEditor(value: Yw2Stats, onChange: (Yw2Stats) -> Unit) {
    val labels = listOf("HP", "力", "妖", "守", "速")
    val values = value.values()
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { index, label ->
            var text by remember(value, index) { mutableStateOf(values[index].toString()) }
            NumberField(label, text, Modifier.weight(1f)) {
                text = it
                val number = it.toIntOrNull()
                if (number != null) {
                    onChange(
                        when (index) {
                            0 -> value.copy(hp = number)
                            1 -> value.copy(power = number)
                            2 -> value.copy(spirit = number)
                            3 -> value.copy(defense = number)
                            else -> value.copy(speed = number)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            if (text.isEmpty() || text.all { it.isDigit() }) onValueChange(text)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun CatalogDialog(
    title: String,
    entries: List<Yw2NamedId>,
    currentId: Long,
    onDismiss: () -> Unit,
    onSelect: (Yw2NamedId) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, entries) {
        if (query.isBlank()) entries
        else entries.filter {
            it.name.contains(query, ignoreCase = true) || it.id.toString().contains(query)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .padding(12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("名前 / IDで検索") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(filtered, key = { it.id }) { entry ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(entry) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                        ) {
                            Text(
                                (if (entry.id == currentId) "● " else "") +
                                    entry.name + "  (" + entry.id + ")"
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) { Text("閉じる") }
            }
        }
    }
}

private fun displayName(uri: Uri): String? = uri.lastPathSegment?.substringAfterLast('/')
