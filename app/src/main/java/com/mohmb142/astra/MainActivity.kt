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
import com.mohmb142.astra.agent.TaskHistoryStore
import kotlinx.coroutines.Job
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
    val logs = remember { mutableStateListOf("جاهز. Astra يعمل بنمط Observe → Plan → Act → Verify → Recover.") }
    val scope = rememberCoroutineScope()
    val executor = remember { AgentExecutor() }
    val history = remember { mutableStateOf<List<String>>(emptyList()) }
    var job by remember { mutableStateOf<Job?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { TaskHistoryStore(context) }

    fun refreshHistory() { history.value = store.recent() }
    LaunchedEffect(Unit) { refreshHistory() }

    Scaffold(topBar = { TopAppBar(title = { Text("✦ Astra Mobile") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("وكيل ذكي حقيقي للهاتف", style = MaterialTheme.typography.headlineSmall)
            Text(if (running) "🟢 الوكيل يعمل" else "⚪ الوكيل متوقف")
            OutlinedTextField(goal, { goal = it }, Modifier.fillMaxWidth(), label = { Text("الهدف") }, minLines = 2)
            OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), label = { Text("مفتاح OpenRouter") }, visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("النموذج") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = goal.isNotBlank() && apiKey.isNotBlank() && !running, onClick = {
                    running = true
                    logs.add("🎯 الهدف: $goal")
                    job = scope.launch {
                        val success = executor.run(goal, apiKey, model) { logs.add(it) }
                        store.add(goal, success)
                        refreshHistory()
                        running = false
                    }
                }) { Text("ابدأ الوكيل") }
                OutlinedButton(enabled = running, onClick = {
                    job?.cancel(); executor.stop(); running = false; logs.add("🛑 تم إيقاف الوكيل يدويًا.")
                }) { Text("إيقاف") }
                OutlinedButton(onClick = onAccessibility) { Text("تفعيل التحكم") }
            }
            HorizontalDivider()
            Text("سجل المهمة", style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) { items(logs) { Text("• $it") } }
            Text("آخر المهام", style = MaterialTheme.typography.titleMedium)
            history.value.take(5).forEach { Text("• $it") }
        }
    }
}
