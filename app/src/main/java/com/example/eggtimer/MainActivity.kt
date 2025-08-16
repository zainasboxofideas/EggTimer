@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.eggtimer  // ← আপনার প্যাকেজ নাম দিন

import androidx.compose.material3.ExperimentalMaterial3Api

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { // কোনো কাস্টম থিম নেই
                EggTimerApp()
            }
        }
    }
}

private enum class EggLevel(val label: String, val minutes: Int) {
    Soft("Soft (6 min)", 6),
    Medium("Medium (9 min)", 9),
    Hard("Hard (12 min)", 12),
    Custom("Custom", 8) // ডিফল্ট কাস্টম
}

@Composable
private fun EggTimerApp() {
    val context = thisLocalContext()

    // শুধু remember ব্যবহার করেছি → কোনো extra dependency লাগবে না
    var level by remember { mutableStateOf(EggLevel.Soft) }
    var customMinutesText by remember { mutableStateOf("8") }

    val totalMillis: Long by remember(level, customMinutesText) {
        val mins = if (level == EggLevel.Custom)
            customMinutesText.toIntOrNull()?.coerceIn(1, 60) ?: 8
        else
            level.minutes
        mutableStateOf(mins * 60_000L)
    }

    var remaining by remember(level, totalMillis) { mutableStateOf(totalMillis) }
    var running by remember { mutableStateOf(false) }

    // লেভেল/টাইম বদলালে রিসেট
    LaunchedEffect(totalMillis) {
        running = false
        remaining = totalMillis
    }

    // টিক-টিক লুপ
    LaunchedEffect(running, remaining) {
        while (running && remaining > 0L) {
            delay(1_000L)
            remaining = (remaining - 1_000L).coerceAtLeast(0L)
        }
        if (running && remaining == 0L) {
            running = false
            notifyFinish(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("🥚 Egg-Boil Timer", fontWeight = FontWeight.SemiBold) })
        }
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            TimerScreen(
                level = level,
                onLevelChange = { level = it },
                customMinutesText = customMinutesText,
                onCustomMinutesChange = { txt ->
                    customMinutesText = txt.filter { ch -> ch.isDigit() }.take(2)
                },
                totalMillis = totalMillis,
                remainingMillis = remaining,
                running = running,
                onStartPause = { running = !running },
                onReset = {
                    running = false
                    remaining = totalMillis
                }
            )
        }
    }
}

@Composable
private fun TimerScreen(
    level: EggLevel,
    onLevelChange: (EggLevel) -> Unit,
    customMinutesText: String,
    onCustomMinutesChange: (String) -> Unit,
    totalMillis: Long,
    remainingMillis: Long,
    running: Boolean,
    onStartPause: () -> Unit,
    onReset: () -> Unit
) {
    val progress = if (totalMillis == 0L) 0f
    else 1f - (remainingMillis.toFloat() / totalMillis.toFloat())

    Column(
        Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "🥚", fontSize = 64.sp)

        // সহজ RadioButton group — কোনো experimental/extra lib লাগবে না
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Boil level:", fontWeight = FontWeight.SemiBold)
            LevelOption(EggLevel.Soft, level, onLevelChange)
            LevelOption(EggLevel.Medium, level, onLevelChange)
            LevelOption(EggLevel.Hard, level, onLevelChange)
            LevelOption(EggLevel.Custom, level, onLevelChange)
        }

        if (level == EggLevel.Custom) {
            OutlinedTextField(
                value = customMinutesText,
                onValueChange = onCustomMinutesChange,
                singleLine = true,
                label = { Text("Custom minutes (1–60)") }
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                strokeWidth = 10.dp,
                modifier = Modifier.size(220.dp)
            )
            Text(
                text = formatTime(remainingMillis),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            Button(onClick = onStartPause, modifier = Modifier.weight(1f)) {
                Text(if (running) "Pause" else if (remainingMillis == 0L) "Restart" else "Start")
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                Text("Reset")
            }
        }

        Text(
            "Tip: Start the timer as soon as you put the eggs in!",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LevelOption(
    option: EggLevel,
    selected: EggLevel,
    onSelect: (EggLevel) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected == option,
            onClick = { onSelect(option) }
        )
        Spacer(Modifier.width(8.dp))
        Text(option.label)
    }
}

private fun formatTime(millis: Long): String {
    val totalSec = (millis / 1000).toInt().coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

@Composable
private fun thisLocalContext(): Context {
    // ছোট helper যাতে imports কম থাকে
    return androidx.compose.ui.platform.LocalContext.current
}

private fun notifyFinish(context: Context) {
    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val tone: Ringtone = RingtoneManager.getRingtone(context, alarmUri)
    tone.play()

    val effect =
        if (Build.VERSION.SDK_INT >= 26)
            VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1)
        else
            @Suppress("DEPRECATION")
            VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)

    if (Build.VERSION.SDK_INT >= 31) {
        val vm = context.getSystemService(VibratorManager::class.java)
        vm?.defaultVibrator?.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(effect)
    }
}
