package app.rork.qu1cg8ux8i9pbr72mmsss.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rork.qu1cg8ux8i9pbr72mmsss.ui.theme.Mpay
import kotlin.math.roundToInt

const val PIN_LENGTH = 4

@Composable
fun PinDots(length: Int, errorTick: Int, modifier: Modifier = Modifier) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorTick) {
        if (errorTick > 0) {
            for (x in listOf(18f, -16f, 12f, -8f, 4f, 0f)) shake.animateTo(x, spring(stiffness = 4000f))
        }
    }
    Row(
        modifier.offset { IntOffset(shake.value.roundToInt(), 0) }.semantics { contentDescription = "Введено $length из $PIN_LENGTH" },
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        repeat(PIN_LENGTH) { i ->
            val filled = i < length
            val c by animateColorAsState(if (errorTick > 0 && length == 0 && shake.isRunning) Mpay.Expense else if (filled) Mpay.Lime else Mpay.SurfaceHigh, label = "dot")
            Box(Modifier.size(16.dp).background(c, CircleShape))
        }
    }
}

@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onBiometric: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val rows = listOf("123", "456", "789")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                r.forEach { ch ->
                    PadKey(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onDigit(ch) }) {
                        Text(ch.toString(), fontSize = 28.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (onBiometric != null) {
                PadKey(onClick = onBiometric, transparent = true, label = "Отпечаток пальца") {
                    Icon(Icons.Rounded.Fingerprint, null, tint = Mpay.Lime, modifier = Modifier.size(32.dp))
                }
            } else Box(Modifier.size(76.dp))
            PadKey(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onDigit('0') }) {
                Text("0", fontSize = 28.sp, fontWeight = FontWeight.Medium)
            }
            PadKey(onClick = onDelete, transparent = true, label = "Стереть") {
                Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = Mpay.TextSecondary)
            }
        }
    }
}

@Composable
private fun PadKey(onClick: () -> Unit, transparent: Boolean = false, label: String? = null, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (transparent) Mpay.Canvas.copy(alpha = 0f) else Mpay.Surface,
        contentColor = Mpay.TextPrimary,
        modifier = Modifier.size(76.dp).then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
