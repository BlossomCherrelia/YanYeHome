package com.yanye.home.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanye.home.ui.theme.YanYeColors

@Composable
fun SecondaryTopBar(
    title: String,
    actionText: String? = null,
    actionColor: androidx.compose.ui.graphics.Color = YanYeColors.Rose,
    onBack: () -> Unit,
    onActionClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "‹",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onBack)
                    .padding(end = 14.dp)
            )
            Text(
                text = title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            if (actionText != null && onActionClick != null) {
                Text(
                    text = actionText,
                    color = actionColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable(onClick = onActionClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        HorizontalDivider(
            color = YanYeColors.Line,
            thickness = 0.8.dp,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}
