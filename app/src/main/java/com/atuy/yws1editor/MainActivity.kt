package com.atuy.yws1editor

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import com.atuy.yws1editor.ui.theme.YwEditorTheme
import com.atuy.yws1editor.shizuku.ShizukuFileServiceClient
import com.atuy.yws1editor.yokai.MainBinBackupInfo
import com.atuy.yws1editor.yokai.SaveInfoCodec
import com.atuy.yws1editor.yokai.ShizukuFileGateway
import com.atuy.yws1editor.yokai.Stat5
import com.atuy.yws1editor.yokai.StatGroup
import com.atuy.yws1editor.yokai.YokaiAttitude
import com.atuy.yws1editor.yokai.YokaiEntry
import com.atuy.yws1editor.yokai.YokaiMasterLoader
import com.atuy.yws1editor.yokai.YokaiStatusCalculator
import com.atuy.yws1editor.yokai.yokaiClassLabel
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.io.File
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val gateway = ShizukuFileGateway()
    private val mainBinPath: String by lazy {
        val currentUserDataRoot = requireNotNull(File(applicationInfo.dataDir).parentFile) {
            "現在ユーザーのデータディレクトリを解決できません"
        }
        File(currentUserDataRoot, "jp.co.level5.yws1/files/save/main.bin").path
    }

    private val requestCode = 1001
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

    private fun connectFileServiceIfRoot() {
        val uid = gateway.serverUid()
        if (uid != 0) {
            shizukuGranted = false
            shizukuStatusMessage = if (uid == 2000) {
                "ShizukuはADBモードです。このアプリの直接編集にはrootモードが必要です"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        lifecycleScope.launch {
            val masterData = withContext(Dispatchers.IO) {
                YokaiMasterLoader.load(this@MainActivity)
            }
            vm.setMasterData(masterData)
        }
        enableEdgeToEdge()
        setContent {
            YwEditorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppScreen(
                        modifier = Modifier.fillMaxSize(),
                        mainViewModel = vm,
                        shizukuGranted = shizukuGranted,
                        shizukuStatusMessage = shizukuStatusMessage,
                        mainBinPath = mainBinPath,
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
}

@Composable
private fun AppScreen(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel(),
    shizukuGranted: Boolean,
    shizukuStatusMessage: String,
    mainBinPath: String,
) {
    val state by mainViewModel.uiState.collectAsState()
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    val requestBackToStartup = {
        if (state.hasUnsavedChanges) {
            showDiscardChangesDialog = true
        } else {
            mainViewModel.backToStartup()
        }
    }

    BackHandler(enabled = state.currentScreen == AppScreen.Editor) {
        requestBackToStartup()
    }

    LaunchedEffect(state.currentScreen, shizukuGranted, state.startupSlotsLoaded) {
        if (
            state.currentScreen == AppScreen.Startup &&
            shizukuGranted &&
            !state.startupSlotsLoaded
        ) {
            mainViewModel.loadStartupSlots(mainBinPath)
        }
    }

    when (state.currentScreen) {
        AppScreen.Startup -> StartupScreen(
            slots = state.startupSaveSlots,
            selectedSection = state.selectedSection,
            shizukuGranted = shizukuGranted,
            shizukuStatusMessage = shizukuStatusMessage,
            loading = state.loading,
            message = state.message,
            onRefresh = {
                if (shizukuGranted && !state.loading) {
                    mainViewModel.loadStartupSlots(mainBinPath)
                }
            },
            onSelectSlot = { sectionName ->
                mainViewModel.openEditorForSection(mainBinPath, sectionName)
            },
            modifier = modifier,
        )

        AppScreen.Editor -> EditorScreen(
            state = state,
            shizukuGranted = shizukuGranted,
            isCheatMode = state.isCheatMode,
            onCheatModeChange = mainViewModel::setCheatMode,
            onBack = requestBackToStartup,
            onSave = { mainViewModel.save(mainBinPath) },
            onCreateBackup = { name, epochMillis ->
                mainViewModel.createBackup(mainBinPath, name, epochMillis)
            },
            onRefreshBackups = { mainViewModel.refreshBackups(mainBinPath) },
            onRestoreBackup = { backupFileName ->
                mainViewModel.restoreBackup(mainBinPath, backupFileName)
            },
            onTabSelect = mainViewModel::selectTopTab,
            onYokaiCardClick = { slot ->
                mainViewModel.select(slot)
                mainViewModel.toggleYokaiExpanded(slot)
            },
            onYokaiChange = { slot, value -> mainViewModel.updateYokai(slot, value) },
            onLevelChange = { slot, value -> mainViewModel.updateLevel(slot, value) },
            onAttitudeChange = { slot, value -> mainViewModel.updateAttitude(slot, value) },
            onAttackLevelChange = { slot, value -> mainViewModel.updateAttackLevel(slot, value) },
            onTechniqueLevelChange = { slot, value -> mainViewModel.updateTechniqueLevel(slot, value) },
            onSoultimateLevelChange = { slot, value -> mainViewModel.updateSoultimateLevel(slot, value) },
            onMajimeCorrectionChange = { slot, value -> mainViewModel.updateMajimeCorrection(slot, value) },
            onStateFlagChange = { slot, mask, enabled -> mainViewModel.setStateFlag(slot, mask, enabled) },
            onStatChange = { slot, group, index, value ->
                mainViewModel.updateStat(slot, group, index, value)
            },
            onPlayHoursChange = mainViewModel::updatePlayHours,
            onPlayMinutesChange = mainViewModel::updatePlayMinutes,
            onMoneyChange = mainViewModel::updateMoney,
            onPlayerNameChange = mainViewModel::updatePlayerName,
            onSaveYearChange = mainViewModel::updateSaveYear,
            onSaveMonthChange = mainViewModel::updateSaveMonth,
            onSaveDayChange = mainViewModel::updateSaveDay,
            onSaveHourChange = mainViewModel::updateSaveHour,
            onSaveMinuteChange = mainViewModel::updateSaveMinute,
            yokaiOptions = state.yokaiOptions,
            modifier = modifier,
        )
    }

    if (showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardChangesDialog = false },
            title = { Text("編集内容を破棄しますか？") },
            text = { Text("保存していない変更があります。最初の画面に戻ると変更内容は失われます。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardChangesDialog = false
                        mainViewModel.backToStartup()
                    },
                ) {
                    Text("破棄して戻る")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardChangesDialog = false }) {
                    Text("キャンセル")
                }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StartupScreen(
    slots: List<SaveSlotCard>,
    selectedSection: String,
    shizukuGranted: Boolean,
    shizukuStatusMessage: String,
    loading: Boolean,
    message: String,
    onRefresh: () -> Unit,
    onSelectSlot: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "セーブデータ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "編集するスロットを選択してください",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                color = if (shizukuGranted) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                contentColor = if (shizukuGranted) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = if (shizukuGranted) "Shizuku 接続済み" else "Shizukuを確認してください",
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!shizukuGranted) {
                        Text(shizukuStatusMessage, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("下に引っ張るとセーブ情報を再読み込みできます", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (message.isNotBlank() && !loading) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            slots.forEach { slot ->
                val isSelected = slot.sectionName == selectedSection
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = shizukuGranted && !loading && slot.hasData) {
                            onSelectSlot(slot.sectionName)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = slot.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = slot.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!slot.hasData) {
                            Text(
                                text = "データがありません",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = slot.displayName ?: "名前未設定",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "${slot.playTimeText ?: "未解析"}  •  妖怪 ${slot.yokaiCount?.toString() ?: "-"}体",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "セーブ日時  ${slot.saveDateText ?: "未解析"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isSelected) {
                            Text(
                                text = "選択中",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(24.dp)
                            .height(24.dp),
                        strokeWidth = 2.5.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("セーブ情報を読み込み中…")
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditorScreen(
    state: EditorUiState,
    shizukuGranted: Boolean,
    isCheatMode: Boolean,
    yokaiOptions: List<YokaiOption>,
    onCheatModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onCreateBackup: (String, Long) -> Unit,
    onRefreshBackups: () -> Unit,
    onRestoreBackup: (String) -> Unit,
    onTabSelect: (EditorTopTab) -> Unit,
    onYokaiCardClick: (Int) -> Unit,
    onYokaiChange: (Int, Long) -> Unit,
    onLevelChange: (Int, Int) -> Unit,
    onAttitudeChange: (Int, Int) -> Unit,
    onAttackLevelChange: (Int, Int) -> Unit,
    onTechniqueLevelChange: (Int, Int) -> Unit,
    onSoultimateLevelChange: (Int, Int) -> Unit,
    onMajimeCorrectionChange: (Int, Int) -> Unit,
    onStateFlagChange: (Int, Int, Boolean) -> Unit,
    onStatChange: (Int, StatGroup, Int, Int) -> Unit,
    onPlayHoursChange: (Int) -> Unit,
    onPlayMinutesChange: (Int) -> Unit,
    onMoneyChange: (Int) -> Unit,
    onPlayerNameChange: (String) -> Unit,
    onSaveYearChange: (Int) -> Unit,
    onSaveMonthChange: (Int) -> Unit,
    onSaveDayChange: (Int) -> Unit,
    onSaveHourChange: (Int) -> Unit,
    onSaveMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showCreateBackupDialog = remember { mutableStateOf(false) }
    val showBackupListDialog = remember { mutableStateOf(false) }
    val restoreTarget = remember { mutableStateOf<MainBinBackupInfo?>(null) }
    var topChromeOffsetPx by remember { mutableStateOf(0f) }
    var topChromeHeightPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val fileOperationBusy = state.loading ||
        state.saving ||
        state.backupsLoading ||
        state.backupsCreating ||
        state.backupsRestoring

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (topChromeHeightPx <= 0f) return Offset.Zero

                val previousOffset = topChromeOffsetPx
                val nextOffset = (previousOffset + available.y).coerceIn(-topChromeHeightPx, 0f)
                topChromeOffsetPx = nextOffset

                // Consume only the distance used by header motion so content doesn't scroll in parallel.
                val consumedY = nextOffset - previousOffset
                return Offset(x = 0f, y = consumedY)
            }
        }
    }

    LaunchedEffect(state.selectedTopTab) {
        topChromeOffsetPx = 0f
    }

    val topChromeVisibleHeightDp = with(density) {
        (topChromeHeightPx + topChromeOffsetPx).coerceAtLeast(0f).toDp()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(topChromeVisibleHeightDp))

            if (state.message.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (fileOperationBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(text = state.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            when (state.selectedTopTab) {
                EditorTopTab.Yokai -> YokaiTabContent(
                    entries = state.entries,
                    expandedSlot = state.expandedYokaiSlot,
                    yokaiOptions = yokaiOptions,
                    attitudes = state.attitudes,
                    isCheatMode = isCheatMode,
                    onCheatModeChange = onCheatModeChange,
                    levelInputMax = if (isCheatMode) 255 else 99,
                    ivaInputMax = if (isCheatMode) 255 else 8,
                    cbInputMax = if (isCheatMode) 255 else 20,
                    onCardClick = onYokaiCardClick,
                    onYokaiChange = onYokaiChange,
                    onLevelChange = onLevelChange,
                    onAttitudeChange = onAttitudeChange,
                    onAttackLevelChange = onAttackLevelChange,
                    onTechniqueLevelChange = onTechniqueLevelChange,
                    onSoultimateLevelChange = onSoultimateLevelChange,
                    onMajimeCorrectionChange = onMajimeCorrectionChange,
                    onStateFlagChange = onStateFlagChange,
                    onStatChange = onStatChange,
                    modifier = Modifier.fillMaxSize(),
                )

                EditorTopTab.Info -> SaveInfoEditorSection(
                    playHours = state.playHours,
                    playMinutes = state.playMinutes,
                    money = state.money,
                    playerName = state.playerName,
                    playerNameError = state.playerNameError,
                    saveYear = state.saveYear,
                    saveMonth = state.saveMonth,
                    saveDay = state.saveDay,
                    saveHour = state.saveHour,
                    saveMinute = state.saveMinute,
                    onPlayHoursChange = onPlayHoursChange,
                    onPlayMinutesChange = onPlayMinutesChange,
                    onMoneyChange = onMoneyChange,
                    onPlayerNameChange = onPlayerNameChange,
                    onSaveYearChange = onSaveYearChange,
                    onSaveMonthChange = onSaveMonthChange,
                    onSaveDayChange = onSaveDayChange,
                    onSaveHourChange = onSaveHourChange,
                    onSaveMinuteChange = onSaveMinuteChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                )

                else -> PlaceholderTabContent(
                    tab = state.selectedTopTab,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .onSizeChanged { size ->
                    topChromeHeightPx = size.height.toFloat()
                    topChromeOffsetPx = topChromeOffsetPx.coerceIn(-topChromeHeightPx, 0f)
                }
                .offset { androidx.compose.ui.unit.IntOffset(0, topChromeOffsetPx.roundToInt()) },
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = sectionDisplayName(state.selectedSection),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (state.hasUnsavedChanges) "未保存の変更があります" else "保存済み",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.hasUnsavedChanges) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showCreateBackupDialog.value = true },
                        enabled = shizukuGranted && state.loaded && !fileOperationBusy,
                    ) {
                        Text("バックアップ")
                    }
                    TextButton(
                        onClick = {
                            onRefreshBackups()
                            showBackupListDialog.value = true
                        },
                        enabled = shizukuGranted && state.loaded && !fileOperationBusy,
                    ) {
                        Text("リストア")
                    }
                    TextButton(
                        onClick = onSave,
                        enabled = shizukuGranted && state.loaded && !fileOperationBusy,
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = "保存")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保存")
                    }
                },
            )

            PrimaryScrollableTabRow(
                selectedTabIndex = state.selectedTopTab.ordinal,
                edgePadding = 8.dp,
            ) {
                EditorTopTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTopTab == tab,
                        onClick = { onTabSelect(tab) },
                        text = { Text(tab.label) },
                    )
                }
            }
        }
    }

    if (showCreateBackupDialog.value) {
        CreateBackupDialog(
            creating = state.backupsCreating,
            onDismiss = { showCreateBackupDialog.value = false },
            onConfirm = { name, epochMillis ->
                onCreateBackup(name, epochMillis)
                showCreateBackupDialog.value = false
            },
        )
    }

    if (showBackupListDialog.value) {
        BackupListDialog(
            backupItems = state.backupItems,
            loading = state.backupsLoading,
            restoring = state.backupsRestoring,
            onDismiss = { showBackupListDialog.value = false },
            onRefresh = onRefreshBackups,
            onRequestRestore = { restoreTarget.value = it },
        )
    }

    val target = restoreTarget.value
    if (target != null) {
        AlertDialog(
            onDismissRequest = { restoreTarget.value = null },
            title = { Text("バックアップを復元") },
            text = { Text("${target.displayName} (${formatDateTime(target.backupEpochMillis)}) を復元します。現在の main.bin は上書きされます。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRestoreBackup(target.fileName)
                        restoreTarget.value = null
                        showBackupListDialog.value = false
                    },
                    enabled = !state.backupsRestoring,
                ) {
                    Text("復元")
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget.value = null }) {
                    Text("キャンセル")
                }
            },
        )
    }
}

@Composable
private fun SaveInfoEditorSection(
    playHours: Int,
    playMinutes: Int,
    money: Int,
    playerName: String,
    playerNameError: String?,
    saveYear: Int,
    saveMonth: Int,
    saveDay: Int,
    saveHour: Int,
    saveMinute: Int,
    onPlayHoursChange: (Int) -> Unit,
    onPlayMinutesChange: (Int) -> Unit,
    onMoneyChange: (Int) -> Unit,
    onPlayerNameChange: (String) -> Unit,
    onSaveYearChange: (Int) -> Unit,
    onSaveMonthChange: (Int) -> Unit,
    onSaveDayChange: (Int) -> Unit,
    onSaveHourChange: (Int) -> Unit,
    onSaveMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("セーブ情報", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("プレイ時間")
                    CompactNumberField(
                        value = playHours,
                        max = 999_999,
                        modifier = Modifier.width(100.dp),
                        onValueChange = onPlayHoursChange,
                    )
                    Text("時間")
                    CompactNumberField(
                        value = playMinutes,
                        max = 59,
                        modifier = Modifier.width(84.dp),
                        onValueChange = onPlayMinutesChange,
                    )
                    Text("分")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("所持金")
                    CompactNumberField(
                        value = money,
                        max = SaveInfoCodec.MONEY_MAX,
                        modifier = Modifier.width(140.dp),
                        onValueChange = onMoneyChange,
                    )
                    Text("円")
                }

                val nameBytes = playerName.toByteArray(Charsets.UTF_8).size
                OutlinedTextField(
                    value = playerName,
                    onValueChange = onPlayerNameChange,
                    singleLine = true,
                    label = { Text("プレイヤー名") },
                    isError = playerNameError != null,
                    supportingText = {
                        Text(playerNameError ?: "UTF-8 ${nameBytes}/23 バイト")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("セーブ日時")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompactNumberField(
                        value = saveYear,
                        max = 9999,
                        modifier = Modifier.width(100.dp),
                        onValueChange = onSaveYearChange,
                    )
                    Text("年")
                    CompactNumberField(
                        value = saveMonth,
                        max = 12,
                        modifier = Modifier.width(72.dp),
                        onValueChange = onSaveMonthChange,
                    )
                    Text("月")
                    CompactNumberField(
                        value = saveDay,
                        max = 31,
                        modifier = Modifier.width(72.dp),
                        onValueChange = onSaveDayChange,
                    )
                    Text("日")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompactNumberField(
                        value = saveHour,
                        max = 23,
                        modifier = Modifier.width(84.dp),
                        onValueChange = onSaveHourChange,
                    )
                    Text("時")
                    CompactNumberField(
                        value = saveMinute,
                        max = 59,
                        modifier = Modifier.width(84.dp),
                        onValueChange = onSaveMinuteChange,
                    )
                    Text("分")
                }
            }
        }
    }
}

@Composable
private fun CreateBackupDialog(
    creating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(formatDateForInput(System.currentTimeMillis())) }
    val dateError = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!creating) onDismiss()
        },
        title = { Text("バックアップ作成") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        dateError.value = false
                    },
                    singleLine = true,
                    label = { Text("日時(yyyyMMddHHmm)") },
                    isError = dateError.value,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (dateError.value) {
                    Text("日時は yyyyMMddHHmm 形式で入力してください")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = parseDateFromInput(dateText)
                    if (parsed == null) {
                        dateError.value = true
                        return@TextButton
                    }
                    onConfirm(name, parsed)
                },
                enabled = !creating,
            ) {
                Text("作成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) {
                Text("キャンセル")
            }
        },
    )
}

@Composable
private fun BackupListDialog(
    backupItems: List<MainBinBackupInfo>,
    loading: Boolean,
    restoring: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onRequestRestore: (MainBinBackupInfo) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("バックアップ一覧") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (backupItems.isEmpty()) {
                    Text(if (loading) "読み込み中..." else "バックアップはありません")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(backupItems, key = { it.fileName }) { backup ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !restoring) { onRequestRestore(backup) },
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                ) {
                                    Text(backup.displayName, fontWeight = FontWeight.Medium)
                                    Text(formatDateTime(backup.backupEpochMillis))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh, enabled = !loading && !restoring) {
                Text("再読込")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        },
    )
}

private fun formatDateTime(epochMillis: Long?): String {
    if (epochMillis == null) return "未取得"
    val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
    return formatter.format(Date(epochMillis))
}

private fun sectionDisplayName(sectionName: String): String {
    return when (sectionName) {
        "game0.yw" -> "オートセーブ"
        "game1.yw" -> "にっき1"
        "game2.yw" -> "にっき2"
        "game3.yw" -> "にっき3"
        else -> sectionName.removeSuffix(".yw")
    }
}

private fun formatDateForInput(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyyMMddHHmm", Locale.JAPAN)
    return formatter.format(Date(epochMillis))
}

private fun parseDateFromInput(value: String): Long? {
    if (!Regex("^\\d{12}$").matches(value)) return null
    val formatter = SimpleDateFormat("yyyyMMddHHmm", Locale.JAPAN)
    formatter.isLenient = false
    return runCatching { formatter.parse(value)?.time }.getOrNull()
}

@Composable
private fun PlaceholderTabContent(
    tab: EditorTopTab,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text("${tab.label}は未実装です")
    }
}

@Composable
private fun YokaiTabContent(
    entries: List<YokaiEntry>,
    expandedSlot: Int?,
    yokaiOptions: List<YokaiOption>,
    attitudes: List<YokaiAttitude>,
    isCheatMode: Boolean,
    onCheatModeChange: (Boolean) -> Unit,
    levelInputMax: Int,
    ivaInputMax: Int,
    cbInputMax: Int,
    onCardClick: (Int) -> Unit,
    onYokaiChange: (Int, Long) -> Unit,
    onLevelChange: (Int, Int) -> Unit,
    onAttitudeChange: (Int, Int) -> Unit,
    onAttackLevelChange: (Int, Int) -> Unit,
    onTechniqueLevelChange: (Int, Int) -> Unit,
    onSoultimateLevelChange: (Int, Int) -> Unit,
    onMajimeCorrectionChange: (Int, Int) -> Unit,
    onStateFlagChange: (Int, Int, Boolean) -> Unit,
    onStatChange: (Int, StatGroup, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchText by remember { mutableStateOf("") }
    val filtered = entries.filter {
        searchText.isBlank() ||
            it.name.contains(searchText, ignoreCase = true) ||
            it.slot.toString().contains(searchText)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Checkbox(
                checked = isCheatMode,
                onCheckedChange = onCheatModeChange,
            )
            Text("チートモード (LV/IVA/CBは最大255、IVB1/IVB2は15固定)")
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            singleLine = true,
            label = { Text("妖怪検索") },
            modifier = Modifier.fillMaxWidth(),
        )

        if (entries.isEmpty()) {
            Text("セーブが未読込です。起動画面からセーブを選択してください。")
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.slot }) { entry ->
                val expanded = expandedSlot == entry.slot
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCardClick(entry.slot) },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(entry.name, fontWeight = FontWeight.Bold)
                                Text("${yokaiClassLabel(entry.yokaiClass)} / Slot ${entry.slot}")
                            }
                            Text("Lv.${entry.level}")
                        }

                        if (expanded) {
                            HorizontalDivider()
                            YokaiStatusEditorPanel(
                                entry = entry,
                                yokaiOptions = yokaiOptions,
                                attitudes = attitudes,
                                isCheatMode = isCheatMode,
                                levelInputMax = levelInputMax,
                                ivaInputMax = ivaInputMax,
                                cbInputMax = cbInputMax,
                                onYokaiChange = { onYokaiChange(entry.slot, it) },
                                onLevelChange = { onLevelChange(entry.slot, it) },
                                onAttitudeChange = { onAttitudeChange(entry.slot, it) },
                                onAttackLevelChange = { onAttackLevelChange(entry.slot, it) },
                                onTechniqueLevelChange = { onTechniqueLevelChange(entry.slot, it) },
                                onSoultimateLevelChange = { onSoultimateLevelChange(entry.slot, it) },
                                onMajimeCorrectionChange = { onMajimeCorrectionChange(entry.slot, it) },
                                onStateFlagChange = { mask, enabled -> onStateFlagChange(entry.slot, mask, enabled) },
                                onStatChange = { group, index, value ->
                                    onStatChange(entry.slot, group, index, value)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val ZERO_STAT = listOf(0, 0, 0, 0, 0)
private val STATUS_LABEL_WIDTH = 44.dp
private val STATUS_CELL_WIDTH = 64.dp
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

@Composable
private fun YokaiStatusEditorPanel(
    entry: YokaiEntry,
    yokaiOptions: List<YokaiOption>,
    attitudes: List<YokaiAttitude>,
    isCheatMode: Boolean,
    levelInputMax: Int,
    ivaInputMax: Int,
    cbInputMax: Int,
    onYokaiChange: (Long) -> Unit,
    onLevelChange: (Int) -> Unit,
    onAttitudeChange: (Int) -> Unit,
    onAttackLevelChange: (Int) -> Unit,
    onTechniqueLevelChange: (Int) -> Unit,
    onSoultimateLevelChange: (Int) -> Unit,
    onMajimeCorrectionChange: (Int) -> Unit,
    onStateFlagChange: (Int, Boolean) -> Unit,
    onStatChange: (StatGroup, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val finalStatus = YokaiStatusCalculator.calculate(entry)
    val ivaEditableMask = if (isCheatMode) {
        null
    } else {
        IVA_EDITABLE_BY_CLASS[entry.yokaiClass] ?: listOf(true, true, true, true, true)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        StatusHeadRow(
            name = entry.name,
            yokaiId = entry.id,
            yokaiOptions = yokaiOptions,
            classLabel = yokaiClassLabel(entry.yokaiClass),
            level = entry.level,
            levelInputMax = levelInputMax,
            onYokaiChange = onYokaiChange,
            onLevelChange = onLevelChange,
        )
        AttitudeMajimeRow(
            attitudes = attitudes,
            selectedAttitude = entry.attitudeId,
            correction = entry.majimeCorrection,
            onAttitudeChange = onAttitudeChange,
            onCorrectionChange = onMajimeCorrectionChange,
        )
        StatusHeaderRow()
        StatusReadOnlyRow(label = "BS", values = entry.baseStats?.values() ?: ZERO_STAT)
        StatusEditableRow(
            label = "IVA",
            stat = entry.iva,
            max = ivaInputMax,
            editableMask = ivaEditableMask,
            onValueChange = { i, v -> onStatChange(StatGroup.IVA, i, v) },
        )
        StatusEditableRow(label = "IVB1", stat = entry.ivb1, max = 15, onValueChange = { i, v -> onStatChange(StatGroup.IVB1, i, v) })
        StatusEditableRow(label = "IVB2", stat = entry.ivb2, max = 15, onValueChange = { i, v -> onStatChange(StatGroup.IVB2, i, v) })
        StatusEditableRow(label = "CB", stat = entry.cb, max = cbInputMax, onValueChange = { i, v -> onStatChange(StatGroup.CB, i, v) })
        StatusReadOnlyRow(label = "最終", values = finalStatus?.values() ?: ZERO_STAT)
        TechniqueRow(
            attackLevel = entry.attackLevel,
            techniqueLevel = entry.techniqueLevel,
            soultimateLevel = entry.soultimateLevel,
            onAttackLevelChange = onAttackLevelChange,
            onTechniqueLevelChange = onTechniqueLevelChange,
            onSoultimateLevelChange = onSoultimateLevelChange,
        )
        StateFlagRow(
            stateFlags = entry.stateFlags,
            onFlagChange = onStateFlagChange,
        )
    }
}

@Composable
private fun StatusHeadRow(
    name: String,
    yokaiId: Long,
    yokaiOptions: List<YokaiOption>,
    classLabel: String,
    level: Int,
    levelInputMax: Int,
    onYokaiChange: (Long) -> Unit,
    onLevelChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        YokaiDropdown(
            yokaiOptions = yokaiOptions,
            selectedId = yokaiId,
            selectedName = name,
            onSelected = onYokaiChange,
            modifier = Modifier.weight(1f),
        )
        Text(classLabel)
        CompactNumberField(
            value = level,
            max = levelInputMax,
            modifier = Modifier.width(78.dp),
            onValueChange = onLevelChange,
        )
    }
}

@Composable
private fun AttitudeMajimeRow(
    attitudes: List<YokaiAttitude>,
    selectedAttitude: Int,
    correction: Int,
    onAttitudeChange: (Int) -> Unit,
    onCorrectionChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("性格", modifier = Modifier.width(STATUS_LABEL_WIDTH), fontWeight = FontWeight.Medium)
        AttitudeDropdown(
            attitudes = attitudes,
            selectedId = selectedAttitude,
            onSelected = onAttitudeChange,
            modifier = Modifier.width(132.dp),
        )
        Text("まじめ度")
        CompactNumberField(
            value = correction,
            max = 255,
            modifier = Modifier.width(84.dp),
            onValueChange = onCorrectionChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YokaiDropdown(
    yokaiOptions: List<YokaiOption>,
    selectedId: Long,
    selectedName: String,
    onSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = yokaiOptions.firstOrNull { it.id == selectedId }?.name ?: selectedName

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            yokaiOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        onSelected(option.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttitudeDropdown(
    attitudes: List<YokaiAttitude>,
    selectedId: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = attitudes.firstOrNull { it.id == selectedId }?.name ?: "性格ID:$selectedId"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            attitudes.forEach { attitude ->
                DropdownMenuItem(
                    text = { Text(attitude.name) },
                    onClick = {
                        onSelected(attitude.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun StatusHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("", modifier = Modifier.width(STATUS_LABEL_WIDTH))
        listOf("HP", "力", "妖", "守", "速").forEach { title ->
            Box(modifier = Modifier.width(STATUS_CELL_WIDTH), contentAlignment = Alignment.Center) {
                Text(title, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StatusReadOnlyRow(label: String, values: List<Int>) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(STATUS_LABEL_WIDTH), fontWeight = FontWeight.Medium)
        values.forEach { value ->
            Box(modifier = Modifier.width(STATUS_CELL_WIDTH), contentAlignment = Alignment.Center) {
                Text(value.toString())
            }
        }
    }
}

@Composable
private fun StatusEditableRow(
    label: String,
    stat: Stat5,
    max: Int,
    editableMask: List<Boolean>? = null,
    onValueChange: (Int, Int) -> Unit,
) {
    val values = stat.values()
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(STATUS_LABEL_WIDTH), fontWeight = FontWeight.Medium)
        values.forEachIndexed { index, value ->
            val enabled = editableMask?.getOrElse(index) { true } ?: true
            if (enabled) {
                CompactNumberField(
                    value = value,
                    max = max,
                    modifier = Modifier
                        .width(STATUS_CELL_WIDTH)
                        .padding(horizontal = 1.dp),
                    onValueChange = { onValueChange(index, it) },
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(STATUS_CELL_WIDTH)
                        .padding(horizontal = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(value.toString())
                }
            }
        }
    }
}

@Composable
private fun TechniqueRow(
    attackLevel: Int,
    techniqueLevel: Int,
    soultimateLevel: Int,
    onAttackLevelChange: (Int) -> Unit,
    onTechniqueLevelChange: (Int) -> Unit,
    onSoultimateLevelChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("技Lv", modifier = Modifier.width(STATUS_LABEL_WIDTH), fontWeight = FontWeight.Medium)
        Text("攻")
        CompactNumberField(
            value = attackLevel,
            max = 99,
            modifier = Modifier.width(72.dp),
            onValueChange = onAttackLevelChange,
        )
        Text("妖")
        CompactNumberField(
            value = techniqueLevel,
            max = 99,
            modifier = Modifier.width(72.dp),
            onValueChange = onTechniqueLevelChange,
        )
        Text("必")
        CompactNumberField(
            value = soultimateLevel,
            max = 99,
            modifier = Modifier.width(72.dp),
            onValueChange = onSoultimateLevelChange,
        )
    }
}


@Composable
private fun StateFlagRow(
    stateFlags: Int,
    onFlagChange: (Int, Boolean) -> Unit,
) {
    val lockMask = 0x03
    val bookMask = 0x04
    val newMask = 0x08

    val lockEnabled = stateFlags and lockMask == lockMask
    val bookEnabled = stateFlags and bookMask != 0
    val newEnabled = stateFlags and newMask != 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = lockEnabled, onCheckedChange = { onFlagChange(lockMask, it) })
                Text("おわかれ不可")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = bookEnabled, onCheckedChange = { onFlagChange(bookMask, it) })
                Text("本使用済み")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = newEnabled, onCheckedChange = { onFlagChange(newMask, it) })
                Text("NEW!")
            }
        }
    }
}


@Composable
private fun CompactNumberField(
    value: Int,
    max: Int,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            if (input.isBlank()) return@OutlinedTextField

            val parsed = input.toIntOrNull() ?: return@OutlinedTextField
            val clamped = when {
                parsed < 0 -> 0
                parsed > max -> max
                else -> parsed
            }
            onValueChange(clamped)
            if (clamped.toString() != input) {
                text = clamped.toString()
            }
        },
        modifier = modifier.onFocusChanged { state ->
            if (!state.isFocused) {
                val parsed = text.toIntOrNull()
                val fixed = when {
                    parsed == null -> 0
                    parsed < 0 -> 0
                    parsed > max -> max
                    else -> parsed
                }
                onValueChange(fixed)
                text = fixed.toString()
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    YwEditorTheme {
        Column(Modifier.padding(16.dp)) {
            Text("プレビュー")
            Spacer(Modifier.height(8.dp))
            Text("実機で Shizuku 接続後に動作します")
        }
    }
}
