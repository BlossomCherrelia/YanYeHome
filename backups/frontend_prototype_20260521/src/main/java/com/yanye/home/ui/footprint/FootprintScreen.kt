package com.yanye.home.ui.footprint

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yanye.home.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.domain.model.CityLight
import com.yanye.home.domain.model.CityMemory
import com.yanye.home.domain.model.ProvinceLight
import com.yanye.home.ui.common.AutoSyncLifecycleEffect
import com.yanye.home.ui.theme.YanYeColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val DefaultProvinceColorArgb = -34150
private val ProvinceColorChoices = listOf(
    -34150,
    -15916,
    -11410,
    -7350348,
    -7817217
)

private fun cityLightKey(
    provinceName: String,
    cityName: String
): String = "$provinceName|$cityName"

@Composable
fun FootprintScreen(
    viewModel: FootprintViewModel = viewModel()
) {
    val context = LocalContext.current
    val provinceShapes = remember(context) { loadChinaProvinceShapes(context) }
    val provinceLights by viewModel.provinceLights.collectAsState()
    val cityMemories by viewModel.cityMemories.collectAsState()
    val cityLights by viewModel.cityLights.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    AutoSyncLifecycleEffect(
        onEnterSync = viewModel::syncFootprints,
        onFlushSync = viewModel::flushSync
    )
    var selectedProvinceName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCityName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedProvince = selectedProvinceName?.let { name ->
        ChinaProvinceMap.firstOrNull { it.name == name }
    }
    val selectedProvinceShape = selectedProvinceName?.let { name ->
        provinceShapes.firstOrNull { it.name == name }
    }
    val selectedCityShapes = remember(context, selectedProvinceShape?.adcode) {
        selectedProvinceShape?.let { loadProvinceCityShapes(context, it.adcode) }.orEmpty()
    }
    val provinceLightMap = provinceLights.associateBy { it.provinceName }
    val cityLightMap = cityLights.associateBy { cityLightKey(it.provinceName, it.cityName) }
    val litProvinceNames = remember(provinceLights) {
        provinceLights.filter { it.isLit }.map { it.provinceName }.toSet()
    }
    val litCityNames = remember(cityLights, selectedProvinceName) {
        cityLights
            .filter { it.provinceName == selectedProvinceName && it.isLit }
            .map { it.cityName }
            .toSet()
    }
    val provinceCount = ChinaProvinceMap.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(YanYeColors.Paper),
        contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            when {
                selectedProvince == null -> {
                    FootprintStatsHeader(
                        provinceCount = provinceCount,
                        litProvinceCount = litProvinceNames.size,
                        memoryCount = cityMemories.size,
                        selectedProvince = null,
                        syncMessage = syncMessage,
                        onSyncClick = viewModel::syncFootprints,
                        onBackToChina = {}
                    )
                }
                selectedCityName != null -> {
                    val cityLight = cityLightMap[cityLightKey(selectedProvince.name, selectedCityName!!)]
                    LightSwitchPanel(
                        title = "点亮$selectedCityName",
                        isLit = cityLight?.isLit == true,
                        colorArgb = cityLight?.fillColorArgb ?: DefaultProvinceColorArgb,
                        backLabel = selectedProvince.name,
                        onLitChange = { isLit ->
                            viewModel.saveCityLight(
                                cityLight?.copy(isLit = isLit)
                                    ?: CityLight(
                                        provinceName = selectedProvince.name,
                                        cityName = selectedCityName!!,
                                        isLit = isLit
                                    )
                            )
                        },
                        onColorChange = { colorArgb ->
                            viewModel.saveCityLight(
                                cityLight?.copy(isLit = true, fillColorArgb = colorArgb)
                                    ?: CityLight(
                                        provinceName = selectedProvince.name,
                                        cityName = selectedCityName!!,
                                        isLit = true,
                                        fillColorArgb = colorArgb
                                    )
                            )
                        },
                        onBack = {
                            selectedCityName = null
                        }
                    )
                }
                else -> {
                    val provinceLight = provinceLightMap[selectedProvince.name]
                    LightSwitchPanel(
                        title = "点亮${selectedProvince.name}",
                        isLit = provinceLight?.isLit == true,
                        colorArgb = provinceLight?.fillColorArgb ?: DefaultProvinceColorArgb,
                        backLabel = "全国",
                        onLitChange = { isLit ->
                            viewModel.saveProvinceLight(
                                provinceLight?.copy(isLit = isLit)
                                    ?: ProvinceLight(
                                        provinceName = selectedProvince.name,
                                        isLit = isLit
                                    )
                            )
                        },
                        onColorChange = { colorArgb ->
                            viewModel.saveProvinceLight(
                                provinceLight?.copy(isLit = true, fillColorArgb = colorArgb)
                                    ?: ProvinceLight(
                                        provinceName = selectedProvince.name,
                                        isLit = true,
                                        fillColorArgb = colorArgb
                                    )
                            )
                        },
                        onBack = {
                            selectedProvinceName = null
                            selectedCityName = null
                        }
                    )
                }
            }
        }
        item {
            if (selectedProvince == null) {
                ProvinceMapCard(
                    provinceShapes = provinceShapes,
                    provinceLightMap = provinceLightMap,
                    litProvinceNames = litProvinceNames,
                    selectedProvinceName = selectedProvinceName,
                    onProvinceClick = { provinceName ->
                        selectedProvinceName = provinceName
                        selectedCityName = null
                    }
                )
            } else {
                val provinceIsLit = selectedProvince.name in litProvinceNames
                if (selectedCityName != null) {
                    val cityLight = cityLightMap[cityLightKey(selectedProvince.name, selectedCityName!!)]
                    if (cityLight?.isLit == true) {
                        CityLitPanel(
                            province = selectedProvince.name,
                            city = selectedCityName!!,
                            cityLight = cityLight,
                            onSaveCityNote = { note ->
                                viewModel.saveCityLight(cityLight.copy(note = note))
                            }
                        )
                    }
                } else if (provinceIsLit) {
                    if (selectedCityShapes.isNotEmpty()) {
                        CityBoundaryMapCard(
                            cityShapes = selectedCityShapes,
                            cityLightMap = cityLightMap,
                            provinceName = selectedProvince.name,
                            litCityNames = litCityNames,
                            selectedCityName = selectedCityName,
                            onCityClick = { cityName -> selectedCityName = cityName }
                        )
                    } else {
                        CityMapCard(
                            province = selectedProvince,
                            isProvinceLit = true,
                            litCityNames = litCityNames,
                            selectedCityName = selectedCityName,
                            onCityClick = { city -> selectedCityName = city.name },
                            onLightProvince = viewModel::lightProvince
                        )
                    }
                }
            }
        }
        item {
            if (selectedProvince != null && selectedCityName == null && selectedProvince.name in litProvinceNames) {
                ProvincePanel(
                    province = selectedProvince,
                    provinceLight = provinceLightMap[selectedProvince.name],
                    litCityCount = litCityNames.size,
                    onSaveProvinceNote = { note ->
                        val existing = provinceLightMap[selectedProvince.name]
                        viewModel.saveProvinceLight(
                            existing?.copy(note = note)
                                ?: ProvinceLight(
                                    provinceName = selectedProvince.name,
                                    note = note
                                )
                        )
                    }
                )
            }
        }
        selectedProvince?.let { province ->
            selectedCityName?.let { cityName ->
                val cityLight = cityLightMap[cityLightKey(province.name, cityName)]
                if (cityLight?.isLit == true) {
                    val existingMemory = cityMemories.firstOrNull {
                        it.provinceName == province.name && it.cityName == cityName
                    }
                    item {
                        CityMemoryEditor(
                            province = province.name,
                            city = cityName,
                            existingMemory = existingMemory,
                            onSave = viewModel::saveCityMemory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FootprintStatsHeader(
    provinceCount: Int,
    litProvinceCount: Int,
    memoryCount: Int,
    selectedProvince: String?,
    syncMessage: String?,
    onSyncClick: () -> Unit,
    onBackToChina: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBlock("点亮省份", "$litProvinceCount/$provinceCount")
                StatBlock("城市回忆", memoryCount.toString())
                StatBlock("完成度", "${provinceCount.takeIf { it > 0 }?.let { litProvinceCount * 100 / it } ?: 0}%")
                if (selectedProvince != null) {
                    OutlinedButton(onClick = onBackToChina) {
                        Text("全国")
                    }
                } else {
                    TextButton(onClick = onSyncClick) {
                        Text("同步")
                    }
                }
            }
            syncMessage?.let { message ->
                Text(
                    text = message,
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun LightSwitchPanel(
    title: String,
    isLit: Boolean,
    colorArgb: Int,
    backLabel: String,
    onLitChange: (Boolean) -> Unit,
    onColorChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    var showPalette by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLit) 68.dp else 520.dp)
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(backLabel)
        }
        Row(
            modifier = if (isLit) {
                Modifier.align(Alignment.TopStart).padding(top = 8.dp)
            } else {
                Modifier.align(Alignment.Center)
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Switch(
                checked = isLit,
                onCheckedChange = onLitChange
            )
            if (isLit) {
                IconButton(onClick = { showPalette = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_palette),
                        contentDescription = "选择填充颜色",
                        tint = Color(colorArgb)
                    )
                }
            }
        }
    }

    if (showPalette) {
        AlertDialog(
            onDismissRequest = { showPalette = false },
            title = { Text("选择填充颜色") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ProvinceColorChoices.forEach { choice ->
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    onColorChange(choice)
                                    showPalette = false
                                },
                            shape = CircleShape,
                            color = Color(choice),
                            border = BorderStroke(
                                width = if (choice == colorArgb) 3.dp else 1.dp,
                                color = if (choice == colorArgb) YanYeColors.Ink else YanYeColors.Line
                            )
                        ) {}
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPalette = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
private fun ImagePlaceholderCard(label: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFFF8FBF7)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProvinceMapCard(
    provinceShapes: List<ProvinceShape>,
    provinceLightMap: Map<String, ProvinceLight>,
    litProvinceNames: Set<String>,
    selectedProvinceName: String?,
    onProvinceClick: (String) -> Unit
) {
    val bounds = remember(provinceShapes) { provinceBounds(provinceShapes) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .background(Color(0xFFF8FBF7))
                .onSizeChanged { mapSize = it }
                .pointerInput(provinceShapes, scale, offset, mapSize) {
                    detectTapGestures { tap ->
                        if (mapSize.width == 0 || mapSize.height == 0) return@detectTapGestures
                        val local = Offset(
                            x = (tap.x - offset.x) / scale,
                            y = (tap.y - offset.y) / scale
                        )
                        val geo = unprojectMapPoint(
                            point = local,
                            size = Size(mapSize.width.toFloat(), mapSize.height.toFloat()),
                            bounds = bounds
                        )
                        val hit = provinceShapes.asReversed().firstOrNull { shape ->
                            pointInProvince(geo.lon, geo.lat, shape)
                        } ?: nearestShapeAtLocalPoint(
                            local = local,
                            shapes = provinceShapes,
                            size = Size(mapSize.width.toFloat(), mapSize.height.toFloat()),
                            bounds = bounds,
                            scale = scale
                        )
                        if (hit != null) onProvinceClick(hit.name)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.7f, 6f)
                        offset += pan
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                provinceShapes.forEach { shape ->
                    val lit = shape.name in litProvinceNames
                    val selected = shape.name == selectedProvinceName
                    val fillColor = when {
                        selected -> YanYeColors.Rose
                        lit -> Color(provinceLightMap[shape.name]?.fillColorArgb ?: DefaultProvinceColorArgb)
                        else -> Color(0xFFE8EDE8)
                    }
                    shape.polygons.forEach { ring ->
                        val path = ring.toMapPath(size, bounds)
                        drawPath(path = path, color = fillColor)
                        drawPath(
                            path = path,
                            color = if (selected) Color(0xFF8C3142) else Color.White,
                            style = Stroke(width = if (selected) 2.2f else 1.1f)
                        )
                    }
                }
                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.rgb(72, 68, 61)
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11f
                    isFakeBoldText = true
                }
                provinceShapes.forEach { shape ->
                    val point = projectMapPoint(
                        lon = shape.centerLon,
                        lat = shape.centerLat,
                        size = size,
                        bounds = bounds
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        shape.name,
                        point.x,
                        point.y,
                        textPaint
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapZoomButton(text = "+") {
                    scale = (scale + 0.3f).coerceAtMost(6f)
                }
                MapZoomButton(text = "-") {
                    scale = (scale - 0.3f).coerceAtLeast(0.7f)
                }
            }
        }
    }
}

@Composable
private fun CityBoundaryMapCard(
    cityShapes: List<ProvinceShape>,
    cityLightMap: Map<String, CityLight>,
    provinceName: String,
    litCityNames: Set<String>,
    selectedCityName: String?,
    onCityClick: (String) -> Unit
) {
    val bounds = remember(cityShapes) { provinceBounds(cityShapes) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .background(Color(0xFFF8FBF7))
                .onSizeChanged { mapSize = it }
                .pointerInput(cityShapes, scale, offset, mapSize) {
                    detectTapGestures { tap ->
                        if (mapSize.width == 0 || mapSize.height == 0) return@detectTapGestures
                        val local = Offset(
                            x = (tap.x - offset.x) / scale,
                            y = (tap.y - offset.y) / scale
                        )
                        val geo = unprojectMapPoint(
                            point = local,
                            size = Size(mapSize.width.toFloat(), mapSize.height.toFloat()),
                            bounds = bounds
                        )
                        val hit = cityShapes.asReversed().firstOrNull { shape ->
                            pointInProvince(geo.lon, geo.lat, shape)
                        } ?: nearestShapeAtLocalPoint(
                            local = local,
                            shapes = cityShapes,
                            size = Size(mapSize.width.toFloat(), mapSize.height.toFloat()),
                            bounds = bounds,
                            scale = scale
                        )
                        if (hit != null) onCityClick(hit.name)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.7f, 6f)
                        offset += pan
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                cityShapes.forEach { shape ->
                    val lit = shape.name in litCityNames
                    val selected = shape.name == selectedCityName
                    val fillColor = when {
                        selected -> YanYeColors.Rose
                        lit -> Color(cityLightMap[cityLightKey(provinceName, shape.name)]?.fillColorArgb ?: DefaultProvinceColorArgb)
                        else -> Color(0xFFE8EDE8)
                    }
                    shape.polygons.forEach { ring ->
                        val path = ring.toMapPath(size, bounds)
                        drawPath(path = path, color = fillColor)
                        drawPath(
                            path = path,
                            color = if (selected) Color(0xFF8C3142) else Color.White,
                            style = Stroke(width = if (selected) 2.2f else 1.1f)
                        )
                    }
                }
                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.rgb(72, 68, 61)
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11f
                    isFakeBoldText = true
                }
                cityShapes.forEach { shape ->
                    val point = projectMapPoint(
                        lon = shape.centerLon,
                        lat = shape.centerLat,
                        size = size,
                        bounds = bounds
                    )
                    drawContext.canvas.nativeCanvas.drawText(shape.name, point.x, point.y, textPaint)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapZoomButton(text = "+") {
                    scale = (scale + 0.3f).coerceAtMost(6f)
                }
                MapZoomButton(text = "-") {
                    scale = (scale - 0.3f).coerceAtLeast(0.7f)
                }
            }
        }
    }
}

private data class MapProjection(
    val originX: Float,
    val originY: Float,
    val scale: Float
)

private fun projectionFor(
    size: Size,
    bounds: GeoBounds
): MapProjection {
    val padding = 18f
    val lonSpan = (bounds.maxLon - bounds.minLon).toFloat().coerceAtLeast(1f)
    val latSpan = (bounds.maxLat - bounds.minLat).toFloat().coerceAtLeast(1f)
    val scale = minOf(
        (size.width - padding * 2) / lonSpan,
        (size.height - padding * 2) / latSpan
    ).coerceAtLeast(1f)
    val mapWidth = lonSpan * scale
    val mapHeight = latSpan * scale
    return MapProjection(
        originX = (size.width - mapWidth) / 2f,
        originY = (size.height - mapHeight) / 2f,
        scale = scale
    )
}

private fun List<GeoPoint>.toMapPath(
    size: Size,
    bounds: GeoBounds
): Path {
    val path = Path()
    forEachIndexed { index, point ->
        val projected = projectMapPoint(point.lon, point.lat, size, bounds)
        if (index == 0) {
            path.moveTo(projected.x, projected.y)
        } else {
            path.lineTo(projected.x, projected.y)
        }
    }
    path.close()
    return path
}

private fun projectMapPoint(
    lon: Double,
    lat: Double,
    size: Size,
    bounds: GeoBounds
): Offset {
    val projection = projectionFor(size, bounds)
    return Offset(
        x = projection.originX + ((lon - bounds.minLon).toFloat() * projection.scale),
        y = projection.originY + ((bounds.maxLat - lat).toFloat() * projection.scale)
    )
}

private fun unprojectMapPoint(
    point: Offset,
    size: Size,
    bounds: GeoBounds
): GeoPoint {
    val projection = projectionFor(size, bounds)
    return GeoPoint(
        lon = bounds.minLon + (point.x - projection.originX) / projection.scale,
        lat = bounds.maxLat - (point.y - projection.originY) / projection.scale
    )
}

private fun nearestShapeAtLocalPoint(
    local: Offset,
    shapes: List<ProvinceShape>,
    size: Size,
    bounds: GeoBounds,
    scale: Float
): ProvinceShape? {
    val threshold = (52f / scale).coerceIn(12f, 58f)
    return shapes
        .map { shape ->
            val center = projectMapPoint(
                lon = shape.centerLon,
                lat = shape.centerLat,
                size = size,
                bounds = bounds
            )
            shape to (center - local).getDistance()
        }
        .filter { (_, distance) -> distance <= threshold }
        .minByOrNull { (_, distance) -> distance }
        ?.first
}

@Composable
private fun CityMapCard(
    province: MapProvince,
    isProvinceLit: Boolean,
    litCityNames: Set<String>,
    selectedCityName: String?,
    onCityClick: (MapCity) -> Unit,
    onLightProvince: (String) -> Unit
) {
    if (!isProvinceLit) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${province.name}还没点亮",
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "先点亮省份，再进入城市级回忆。",
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { onLightProvince(province.name) }) {
                    Text("点亮${province.name}")
                }
            }
        }
        return
    }

    ZoomableMapFrame {
        province.cities.forEach { city ->
            MapNode(
                label = city.name,
                x = city.x,
                y = city.y,
                lit = city.name in litCityNames,
                selected = city.name == selectedCityName,
                onClick = { onCityClick(city) }
            )
        }
    }
}

@Composable
private fun ZoomableMapFrame(
    content: @Composable BoxWithConstraintsScopeMarker.() -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .background(Color(0xFFF8FBF7))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.85f, 2.8f)
                        offset += pan
                    }
                }
        ) {
            val frameWidth = maxWidth
            val frameHeight = maxHeight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                BoxWithConstraintsScopeMarker(
                    maxWidth = frameWidth,
                    maxHeight = frameHeight
                ).content()
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapZoomButton(text = "+") {
                    scale = (scale + 0.18f).coerceAtMost(2.8f)
                }
                MapZoomButton(text = "-") {
                    scale = (scale - 0.18f).coerceAtLeast(0.85f)
                }
            }
        }
    }
}

private data class BoxWithConstraintsScopeMarker(
    val maxWidth: androidx.compose.ui.unit.Dp,
    val maxHeight: androidx.compose.ui.unit.Dp
)

@Composable
private fun BoxWithConstraintsScopeMarker.MapNode(
    label: String,
    x: Float,
    y: Float,
    lit: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        selected -> YanYeColors.Rose
        lit -> Color(0xFFFFD36E)
        else -> Color.White
    }
    val contentColor = when {
        selected -> Color.White
        lit -> Color(0xFF61410A)
        else -> YanYeColors.Ink
    }
    Surface(
        modifier = Modifier
            .offset(
                x = maxWidth * x - 34.dp,
                y = maxHeight * y - 18.dp
            )
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = background,
        contentColor = contentColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (lit || selected) Color(0xFFD79A24) else YanYeColors.Line
        ),
        shadowElevation = if (lit || selected) 4.dp else 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MapZoomButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, YanYeColors.Line),
        shadowElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProvinceOverview(
    provinceCount: Int,
    litProvinceCount: Int,
    memoryCount: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatBlock("点亮省份", "$litProvinceCount/$provinceCount")
            StatBlock("城市回忆", memoryCount.toString())
            StatBlock("完成度", "${provinceCount.takeIf { it > 0 }?.let { litProvinceCount * 100 / it } ?: 0}%")
        }
    }
}

@Composable
private fun StatBlock(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(text = label, color = YanYeColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProvincePanel(
    province: MapProvince,
    provinceLight: ProvinceLight?,
    litCityCount: Int,
    onSaveProvinceNote: (String) -> Unit
) {
    var note by remember(province.name, provinceLight?.note) { mutableStateOf(provinceLight?.note.orEmpty()) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = province.name,
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$litCityCount 个城市点亮",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("省份备注") }
            )
            Button(
                onClick = { onSaveProvinceNote(note.trim()) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("保存省份备注")
            }
        }
    }
}

@Composable
private fun CityLitPanel(
    province: String,
    city: String,
    cityLight: CityLight,
    onSaveCityNote: (String) -> Unit
) {
    var note by remember(province, city, cityLight.note) { mutableStateOf(cityLight.note) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = city,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("城市备注") }
            )
            Button(
                onClick = { onSaveCityNote(note.trim()) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("保存城市备注")
            }
        }
    }
}

@Composable
private fun CityMemoryEditor(
    province: String,
    city: String,
    existingMemory: CityMemory?,
    onSave: (CityMemory) -> Unit
) {
    var dateText by remember(city, existingMemory?.id) {
        mutableStateOf(
            existingMemory?.let { LocalDate.ofEpochDay(it.dateEpochDay).format(DateTimeFormatter.ISO_LOCAL_DATE) }
                ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }
    var title by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.title ?: "${city}回忆") }
    var foods by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.foods.orEmpty()) }
    var places by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.places.orEmpty()) }
    var photoUris by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.photoUris.orEmpty()) }
    var scheduleIdText by remember(city, existingMemory?.id) {
        mutableStateOf(existingMemory?.linkedScheduleId?.toString().orEmpty())
    }
    var joke by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.insideJoke.orEmpty()) }
    var expenseText by remember(city, existingMemory?.id) {
        mutableStateOf(existingMemory?.expenseCents?.let { (it / 100).toString() }.orEmpty())
    }
    var pitfall by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.pitfallNotes.orEmpty()) }
    var note by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.note.orEmpty()) }
    var rating by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.rating?.takeIf { it > 0 } ?: 4) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "点亮 $city",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("标题") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it.take(10) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("日期") }
                )
                OutlinedTextField(
                    value = expenseText,
                    onValueChange = { expenseText = it.filter { char -> char.isDigit() }.take(7) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("花费") },
                    suffix = { Text("元") }
                )
            }
            OutlinedTextField(
                value = foods,
                onValueChange = { foods = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("吃了什么") }
            )
            OutlinedTextField(
                value = places,
                onValueChange = { places = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("玩了什么") }
            )
            OutlinedTextField(
                value = photoUris,
                onValueChange = { photoUris = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("照片 URI") }
            )
            OutlinedTextField(
                value = scheduleIdText,
                onValueChange = { scheduleIdText = it.filter(Char::isDigit).take(12) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("关联日程 ID") }
            )
            OutlinedTextField(
                value = joke,
                onValueChange = { joke = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("梗") }
            )
            OutlinedTextField(
                value = pitfall,
                onValueChange = { pitfall = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("避坑提示") }
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("复盘") }
            )
            RecordRating(
                rating = rating,
                onRatingChange = { rating = it }
            )
            Button(
                onClick = {
                    val date = runCatching { LocalDate.parse(dateText) }.getOrDefault(LocalDate.now())
                    val memory = existingMemory?.copy(
                        title = title.trim().ifBlank { "${city}回忆" },
                        dateEpochDay = date.toEpochDay(),
                        foods = foods.trim(),
                        places = places.trim(),
                        photoUris = photoUris.trim(),
                        linkedScheduleId = scheduleIdText.toLongOrNull(),
                        insideJoke = joke.trim(),
                        expenseCents = expenseText.toIntOrNull()?.times(100),
                        pitfallNotes = pitfall.trim(),
                        rating = rating,
                        note = note.trim()
                    ) ?: CityMemory(
                            provinceName = province,
                            cityName = city,
                            title = title.trim().ifBlank { "${city}回忆" },
                            dateEpochDay = date.toEpochDay(),
                            foods = foods.trim(),
                            places = places.trim(),
                            photoUris = photoUris.trim(),
                            linkedScheduleId = scheduleIdText.toLongOrNull(),
                            insideJoke = joke.trim(),
                            expenseCents = expenseText.toIntOrNull()?.times(100),
                            pitfallNotes = pitfall.trim(),
                            rating = rating,
                            note = note.trim()
                        )
                    onSave(memory)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("保存城市回忆")
            }
        }
    }
}

@Composable
private fun RecordRating(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "评分",
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { score ->
                FilterChip(
                    selected = rating == score,
                    onClick = { onRatingChange(score) },
                    label = { Text("$score") }
                )
            }
        }
    }
}

@Composable
private fun CityMemoryCard(
    memory: CityMemory,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = memory.title,
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = LocalDate.ofEpochDay(memory.dateEpochDay).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = onDelete) {
                    Text("删除", color = YanYeColors.Rose)
                }
            }
            MemoryLine("吃过", memory.foods)
            MemoryLine("玩过", memory.places)
            MemoryLine("照片", memory.photoUris)
            MemoryLine("日程", memory.linkedScheduleId?.toString().orEmpty())
            MemoryLine("梗", memory.insideJoke)
            MemoryLine("花费", memory.expenseCents?.let { "${it / 100} 元" }.orEmpty())
            MemoryLine("避坑", memory.pitfallNotes)
            MemoryLine("复盘", memory.note)
            Text(
                text = "评分 ${memory.rating}/5",
                color = Color(0xFFD79A24),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MemoryLine(
    label: String,
    value: String
) {
    if (value.isBlank()) return
    Text(
        text = "$label：$value",
        color = YanYeColors.Ink,
        style = MaterialTheme.typography.bodyMedium
    )
}
