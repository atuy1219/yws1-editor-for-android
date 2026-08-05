package com.atuy.yws1editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atuy.yws1editor.yokai.DailySimpleFlag
import com.atuy.yws1editor.yokai.DailySystemState

@Composable
internal fun DailySystemsTabContent(
    state: DailySystemState,
    enabled: Boolean,
    onBattleChange: (Int, Boolean) -> Unit,
    onSetAllBattles: (Boolean) -> Unit,
    onResetGasha: () -> Unit,
    onResetSasurai: () -> Unit,
    onSimpleFlagChange: (DailySimpleFlag, Boolean) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("一日一回系", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "チェックあり＝本日利用済みです。変更後は画面上部の保存を押してください。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onResetAll,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("すべての日次制限を解除")
                }
            }
        }

        item {
            DailyResetCard(
                title = "妖怪ガシャ",
                status = buildString {
                    append("本日の使用回数: ${state.gashaUseCount}")
                    append(" / 日次報酬: ")
                    append(if (state.gashaRewardClaimed) "受取済み" else "未受取")
                },
                explanation = "日次使用回数と1日1回報酬だけを戻します。抽選乱数stateと次に出る景品は変更しません。",
                buttonLabel = "ガシャの日次制限をリセット",
                enabled = enabled,
                onReset = onResetGasha,
            )
        }

        item {
            DailyResetCard(
                title = "さすらい荘",
                status = buildString {
                    append("抽選: ")
                    append(if (state.sasuraiRewardDrawn) "実行済み" else "未実行")
                    append(" / 報酬数: ${state.sasuraiRewardCount}")
                },
                explanation = "日次抽選と報酬数だけを戻します。9部屋の住人・妖怪・遭遇データは消しません。",
                buttonLabel = "さすらい荘の日次抽選をリセット",
                enabled = enabled,
                onReset = onResetSasurai,
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("その他の日次イベント", fontWeight = FontWeight.Bold)
                    Text(
                        "ROMのフラグ定義で『1日1回』と明記されているイベントです。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.simpleFlags.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled) {
                                    onSimpleFlagChange(entry.definition, !entry.usedToday)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = entry.usedToday,
                                onCheckedChange = if (enabled) { checked ->
                                    onSimpleFlagChange(entry.definition, checked)
                                } else {
                                    null
                                },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.definition.displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    entry.definition.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(if (entry.usedToday) "利用済み" else "利用可能")
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("一日一回バトル", fontWeight = FontWeight.Bold)
                    Text(
                        "友達済みやイベント進行フラグは変更せず、当日の戦闘済みフラグだけを操作します。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onSetAllBattles(false) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("全員再戦可能")
                        }
                        OutlinedButton(
                            onClick = { onSetAllBattles(true) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("全員戦闘済み")
                        }
                    }
                }
            }
        }

        items(state.battles, key = { it.definition.flagIndex }) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clickable(enabled = enabled) {
                        onBattleChange(entry.definition.flagIndex, !entry.foughtToday)
                    }
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = entry.foughtToday,
                    onCheckedChange = if (enabled) { checked ->
                        onBattleChange(entry.definition.flagIndex, checked)
                    } else {
                        null
                    },
                )
                Text(
                    entry.definition.name,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(if (entry.foughtToday) "戦闘済み" else "再戦可能")
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DailyResetCard(
    title: String,
    status: String,
    explanation: String,
    buttonLabel: String,
    enabled: Boolean,
    onReset: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(status)
            Text(
                explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onReset,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(buttonLabel)
            }
        }
    }
}
