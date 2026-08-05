package com.atuy.yws1editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun DailyBattleScreen(
    state: DailyBattleUiState,
    shizukuGranted: Boolean,
    shizukuStatusMessage: String,
    onClose: () -> Unit,
    onRetryShizuku: () -> Unit,
    onReload: () -> Unit,
    onSelectSection: (String) -> Unit,
    onMirrorGame0Change: (Boolean) -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onSetAllAvailable: () -> Unit,
    onSetAllFought: () -> Unit,
    onSave: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val busy = state.loading || state.saving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onClose) { Text("閉じる") }
            Text("一日一回バトル", fontWeight = FontWeight.Bold)
            TextButton(onClick = onSave, enabled = shizukuGranted && state.loaded && !busy && state.dirty) {
                Text("保存")
            }
        }
        HorizontalDivider()

        if (!shizukuGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(shizukuStatusMessage)
                    Button(onClick = onRetryShizuku) { Text("再接続") }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = onReload, enabled = !busy) { Text("再読み込み") }
                if (busy) CircularProgressIndicator(modifier = Modifier.height(24.dp))
                if (state.message.isNotBlank()) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (state.loaded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.sections.forEach { section ->
                    if (section == state.selectedSection) {
                        Button(onClick = {}, enabled = false) { Text(sectionLabel(section)) }
                    } else {
                        OutlinedButton(
                            onClick = { onSelectSection(section) },
                            enabled = !busy && !state.dirty,
                        ) {
                            Text(sectionLabel(section))
                        }
                    }
                }
            }

            if (state.selectedSection != "game0.yw" && "game0.yw" in state.sections) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !busy) { onMirrorGame0Change(!state.mirrorGame0) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.mirrorGame0,
                        onCheckedChange = if (busy) null else onMirrorGame0Change,
                    )
                    Text("保存時にオートセーブ（game0.yw）にも同じ日次状態を反映")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onSetAllAvailable, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text("全員再戦可能")
                }
                OutlinedButton(onClick = onSetAllFought, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text("全員戦闘済み")
                }
            }

            Text(
                text = "チェックあり＝本日は戦闘済み。友達済み・イベント進行フラグには触れません。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp),
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.entries, key = { it.definition.flagIndex }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !busy) {
                                onToggle(entry.definition.flagIndex, !entry.foughtToday)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = entry.foughtToday,
                            onCheckedChange = if (busy) null else { checked ->
                                onToggle(entry.definition.flagIndex, checked)
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.definition.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "flag ${entry.definition.flagIndex} / offset 0x${entry.definition.byteOffset.toString(16).uppercase()} / mask 0x${entry.definition.bitMask.toString(16).padStart(2, '0').uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(if (entry.foughtToday) "戦闘済み" else "再戦可能")
                    }
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

private fun sectionLabel(sectionName: String): String = when (sectionName) {
    "game0.yw" -> "オートセーブ"
    "game1.yw" -> "にっき1"
    "game2.yw" -> "にっき2"
    "game3.yw" -> "にっき3"
    else -> sectionName
}
