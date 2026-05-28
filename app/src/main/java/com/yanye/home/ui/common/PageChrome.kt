package com.yanye.home.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanye.home.ui.theme.YanYeColors

object PageChrome {
    val horizontalPadding = 18.dp
    val topPadding = 22.dp
    val secondaryTopPadding = topPadding
    val bottomPadding = 96.dp

    val primaryPadding: PaddingValues
        get() = PaddingValues(
            start = horizontalPadding,
            top = topPadding,
            end = horizontalPadding,
            bottom = bottomPadding
        )

    val secondaryPadding: PaddingValues
        get() = PaddingValues(
            start = horizontalPadding,
            top = secondaryTopPadding,
            end = horizontalPadding,
            bottom = bottomPadding
        )
}

@Composable
fun HeaderTextAction(
    text: String,
    color: Color = YanYeColors.Muted,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun PrimaryPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
        HorizontalDivider(
            color = YanYeColors.Line,
            thickness = 0.8.dp,
            modifier = Modifier.padding(top = 14.dp)
        )
        message?.let {
            Text(
                text = it,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
