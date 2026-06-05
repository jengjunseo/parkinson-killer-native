package com.parkinsonkiller

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val Background = Color(0xFFF6F7F9)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1F2937)
private val TextSecondary = Color(0xFF6B7280)
private val Accent = Color(0xFF7C3AED)
private val Danger = Color(0xFFDC2626)
private val DangerSoft = Color(0xFFFEE2E2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionNotification = SessionNotification(this)
        sessionNotification.ensureChannel()

        setContent {
            MaterialTheme {
                ParkinsonKillerApp(sessionNotification)
            }
        }
    }
}

@Composable
fun ParkinsonKillerApp(sessionNotification: SessionNotification) {
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var hasStartedFlow by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }
    var taskName by remember { mutableStateOf("") }
    var hoursInput by remember { mutableStateOf("") }
    var failMessage by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var isBlackout by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(0) }

    val calculatedMinutes = remember(hoursInput) {
        hoursInput.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }?.let {
            (it * 60).roundToInt()
        } ?: 0
    }

    LaunchedEffect(isRunning, remainingSeconds, taskName) {
        if (!isRunning) return@LaunchedEffect

        if (remainingSeconds <= 0) {
            sessionNotification.cancel()
            isRunning = false
            isBlackout = true
            return@LaunchedEffect
        }

        sessionNotification.show(taskName, formatTime(remainingSeconds))
        delay(1000)
        remainingSeconds -= 1
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background,
    ) {
        when {
            isBlackout -> BlackoutScreen(
                onExit = {
                    isBlackout = false
                    hasStartedFlow = false
                    step = 0
                },
            )

            isRunning -> FocusScreen(
                taskName = taskName,
                failMessage = failMessage,
                remainingSeconds = remainingSeconds,
                onAbort = {
                    sessionNotification.cancel()
                    isRunning = false
                    remainingSeconds = 0
                },
            )

            !hasStartedFlow -> HomeScreen(
                onStart = {
                    hasStartedFlow = true
                    step = 0
                },
            )

            else -> StartFlowScreen(
                step = step,
                taskName = taskName,
                onTaskNameChange = { taskName = it },
                hoursInput = hoursInput,
                onHoursInputChange = { hoursInput = it },
                calculatedMinutes = calculatedMinutes,
                failMessage = failMessage,
                onFailMessageChange = { failMessage = it },
                onNext = {
                    step = (step + 1).coerceAtMost(3)
                },
                onStartSession = {
                    remainingSeconds = calculatedMinutes * 60
                    isRunning = true
                },
            )
        }
    }
}

@Composable
private fun HomeScreen(onStart: () -> Unit) {
    CenterColumn {
        Text(
            text = "파킨슨 킬러",
            color = TextPrimary,
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(text = "시작하기", onClick = onStart)
    }
}

@Composable
private fun StartFlowScreen(
    step: Int,
    taskName: String,
    onTaskNameChange: (String) -> Unit,
    hoursInput: String,
    onHoursInputChange: (String) -> Unit,
    calculatedMinutes: Int,
    failMessage: String,
    onFailMessageChange: (String) -> Unit,
    onNext: () -> Unit,
    onStartSession: () -> Unit,
) {
    CenterColumn {
        when (step) {
            0 -> {
                StepTitle("무엇을 목표할건가요?")
                TextInputBox(
                    value = taskName,
                    onValueChange = onTaskNameChange,
                    placeholder = "예: 발표 자료 완성",
                )
                PrimaryButton(
                    text = "다음",
                    enabled = taskName.isNotBlank(),
                    onClick = onNext,
                )
            }

            1 -> {
                StepTitle("몇 시간 동안 할건가요?")
                TextInputBox(
                    value = hoursInput,
                    onValueChange = onHoursInputChange,
                    placeholder = "예: 1 또는 1.5",
                    keyboardType = KeyboardType.Decimal,
                )
                Text(
                    text = "1 = 60분, 1.5 = 90분으로 계산됩니다.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                PrimaryButton(
                    text = "다음",
                    enabled = calculatedMinutes > 0,
                    onClick = onNext,
                )
            }

            2 -> {
                StepTitle("실패시 나 자신에게 할 한마디는?")
                TextInputBox(
                    value = failMessage,
                    onValueChange = onFailMessageChange,
                    placeholder = "예: 또 미뤘다. 이번엔 인정하고 다시 시작하자.",
                    minLines = 4,
                )
                PrimaryButton(
                    text = "다음",
                    enabled = failMessage.isNotBlank(),
                    onClick = onNext,
                )
            }

            else -> {
                StepTitle("좋아요, 그러면 한번 해 보자고요!")
                Text(
                    text = "완벽한 하루보다 끝낸 한 시간이 더 강합니다.",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                )
                PrimaryButton(text = "스타트!", onClick = onStartSession)
            }
        }
    }
}

@Composable
private fun FocusScreen(
    taskName: String,
    failMessage: String,
    remainingSeconds: Int,
    onAbort: () -> Unit,
) {
    CenterColumn {
        Text(
            text = "TIMEBOXING 진행 중",
            color = Accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "현재 달성중인 목표", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = taskName,
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            text = formatTime(remainingSeconds),
            color = TextPrimary,
            fontSize = 82.sp,
            fontWeight = FontWeight.Black,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DangerSoft, RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "나와의 약속, 잊지 말 것", color = Danger, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$failMessage\"",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        DangerButton(text = "패배선언 (종료하기)", onClick = onAbort)
    }
}

@Composable
private fun BlackoutScreen(onExit: () -> Unit) {
    CenterColumn {
        Text(
            text = "종료!",
            color = Danger,
            fontSize = 70.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "즉시 손을 떼십시오!",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(text = "완벽주의는 죄악입니다.", color = TextSecondary, fontSize = 18.sp)
        PrimaryButton(text = "인정하고 나가기", onClick = onExit)
    }
}

@Composable
private fun CenterColumn(content: @Composable Column.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        color = TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        lineHeight = 32.sp,
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun TextInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Accent),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun DangerButton(text: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DangerSoft,
            contentColor = Danger,
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val restSeconds = seconds % 60
    return "%02d:%02d".format(minutes, restSeconds)
}
