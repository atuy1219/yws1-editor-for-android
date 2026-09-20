package com.atuy.yws1editor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atuy.yws1editor.ui.theme.YwEditorTheme

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YwEditorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GameSelectionScreen(
                        onYw1 = {
                            startActivity(Intent(this, MainActivity::class.java))
                        },
                        onYw2 = {
                            startActivity(Intent(this, Yw2MainActivity::class.java))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GameSelectionScreen(
    onYw1: () -> Unit,
    onYw2: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "妖怪ウォッチ セーブエディタ",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "編集するゲームを選択してください",
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = onYw1,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("妖怪ウォッチ1 スマホ")
        }

        OutlinedButton(
            onClick = onYw2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text("妖怪ウォッチ2 真打")
        }
    }
}
