package com.atuy.yws1editor

import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.atuy.yws1editor.ui.theme.YwEditorTheme
import com.atuy.yws1editor.yw2.Yw2Crypto
import com.atuy.yws1editor.yw2.Yw2InventoryEntry
import com.atuy.yws1editor.yw2.Yw2InventoryKind
import com.atuy.yws1editor.yw2.Yw2MasterData
import com.atuy.yws1editor.yw2.Yw2NamedId
import com.atuy.yws1editor.yw2.Yw2SaveCodec
import com.atuy.yws1editor.yw2.Yw2Stats
import com.atuy.yws1editor.yw2.Yw2Yokai
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Yw2MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YwEditorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Yw2EditorScreen()
                }
            }
        }
    }
}

private data class Yw2Session(
    val uri: Uri,
    val fileName: String,
    val originalEncrypted: ByteArray,
    val decoded: ByteArray,
    val keyMode: Yw2Crypto.KeyMode,
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
private fun Yw2EditorScreen() {
    val context = LocalContext.current
    val master = remember { Yw2MasterData(context) }

    var session by remember { mutableStateOf<Yw2Session?>(null) }
    var headBytes by remember { mutableStateOf<ByteArray?>(null) }
    var headName by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("game*.yw を開いてください") }
    var busy by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

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
                // Fail early if this is an unrelated file.
                Yw2SaveCodec.parseYokai(decoded.data)
                Yw2SaveCodec.parseInventory(decoded.data, Yw2InventoryKind.ITEM)
                Yw2Session(
                    uri = uri,
                    fileName = displayName(uri) ?: uri.lastPathSegment ?: "game.yw",
                    originalEncrypted = encrypted,
                    decoded = decoded.data,
                    keyMode = decoded.keyMode,
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

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("妖怪ウォッチ2 セーブエディタ", style = MaterialTheme.typography.headlineSmall)
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
                            busy = true
                            val current = session ?: return@Button
                            runCatching {
                                val backupDir = File(context.filesDir, "yw2-backups").apply { mkdirs() }
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                val safeName = current.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                                File(backupDir, safeName + "." + stamp + ".bak")
                                    .writeBytes(current.originalEncrypted)

                                val encrypted = Yw2Crypto.encrypt(current.decoded, current.keyMode, headBytes)
                                val verify = Yw2Crypto.decrypt(encrypted, headBytes)
                                Yw2SaveCodec.parseYokai(verify.data)
                                context.contentResolver.openOutputStream(current.uri, "wt")?.use {
                                    it.write(encrypted)
                                    it.flush()
                                } ?: error("選択ファイルへ書き込めません")
                                encrypted
                            }.onSuccess { encrypted ->
                                session = current.copy(originalEncrypted = encrypted)
                                status = "保存完了。バックアップはアプリ内部の yw2-backups に作成しました"
                            }.onFailure {
                                status = "保存失敗: " + (it.message ?: it::class.java.simpleName)
                            }
                            busy = false
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
                Text(
                    "真打・元祖/本家1.xは game*.yw のみで読み込めます。元祖/本家2.xは先に head.yw を選択してください。",
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
    val entries = remember(data) { runCatching { Yw2SaveCodec.parseYokai(data) }.getOrDefault(emptyList()) }
    var editing by remember { mutableStateOf<Yw2Yokai?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text("妖怪: " + entries.size + "体", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(entries, key = { it.slot }) { entry ->
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
    val entries = remember(data, kind) {
        runCatching { Yw2SaveCodec.parseInventory(data, kind) }.getOrDefault(emptyList())
    }
    var editing by remember { mutableStateOf<Yw2InventoryEntry?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text(kind.label + ": " + entries.size + "件", fontWeight = FontWeight.Bold)
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
    val currentMoney = remember(data) { runCatching { Yw2SaveCodec.readMoney(data) }.getOrDefault(0L) }
    var money by remember(currentMoney) { mutableStateOf(currentMoney.toString()) }

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
        NumberField("所持金", money) { money = it }
        Button(onClick = { onUpdateMoney(money.toLongOrNull() ?: currentMoney) }) {
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
