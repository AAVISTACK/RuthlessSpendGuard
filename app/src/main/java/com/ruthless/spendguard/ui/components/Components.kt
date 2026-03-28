package com.ruthless.spendguard.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.ruthless.spendguard.ui.theme.SG
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

// ─── SURFACE CARD ─────────────────────────────────────────────────────────────

@Composable
fun SGCard(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = accentColor?.copy(alpha = 0.25f) ?: SG.CardBorder

    val baseModifier = modifier
        .clip(RoundedCornerShape(16.dp))
        .background(SG.Card)
        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Column(
        modifier = baseModifier.padding(16.dp),
        content = content
    )
}

// ─── SECTION LABEL ────────────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = SG.TextDim,
        modifier = modifier
    )
}

// ─── DIVIDER ──────────────────────────────────────────────────────────────────

@Composable
fun SGDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = SG.Divider
    )
}

// ─── BUDGET ARC ───────────────────────────────────────────────────────────────

@Composable
fun BudgetArc(
    spent: Double,
    limit: Double,
    modifier: Modifier = Modifier
) {
    // FIX: guard against division-by-zero when limit is 0
    val safeLimit = limit.coerceAtLeast(0.01)
    val progress = (spent / safeLimit).coerceIn(0.0, 1.0).toFloat()
    val isOver = spent >= safeLimit
    val isDanger = progress >= 0.7f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "arc"
    )

    val accentColor = when {
        isOver   -> SG.Red
        isDanger -> lerp(SG.Green, SG.Red, (progress - 0.7f) / 0.3f)
        else     -> SG.Green
    }

    val statusText = when {
        isOver           -> "LIMIT EXCEEDED"
        progress >= 0.7f -> "SLIPPING"
        progress >= 0.4f -> "CAUTION"
        progress > 0f    -> "CONTROLLED"
        else             -> "CLEAN"
    }

    val statusColor = when {
        isOver           -> SG.Red
        progress >= 0.7f -> SG.Amber
        else             -> SG.Green
    }

    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val stroke = 10.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val startAngle = 135f
            val sweepTotal = 270f

            // Track
            drawArc(
                color = Color(0xFF242424),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Progress
            if (animatedProgress > 0.005f) {
                drawArc(
                    color = accentColor,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "₹${spent.toLong()}",
                style = MaterialTheme.typography.displayMedium,
                color = SG.TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "of ₹${limit.toLong()}",
                style = MaterialTheme.typography.labelMedium,
                color = SG.TextBody
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
        }
    }
}

// ─── BUDGET BAR ───────────────────────────────────────────────────────────────

@Composable
fun BudgetBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    val barColor = when {
        safeProgress >= 1f   -> SG.Red
        safeProgress >= 0.7f -> SG.Amber
        else                 -> SG.Green
    }

    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "budgetBar"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(SG.CardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
    }
}

// ─── STATS ROW ────────────────────────────────────────────────────────────────

@Composable
fun StatsRow(
    waste: Double,
    essential: Double,
    txnCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatPill(
            label = "WASTE",
            value = "₹${waste.toLong()}",
            color = SG.Red,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = "ESSENTIAL",
            value = "₹${essential.toLong()}",
            color = SG.Green,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = "TXN",
            value = txnCount.toString(),
            color = SG.TextBody,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SG.Card)
            .border(1.dp, SG.CardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = SG.TextDim)
    }
}

// ─── TRANSACTION ITEM ─────────────────────────────────────────────────────────

@Composable
fun TransactionItem(
    merchant: String,
    amount: Double,
    category: String,
    isWaste: Boolean,
    time: Long,
    onDelete: (() -> Unit)? = null
) {
    val amountColor = if (isWaste) SG.Red else SG.Green

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(merchant, style = MaterialTheme.typography.titleMedium, color = SG.TextPrimary)
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(category, style = MaterialTheme.typography.labelMedium, color = SG.TextDim)
                Text("·", style = MaterialTheme.typography.labelMedium, color = SG.TextDim)
                Text(formatTime(time), style = MaterialTheme.typography.labelMedium, color = SG.TextDim)
            }
        }
        Text(
            text = "-₹${amount.toLong()}",
            style = MaterialTheme.typography.titleMedium,
            color = amountColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── GOAL CARD ────────────────────────────────────────────────────────────────

@Composable
fun GoalCard(
    name: String,
    emoji: String,
    saved: Double,
    target: Double,
    wasteToday: Double,
    modifier: Modifier = Modifier
) {
    // FIX: guard against division-by-zero
    val safeTarget = target.coerceAtLeast(0.01)
    val progress = (saved / safeTarget).coerceIn(0.0, 1.0).toFloat()
    val daysLeft = if (wasteToday > 0) ((target - saved) / wasteToday).toInt() else 0

    SGCard(modifier = modifier, accentColor = SG.Green) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = SG.TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    "₹${saved.toLong()} / ₹${target.toLong()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SG.TextBody
                )
            }
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = SG.Green
            )
        }
        Spacer(Modifier.height(10.dp))
        BudgetBar(progress = progress)
        if (daysLeft > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "~$daysLeft days to reach goal if waste stops",
                style = MaterialTheme.typography.labelSmall,
                color = SG.TextDim
            )
        }
    }
}

// ─── PRIMARY BUTTON ───────────────────────────────────────────────────────────

@Composable
fun SGButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SG.Green,
    outlined: Boolean = false
) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, color),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = SG.TextOnAccent
            )
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ─── STREAK ROW ───────────────────────────────────────────────────────────────

@Composable
fun StreakRow(
    pairs: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    SGCard(modifier = modifier) {
        SectionLabel("STREAKS")
        Spacer(Modifier.height(10.dp))
        pairs.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, count) ->
                    StreakBadge(label = label, count = count, modifier = Modifier.weight(1f))
                }
                // Fill remaining space if odd number
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            if (row != pairs.chunked(2).last()) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StreakBadge(label: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SG.Surface)
            .border(1.dp, SG.CardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("$count", style = MaterialTheme.typography.titleMedium, color = SG.Green, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SG.TextBody)
    }
}

// ─── LOCKDOWN WALL ────────────────────────────────────────────────────────────

@Composable
fun LockdownWall(spent: Double, limit: Double) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SG.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(36.dp)
        ) {
            Text(
                "🔒",
                fontSize = 48.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "LOCKED",
                style = MaterialTheme.typography.headlineLarge,
                color = SG.Red
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "You've spent ₹${spent.toLong()} of your ₹${limit.toLong()} limit.",
                style = MaterialTheme.typography.bodyLarge,
                color = SG.TextBody,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Come back tomorrow.",
                style = MaterialTheme.typography.bodyMedium,
                color = SG.TextDim,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── SHAME SCREEN ─────────────────────────────────────────────────────────────

@Composable
fun ShameScreen(failCount: Int, onAcknowledge: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SG.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(36.dp)
        ) {
            Text(
                "Tu control me nahi hai.",
                style = MaterialTheme.typography.headlineLarge.copy(lineHeight = 30.sp),
                color = SG.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Baar baar same mistake.\n$failCount baar ho chuka hai.",
                style = MaterialTheme.typography.bodyLarge,
                color = SG.TextBody,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(48.dp))
            SGButton("I will do better", onClick = onAcknowledge)
        }
    }
}

// ─── TOP BAR ──────────────────────────────────────────────────────────────────

@Composable
fun TopBar(onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "SPENDGUARD",
                style = MaterialTheme.typography.headlineMedium,
                color = SG.TextPrimary
            )
            Text(
                "TODAY",
                style = MaterialTheme.typography.labelSmall,
                color = SG.TextDim
            )
        }
        IconButton(onClick = onSettings) {
            // Settings icon placeholder — replaced at call site with material icon
        }
    }
}

// ─── HELPERS ──────────────────────────────────────────────────────────────────

// FIX: was using non-thread-safe SimpleDateFormat as a top-level object.
// Now it is recreated per call — for better practice in a real app use
// DateTimeFormatter (java.time) on API 26+ which this project targets.
fun formatTime(ts: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(ts))
}

fun lerp(start: Color, end: Color, f: Float): Color {
    val t = f.coerceIn(0f, 1f)
    return Color(
        red   = start.red   + (end.red   - start.red)   * t,
        green = start.green + (end.green - start.green) * t,
        blue  = start.blue  + (end.blue  - start.blue)  * t,
        alpha = 1f
    )
}

@Composable
fun NavTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) SG.Green else SG.TextDim,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) SG.Green else SG.TextDim
        )
    }
}

