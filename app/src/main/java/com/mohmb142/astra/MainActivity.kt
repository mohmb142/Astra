package com.mohmb142.astra

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mohmb142.astra.agent.AstraEngine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AstraScreen(onAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
            }
        }
    }
}

@Composable
private fun AstraScreen(onAccessibility: () -> Unit) {
    var input by remember { mutableStateOf(TextFieldValue()) }
    var running by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf("جاهز. اكتب هدفًا لأسترا.") }

    Scaffold(topBar = { TopAppBar(title = { Text("✦ Astra Mobile") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("وكيل ذكي للهاتف", style = MaterialTheme.typography.headlineSmall)
            Text("يفهم الهدف، يخطط، وينفذ عبر Android Accessibility مع التحقق من النتائج.")
            OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(), label = { Text("ماذا تريد أن أفعل؟") }, minLines = 3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = input.text.isNotBlank() && !running, onClick = {
                    running = true
                    logs.add("الهدف: ${input.text}")
                    logs.add(AstraEngine.plan(input.text))
                    running = false
                }) { Text(if (running) "يعمل..." else "تنفيذ") }
                OutlinedButton(onClick = onAccessibility) { Text("تفعيل التحكم") }
            }
            HorizontalDivider()
            Text("سجل المهمة", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) { items(logs) { Text("• $it") } }
        }
    }
}
