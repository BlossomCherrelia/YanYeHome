package com.yanye.home.ui.food

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.R
import com.yanye.home.domain.model.Restaurant
import com.yanye.home.ui.common.AutoSyncLifecycleEffect
import com.yanye.home.ui.common.PageChrome
import com.yanye.home.ui.common.SecondaryTopBar
import com.yanye.home.ui.common.WallpaperBackground
import com.yanye.home.ui.theme.YanYeColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class FoodPickMode {
    Blind,
    Cuisine,
    Rules
}

@Composable
fun FoodScreen(
    viewModel: FoodViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val restaurants by viewModel.restaurants.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    AutoSyncLifecycleEffect(
        onEnterSync = viewModel::syncRestaurants,
        onFlushSync = viewModel::flushSync
    )
    var mode by remember { mutableStateOf(FoodPickMode.Blind) }
    var ruleOptions by remember { mutableStateOf(listOf("1")) }
    var showRestaurantEditor by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }

    val stableWheelRestaurants = restaurants.sortedWith(
        compareBy<Restaurant> { it.createdAt }
            .thenBy { it.id }
            .thenBy { it.name }
    )
    val blindOptions = stableWheelRestaurants.map { it.name }
    val cuisineOptions = stableWheelRestaurants
        .map { it.cuisine.trim() }
        .filter { it.isNotBlank() }
        .distinct()
    val ruleOptionsForWheel = ruleOptions.mapIndexed { index, value ->
        value.trim().ifBlank { (index + 1).toString() }
    }
    val wheelOptions = when (mode) {
        FoodPickMode.Blind -> blindOptions
        FoodPickMode.Cuisine -> cuisineOptions
        FoodPickMode.Rules -> ruleOptionsForWheel
    }

    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PageChrome.secondaryPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FoodHeader(
                    syncMessage = syncMessage,
                    onBack = onBack,
                    onSyncClick = viewModel::syncRestaurants
                )
            }
            item {
                ModePicker(
                    mode = mode,
                    onModeChange = { nextMode ->
                        if (nextMode != mode) {
                            mode = nextMode
                            resultText = null
                            if (nextMode != FoodPickMode.Rules) {
                                ruleOptions = listOf("1")
                            }
                        }
                    }
                )
            }
            item {
                key(mode) {
                    WheelCard(
                        title = when (mode) {
                            FoodPickMode.Blind -> "盲选转盘"
                            FoodPickMode.Cuisine -> "菜系转盘"
                            FoodPickMode.Rules -> "规则转盘"
                        },
                        options = wheelOptions,
                        emptyText = when (mode) {
                            FoodPickMode.Blind -> "先添加餐厅。"
                            FoodPickMode.Cuisine -> "先给餐厅填写菜系。"
                            FoodPickMode.Rules -> "至少保留一个选项。"
                        },
                        onSpinResult = { selected ->
                            if (mode == FoodPickMode.Blind) {
                                restaurants.firstOrNull { it.name == selected }?.let { viewModel.markPicked(it.id) }
                            }
                            resultText = "结果：$selected"
                        }
                    )
                }
            }
            if (mode == FoodPickMode.Rules) {
                item {
                    RuleOptionsCard(
                        options = ruleOptions,
                        onOptionChange = { index, value ->
                            ruleOptions = ruleOptions.toMutableList().also { it[index] = value }
                        },
                        onAdd = { ruleOptions = ruleOptions + "" },
                        onRemove = {
                            if (ruleOptions.size > 1) {
                                ruleOptions = ruleOptions.dropLast(1)
                            }
                        }
                    )
                }
            }
            item {
                RestaurantPoolHeader(
                    restaurantCount = restaurants.size,
                    onAddClick = { showRestaurantEditor = true }
                )
            }
            if (restaurants.isEmpty()) {
                item {
                    EmptyRestaurantCard(onAddClick = { showRestaurantEditor = true })
                }
            } else {
                items(
                    items = restaurants,
                    key = { restaurant -> restaurant.id }
                ) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        onDeleteClick = { viewModel.deleteRestaurant(restaurant.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showRestaurantEditor = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp),
            containerColor = YanYeColors.Gold,
            contentColor = Color.White
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }

    if (showRestaurantEditor) {
        RestaurantEditorDialog(
            onDismiss = { showRestaurantEditor = false },
            onSave = { restaurant ->
                viewModel.saveRestaurant(restaurant)
                showRestaurantEditor = false
            }
        )
    }

    resultText?.let { text ->
        AlertDialog(
            onDismissRequest = { resultText = null },
            title = { Text("转盘结果") },
            text = {
                Text(
                    text = text,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            confirmButton = {
                TextButton(onClick = { resultText = null }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
private fun FoodHeader(
    syncMessage: String?,
    onBack: () -> Unit,
    onSyncClick: () -> Unit
) {
    Column {
        SecondaryTopBar(
            title = "今天吃什么",
            actionText = "同步",
            actionColor = YanYeColors.Gold,
            onBack = onBack,
            onActionClick = onSyncClick
        )
        syncMessage?.let { message ->
            Text(
                text = message,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ModePicker(
    mode: FoodPickMode,
    onModeChange: (FoodPickMode) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = mode == FoodPickMode.Blind,
            onClick = { onModeChange(FoodPickMode.Blind) },
            label = { Text("盲选") }
        )
        FilterChip(
            selected = mode == FoodPickMode.Cuisine,
            onClick = { onModeChange(FoodPickMode.Cuisine) },
            label = { Text("菜系转盘") }
        )
        FilterChip(
            selected = mode == FoodPickMode.Rules,
            onClick = { onModeChange(FoodPickMode.Rules) },
            label = { Text("已有规则") }
        )
    }
}

@Composable
private fun WheelCard(
    title: String,
    options: List<String>,
    emptyText: String,
    onSpinResult: (String) -> Unit
) {
    var targetRotation by remember { mutableStateOf(0f) }
    var pendingResult by remember { mutableStateOf<String?>(null) }
    var isSpinning by remember { mutableStateOf(false) }
    val pointerRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 1300),
        label = "food-wheel-pointer"
    )

    LaunchedEffect(pendingResult) {
        val result = pendingResult ?: return@LaunchedEffect
        delay(1300)
        onSpinResult(result)
        pendingResult = null
        isSpinning = false
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    WheelCanvas(options = options)
                    PointerCanvas(pointerRotation = pointerRotation)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.27f)
                            .aspectRatio(1f)
                            .background(Color.White, MaterialTheme.shapes.extraLarge)
                            .clickable(enabled = options.isNotEmpty() && !isSpinning) {
                                val selectedIndex = Random.nextInt(options.size)
                                val sweep = 360f / options.size
                                val targetAngle = selectedIndex * sweep + sweep / 2f
                                val currentAngle = positiveModulo(targetRotation, 360f)
                                val angleToTarget = positiveModulo(targetAngle - currentAngle, 360f)
                                val extraTurns = 4 + Random.nextInt(3)
                                targetRotation += extraTurns * 360f + angleToTarget
                                pendingResult = options[selectedIndex]
                                isSpinning = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "转",
                            color = if (options.isEmpty()) YanYeColors.Muted else YanYeColors.Ink,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (options.isEmpty()) {
                Text(
                    text = emptyText,
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.take(8).forEach { option ->
                        AssistChip(onClick = {}, label = { Text(option) })
                    }
                    if (options.size > 8) {
                        AssistChip(onClick = {}, label = { Text("+${options.size - 8}") })
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelCanvas(options: List<String>) {
    val colors = listOf(
        YanYeColors.GoldSoft,
        YanYeColors.RoseSoft,
        YanYeColors.GreenSoft,
        YanYeColors.BlueSoft,
        YanYeColors.Soft
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val sweep = if (options.isEmpty()) 360f else 360f / options.size
        val segmentCount = options.size.coerceAtLeast(1)
        val center = Offset(size.width / 2f, size.height / 2f)
        repeat(segmentCount) { index ->
            drawArc(
                color = colors[index % colors.size],
                startAngle = -90f + index * sweep,
                sweepAngle = sweep,
                useCenter = true
            )
        }
        if (options.isNotEmpty()) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(37, 33, 29)
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = when {
                    options.size <= 4 -> 18.dp.toPx()
                    options.size <= 8 -> 14.dp.toPx()
                    else -> 11.dp.toPx()
                }
            }
            val textRadius = size.minDimension * 0.34f
            options.forEachIndexed { index, option ->
                val angleDegrees = -90f + index * sweep + sweep / 2f
                val angleRadians = angleDegrees * PI.toFloat() / 180f
                val label = option.toWheelLabel()
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    center.x + cos(angleRadians) * textRadius,
                    center.y + sin(angleRadians) * textRadius + paint.textSize / 3f,
                    paint
                )
            }
        }
        drawCircle(
            color = YanYeColors.Line,
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = Color.White,
            radius = size.minDimension * 0.18f,
            center = center
        )
    }
}

@Composable
private fun PointerCanvas(pointerRotation: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        rotate(degrees = pointerRotation, pivot = center) {
            drawLine(
                color = YanYeColors.Ink,
                start = center,
                end = Offset(size.width / 2f, size.height * 0.08f),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = YanYeColors.Rose,
                radius = 7.dp.toPx(),
                center = Offset(size.width / 2f, size.height * 0.08f)
            )
        }
    }
}

@Composable
private fun RuleOptionsCard(
    options: List<String>,
    onOptionChange: (Int, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "规则选项",
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRemove, enabled = options.size > 1) { Text("-1") }
                    Button(onClick = onAdd) { Text("+1") }
                }
            }
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEachIndexed { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { onOptionChange(index, it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("选项 ${index + 1}") }
                    )
                }
            }
        }
    }
}

@Composable
private fun RestaurantPoolHeader(
    restaurantCount: Int,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "餐厅池 · ${restaurantCount}个选择",
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onAddClick) {
            Text("添加餐厅")
        }
    }
}

@Composable
private fun EmptyRestaurantCard(onAddClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "还没有餐厅",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(onClick = onAddClick, modifier = Modifier.padding(top = 16.dp)) {
                Text("添加餐厅")
            }
        }
    }
}

@Composable
private fun RestaurantCard(
    restaurant: Restaurant,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = restaurant.name,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = restaurant.cuisine.ifBlank { "未设置菜系" },
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            TextButton(onClick = onDeleteClick) {
                Text("删除", color = YanYeColors.Rose)
            }
        }
    }
}

@Composable
private fun RestaurantEditorDialog(
    onDismiss: () -> Unit,
    onSave: (Restaurant) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var cuisine by remember { mutableStateOf("") }
    val canSave = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加餐厅") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("餐厅名") }
                )
                OutlinedTextField(
                    value = cuisine,
                    onValueChange = { cuisine = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("菜系") }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        Restaurant(
                            name = name.trim(),
                            cuisine = cuisine.trim()
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun positiveModulo(value: Float, modulo: Float): Float =
    ((value % modulo) + modulo) % modulo

private fun String.toWheelLabel(): String {
    val compact = trim()
    return if (compact.length <= 7) compact else compact.take(6) + "..."
}
