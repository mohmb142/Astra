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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mohmb142.astra.agent.AgentExecutor
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AstraScreen { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } } }
    }
}

@Composable
private fun AstraScreen(onAccessibility: () -> Unit) {
    var goal by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("google/gemini-2.5-flash") }
    var running by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf("جاهز. Astra الآن يملك حلقة ملاحظة → تخطيط → تنفيذ → تحقق.") }
    val scope = rememberCoroutineScope()
    val executor = remember { AgentExecutor() }

    Scaffold(topBar = { TopAppBar(title = { Text("✦ Astra Mobile") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("وكيل ذكي حقيقي للهاتف", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(goal, { goal = it }, Modifier.fillMaxWidth(), label = { Text("الهدف") }, minLines = 2)
            OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), label = { Text("مفتاح OpenRouter") }, visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("النموذج") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = goal.isNotBlank() && apiKey.isNotBlank() && !running, onClick = {
                    running = true; logs.add("🎯 الهدف: $goal")
                    scope.launch { executor.run(goal, apiKey, model) { logs.add(it) }; running = false }
                }) { Text(if (running) "يعمل..." else "ابدأ الوكيل") }
                OutlinedButton(onClick = onAccessibility) { Text("تفعيل التحكم") }
            }
            HorizontalDivider()
            Text("سجل المهمة", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) { items(logs) { Text("• $it") } }
        }
    }
}
