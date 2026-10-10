package com.example.notifyzerpocphase1.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AssetLiabilityHud(
    score: Int?,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = when (score) {
        null -> Triple(Color(0xFF334155), Color(0xFF94A3B8), "UNANALYZED BASELINE")
        in 0..49 -> Triple(Color(0xFF7F1D1D), Color(0xFFFCA5A5), "HIGH LIABILITY — SCORE: $score")
        else -> Triple(Color(0xFF064E3B), Color(0xFF6EE7B7), "HIGH ASSET — SCORE: $score")
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HUD MONITOR",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = textColor.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
