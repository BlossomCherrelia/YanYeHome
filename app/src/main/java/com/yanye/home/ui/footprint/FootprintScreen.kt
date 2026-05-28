package com.yanye.home.ui.footprint

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yanye.home.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.domain.model.CityLight
import com.yanye.home.domain.model.CityMemory
import com.yanye.home.domain.model.ProvinceLight
import com.yanye.home.ui.common.AutoSyncLifecycleEffect
import com.yanye.home.ui.common.ImagePickerField
import com.yanye.home.ui.common.ImageUploadState
import com.yanye.home.ui.common.ImagePreviewBox
import com.yanye.home.ui.common.isLocalOnlyImageUri
import com.yanye.home.ui.common.PageChrome
import com.yanye.home.ui.common.WallpaperBackground
import com.yanye.home.ui.theme.YanYeColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val DefaultProvinceColorArgb = -34150
private val MapCardBackground = Color(0xFFFFF4F6)
private val MapUnlitRegion = Color(0xFFFFE1E7)
private fun cityLightKey(
    provinceName: String,
    cityName: String
): String = "$provinceName|$cityName"

@Composable
fun FootprintScreen(
    viewModel: FootprintViewModel = viewModel(),
    onBack: () -> Unit = {}
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
    var editingCityMemory by rememberSaveable(selectedProvinceName, selectedCityName) { mutableStateOf(false) }
    var editingCityMemoryId by rememberSaveable(selectedProvinceName, selectedCityName) { mutableStateOf<Long?>(null) }
    var showProvinceLightManager by rememberSaveable { mutableStateOf(false) }
    var showCityMemoryManager by rememberSaveable { mutableStateOf(false) }
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
    val handleBack: () -> Unit = {
        when {
            showProvinceLightManager -> showProvinceLightManager = false
            showCityMemoryManager -> showCityMemoryManager = false
            selectedCityName != null -> selectedCityName = null
            selectedProvinceName != null -> {
                selectedProvinceName = null
                selectedCityName = null
            }
            else -> onBack()
        }
    }

    BackHandler(enabled = showProvinceLightManager || showCityMemoryManager || selectedProvinceName != null || selectedCityName != null) {
        handleBack()
    }

    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PageChrome.secondaryPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val cityLight = selectedCityName?.let { cityName ->
                    selectedProvince?.let { province -> cityLightMap[cityLightKey(province.name, cityName)] }
                }
                val provinceLight = selectedProvince?.takeIf { selectedCityName == null }?.let { province ->
                    provinceLightMap[province.name]
                }
                val isLit = cityLight?.isLit == true || provinceLight?.isLit == true
                FootprintTopBar(
                    title = when {
                        showProvinceLightManager -> "点亮省份"
                        showCityMemoryManager -> "城市回忆"
                        else -> selectedCityName ?: selectedProvinceName ?: "点亮地图"
                    },
                    showLightControls = !showProvinceLightManager && !showCityMemoryManager && selectedProvince != null && isLit,
                    onBack = handleBack,
                    onToggleLight = {
                        when {
                            selectedCityName != null && selectedProvince != null -> {
                                viewModel.saveCityLight(
                                    cityLight?.copy(isLit = false)
                                        ?: CityLight(
                                            provinceName = selectedProvince.name,
                                            cityName = selectedCityName!!,
                                            isLit = false
                                        )
                                    )
                            }
                            selectedProvince != null -> {
                                viewModel.saveProvinceLight(
                                    provinceLight?.copy(isLit = false)
                                        ?: ProvinceLight(
                                            provinceName = selectedProvince.name,
                                            isLit = false
                                        )
                                    )
                            }
                        }
                    }
                )
            }
            if (showProvinceLightManager) {
                item {
                    ProvinceLightManagerPage(
                        provinces = ChinaProvinceMap,
                        provinceLightMap = provinceLightMap,
                        cityLightMap = cityLightMap,
                        onProvinceToggle = { province ->
                            val current = provinceLightMap[province.name]
                            val nextIsLit = current?.isLit != true
                            viewModel.saveProvinceLight(
                                current?.copy(isLit = nextIsLit)
                                    ?: ProvinceLight(
                                        provinceName = province.name,
                                        isLit = nextIsLit
                                    )
                            )
                        },
                        onCityToggle = { province, city ->
                            val key = cityLightKey(province.name, city.name)
                            val current = cityLightMap[key]
                            val nextIsLit = current?.isLit != true
                            viewModel.saveCityLight(
                                current?.copy(isLit = nextIsLit)
                                    ?: CityLight(
                                        provinceName = province.name,
                                        cityName = city.name,
                                        isLit = nextIsLit
                                    )
                            )
                        }
                    )
                }
            } else if (showCityMemoryManager) {
                item {
                    CityMemoryManagerPage(
                        provinces = ChinaProvinceMap,
                        memories = cityMemories,
                        provinceLightMap = provinceLightMap,
                        cityLightMap = cityLightMap,
                        onSaveMemory = viewModel::saveCityMemory,
                        onSaveProvinceLight = viewModel::saveProvinceLight,
                        onSaveCityLight = viewModel::saveCityLight
                    )
                }
            } else {
            item {
                when {
                    selectedProvince == null -> {
                        FootprintStatsHeader(
                            provinceCount = provinceCount,
                            litProvinceCount = litProvinceNames.size,
                            memoryCount = cityMemories.size,
                            selectedProvince = null,
                            syncMessage = syncMessage,
                            onBackToChina = {},
                            onOpenProvinceLightManager = { showProvinceLightManager = true },
                            onOpenCityMemoryManager = { showCityMemoryManager = true }
                        )
                    }
                    selectedCityName != null -> {
                        val cityLight = cityLightMap[cityLightKey(selectedProvince.name, selectedCityName!!)]
                        LightPromptPanel(
                            title = "点亮$selectedCityName",
                            isLit = cityLight?.isLit == true,
                            onLitChange = { isLit ->
                                viewModel.saveCityLight(
                                    cityLight?.copy(isLit = isLit)
                                        ?: CityLight(
                                            provinceName = selectedProvince.name,
                                            cityName = selectedCityName!!,
                                            isLit = isLit
                                        )
                                    )
                            }
                        )
                    }
                    else -> {
                        val provinceLight = provinceLightMap[selectedProvince.name]
                        LightPromptPanel(
                            title = "点亮${selectedProvince.name}",
                            isLit = provinceLight?.isLit == true,
                            onLitChange = { isLit ->
                                viewModel.saveProvinceLight(
                                    provinceLight?.copy(isLit = isLit)
                                        ?: ProvinceLight(
                                            provinceName = selectedProvince.name,
                                            isLit = isLit
                                        )
                                    )
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
                            val cityMemoryList = cityMemories
                                .filter {
                                it.provinceName == selectedProvince.name && it.cityName == selectedCityName
                                }
                                .sortedWith(
                                    compareBy<CityMemory> { it.sortOrder }
                                        .thenByDescending { it.dateEpochDay }
                                        .thenByDescending { it.updatedAt }
                                )
                            val editingMemory = editingCityMemoryId?.let { editingId ->
                                cityMemoryList.firstOrNull { it.id == editingId }
                            }
                            when {
                                editingCityMemory -> CityMemoryEditor(
                                    province = selectedProvince.name,
                                    city = selectedCityName!!,
                                    existingMemory = editingMemory,
                                    onSave = { memory ->
                                        viewModel.saveCityMemory(memory)
                                        editingCityMemory = false
                                        editingCityMemoryId = null
                                    }
                                )
                                cityMemoryList.isEmpty() -> AddCityMemoryCard(
                                    city = selectedCityName!!,
                                    onClick = {
                                        editingCityMemory = true
                                        editingCityMemoryId = null
                                    }
                                )
                                else -> CityMemoryListPage(
                                    province = selectedProvince.name,
                                    city = selectedCityName!!,
                                    memories = cityMemoryList,
                                    onAdd = {
                                        editingCityMemory = true
                                        editingCityMemoryId = null
                                    },
                                    onEdit = { memory ->
                                        editingCityMemory = true
                                        editingCityMemoryId = memory.id
                                    }
                                )
                            }
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
            }
        }
    }
}

@Composable
private fun CityMemoryManagerPage(
    provinces: List<MapProvince>,
    memories: List<CityMemory>,
    provinceLightMap: Map<String, ProvinceLight>,
    cityLightMap: Map<String, CityLight>,
    onSaveMemory: (CityMemory) -> Unit,
    onSaveProvinceLight: (ProvinceLight) -> Unit,
    onSaveCityLight: (CityLight) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedProvinceName by rememberSaveable { mutableStateOf("") }
    var selectedCityName by rememberSaveable { mutableStateOf("") }
    var activeCityKey by rememberSaveable { mutableStateOf<String?>(null) }
    var editingMemoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var provinceFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showProvinceFilter by rememberSaveable { mutableStateOf(false) }

    val cityGroups = memories
        .groupBy { cityLightKey(it.provinceName, it.cityName) }
        .mapNotNull { (_, cityMemories) ->
            val first = cityMemories.firstOrNull() ?: return@mapNotNull null
            CityMemoryGroup(
                provinceName = first.provinceName,
                cityName = first.cityName,
                memories = cityMemories.sortedWith(
                    compareBy<CityMemory> { it.sortOrder }
                        .thenByDescending { it.dateEpochDay }
                        .thenByDescending { it.updatedAt }
                )
            )
        }
        .sortedWith(compareBy<CityMemoryGroup> { it.provinceName }.thenBy { it.cityName })

    val activeGroup = activeCityKey?.let { key ->
        cityGroups.firstOrNull { cityLightKey(it.provinceName, it.cityName) == key }
    }
    val editingMemory = editingMemoryId?.let { id -> memories.firstOrNull { it.id == id } }

    when {
        isEditing -> {
            CityMemoryEditor(
                province = selectedProvinceName,
                city = selectedCityName,
                existingMemory = editingMemory,
                onSave = { memory ->
                    onSaveMemory(memory)
                    val provinceLight = provinceLightMap[memory.provinceName]
                    if (provinceLight?.isLit != true) {
                        onSaveProvinceLight(
                            provinceLight?.copy(isLit = true)
                                ?: ProvinceLight(
                                    provinceName = memory.provinceName,
                                    isLit = true
                                )
                        )
                    }
                    val memoryCityKey = cityLightKey(memory.provinceName, memory.cityName)
                    val cityLight = cityLightMap[memoryCityKey]
                    if (cityLight?.isLit != true) {
                        onSaveCityLight(
                            cityLight?.copy(isLit = true)
                                ?: CityLight(
                                    provinceName = memory.provinceName,
                                    cityName = memory.cityName,
                                    isLit = true
                                )
                        )
                    }
                    activeCityKey = memoryCityKey
                    selectedProvinceName = memory.provinceName
                    selectedCityName = memory.cityName
                    editingMemoryId = null
                    isEditing = false
                }
            )
        }
        activeGroup != null -> {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "‹ 返回城市回忆",
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { activeCityKey = null }
                )
                CityMemoryListPage(
                    province = activeGroup.provinceName,
                    city = activeGroup.cityName,
                    memories = activeGroup.memories,
                    onAdd = {
                        selectedProvinceName = activeGroup.provinceName
                        selectedCityName = activeGroup.cityName
                        editingMemoryId = null
                        isEditing = true
                    },
                    onEdit = { memory ->
                        selectedProvinceName = memory.provinceName
                        selectedCityName = memory.cityName
                        editingMemoryId = memory.id
                        isEditing = true
                    }
                )
            }
        }
        else -> {
            val keyword = query.trim()
            val filteredGroups = cityGroups.filter { group ->
                (provinceFilter == null || group.provinceName == provinceFilter) &&
                    (keyword.isBlank() ||
                    group.provinceName.contains(keyword, ignoreCase = true) ||
                    group.cityName.contains(keyword, ignoreCase = true) ||
                    group.memories.any { memory ->
                        memory.title.contains(keyword, ignoreCase = true) ||
                            memory.summary.contains(keyword, ignoreCase = true)
                    })
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CityMemoryQuickAddPanel(
                    provinces = provinces,
                    selectedProvinceName = selectedProvinceName,
                    selectedCityName = selectedCityName,
                    onProvinceSelected = { provinceName ->
                        selectedProvinceName = provinceName
                    },
                    onCitySelected = { cityName ->
                        selectedCityName = cityName
                    },
                    onAdd = {
                        editingMemoryId = null
                        isEditing = true
                    }
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    border = BorderStroke(0.6.dp, Color(0xFFFFDCE4)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CityMemorySearchHeader(
                            query = query,
                            onQueryChange = { query = it },
                            provinceFilter = provinceFilter,
                            showProvinceFilter = showProvinceFilter,
                            provinces = cityGroups.map { it.provinceName }.distinct(),
                            onToggleFilter = { showProvinceFilter = !showProvinceFilter },
                            onProvinceFilterSelected = { selected ->
                                provinceFilter = selected
                                showProvinceFilter = false
                            }
                        )
                        if (filteredGroups.isEmpty()) {
                            Text(
                                text = "还没有匹配的城市回忆",
                                color = YanYeColors.Muted,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            filteredGroups.forEach { group ->
                                CityMemoryGroupCard(
                                    group = group,
                                    onClick = { activeCityKey = cityLightKey(group.provinceName, group.cityName) },
                                    onAdd = {
                                        selectedProvinceName = group.provinceName
                                        selectedCityName = group.cityName
                                        editingMemoryId = null
                                        isEditing = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CityMemoryGroup(
    val provinceName: String,
    val cityName: String,
    val memories: List<CityMemory>
)

@Composable
private fun CityMemoryQuickAddPanel(
    provinces: List<MapProvince>,
    selectedProvinceName: String,
    selectedCityName: String,
    onProvinceSelected: (String) -> Unit,
    onCitySelected: (String) -> Unit,
    onAdd: () -> Unit
) {
    var cityQuery by rememberSaveable { mutableStateOf("") }
    val keyword = cityQuery.trim()
    val candidates = if (keyword.isBlank()) {
        emptyList()
    } else {
        provinces.flatMap { province ->
            val provinceMatches = province.name.contains(keyword, ignoreCase = true)
            province.cities
                .filter { city -> provinceMatches || city.name.contains(keyword, ignoreCase = true) }
                .map { city -> province to city }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            Text(
                text = "添加城市回忆",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            PinkSearchField(
                value = cityQuery,
                onValueChange = { cityQuery = it },
                placeholder = "搜索省份、城市（如：新疆  乌鲁木齐）"
            )
            if (candidates.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                    border = BorderStroke(0.8.dp, Color(0xFFFFBFD0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        candidates.take(8).forEach { (province, city) ->
                            CityCandidateRow(
                                provinceName = province.name,
                                cityName = city.name,
                                selected = province.name == selectedProvinceName && city.name == selectedCityName,
                                onClick = {
                                    onProvinceSelected(province.name)
                                    onCitySelected(city.name)
                                    cityQuery = "${city.name} · ${province.name}"
                                }
                            )
                        }
                    }
                }
            }
            if (selectedProvinceName.isNotBlank() && selectedCityName.isNotBlank()) {
                Text(
                    text = "已选择：$selectedCityName · $selectedProvinceName",
                    color = Color(0xFFFF7FA1),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedProvinceName.isNotBlank() && selectedCityName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF7FA1),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFFFEEF3),
                disabledContentColor = YanYeColors.Muted
            ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "＋ 添加回忆",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
    }
}

@Composable
private fun CityCandidateRow(
    provinceName: String,
    cityName: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) Color(0xFFFFEEF3) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$cityName · $provinceName",
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "⌖",
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun CityMemorySearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    provinceFilter: String?,
    showProvinceFilter: Boolean,
    provinces: List<String>,
    onToggleFilter: () -> Unit,
    onProvinceFilterSelected: (String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "已有回忆城市",
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PinkSearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "搜索已点亮城市 / 省份 / 回忆标题",
                modifier = Modifier.weight(1f),
                shadowElevation = 0.dp
            )
            Surface(
                modifier = Modifier
                    .height(44.dp)
                    .clickable(onClick = onToggleFilter),
                shape = RoundedCornerShape(22.dp),
                color = if (provinceFilter != null) Color(0xFFFFE3EB) else Color.White,
                border = BorderStroke(0.8.dp, Color(0xFFFFDCE4))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "筛选",
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
        if (showProvinceFilter) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                border = BorderStroke(0.8.dp, Color(0xFFFFDCE4)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                FlowRow(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MemoryChoiceChip(
                        text = "全部省份",
                        selected = provinceFilter == null,
                        onClick = { onProvinceFilterSelected(null) }
                    )
                    provinces.forEach { province ->
                        MemoryChoiceChip(
                            text = province,
                            selected = province == provinceFilter,
                            onClick = { onProvinceFilterSelected(province) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinkSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    shadowElevation: androidx.compose.ui.unit.Dp = 5.dp
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = YanYeColors.Ink,
            fontWeight = FontWeight.Medium
        ),
        modifier = modifier.height(44.dp),
        decorationBox = { innerTextField ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.92f),
                border = BorderStroke(0.7.dp, Color(0xFFFFE3EB)),
                shadowElevation = shadowElevation
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⌕",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = YanYeColors.Muted.copy(alpha = 0.78f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        innerTextField()
                    }
                }
            }
        }
    )
}

@Composable
private fun MemoryChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Color(0xFFFFE3EB) else Color.White,
        border = BorderStroke(
            width = if (selected) 1.2.dp else 0.6.dp,
            color = if (selected) Color(0xFFFF7FA1) else YanYeColors.Line
        )
    ) {
        Text(
            text = text,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CityMemoryGroupCard(
    group: CityMemoryGroup,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    val latest = group.memories.maxByOrNull { it.dateEpochDay }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        border = BorderStroke(0.6.dp, Color(0xFFFFDCE4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Color(0xFFFFEEF3), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💕",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.cityName,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${group.provinceName} · ${group.memories.size} 条回忆",
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                latest?.let { memory ->
                    Text(
                        text = "最近 ${LocalDate.ofEpochDay(memory.dateEpochDay).format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Text(
                text = "＋",
                color = Color(0xFFFF7FA1),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onAdd)
            )
        }
    }
}

@Composable
private fun ProvinceLightManagerPage(
    provinces: List<MapProvince>,
    provinceLightMap: Map<String, ProvinceLight>,
    cityLightMap: Map<String, CityLight>,
    onProvinceToggle: (MapProvince) -> Unit,
    onCityToggle: (MapProvince, MapCity) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedProvinceNames by remember { mutableStateOf(setOf<String>()) }
    val keyword = query.trim()
    val entries = provinces.mapNotNull { province ->
        val provinceMatches = keyword.isBlank() || province.name.contains(keyword, ignoreCase = true)
        val cityMatches = if (keyword.isBlank() || provinceMatches) {
            province.cities
        } else {
            province.cities.filter { city -> city.name.contains(keyword, ignoreCase = true) }
        }
        if (provinceMatches || cityMatches.isNotEmpty()) {
            province to cityMatches
        } else {
            null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PinkSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "搜索省份、城市（如：新疆  乌鲁木齐）"
        )
        entries.forEach { (province, matchedCities) ->
            val hasCityList = province.cities.size > 1
            val shouldExpand = hasCityList && (
                province.name in expandedProvinceNames ||
                    keyword.isNotBlank() && matchedCities.size < province.cities.size
                )
            ProvinceLightManagerProvinceCard(
                province = province,
                visibleCities = if (shouldExpand) matchedCities else emptyList(),
                isExpanded = shouldExpand,
                isProvinceLit = provinceLightMap[province.name]?.isLit == true,
                cityLightMap = cityLightMap,
                canExpand = hasCityList,
                onToggleExpanded = {
                    expandedProvinceNames = if (province.name in expandedProvinceNames) {
                        expandedProvinceNames - province.name
                    } else {
                        expandedProvinceNames + province.name
                    }
                },
                onProvinceToggle = { onProvinceToggle(province) },
                onCityToggle = { city -> onCityToggle(province, city) }
            )
        }
        if (entries.isEmpty()) {
            Text(
                text = "没有找到匹配的省份或城市",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ProvinceLightManagerProvinceCard(
    province: MapProvince,
    visibleCities: List<MapCity>,
    isExpanded: Boolean,
    isProvinceLit: Boolean,
    cityLightMap: Map<String, CityLight>,
    canExpand: Boolean,
    onToggleExpanded: () -> Unit,
    onProvinceToggle: () -> Unit,
    onCityToggle: (MapCity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        border = BorderStroke(0.6.dp, Color(0xFFFFDCE4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = canExpand, onClick = onToggleExpanded)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = province.name,
                            color = YanYeColors.Ink,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isProvinceLit) {
                            LitBadge()
                        }
                    }
                    Text(
                        text = if (canExpand) "${province.cities.size} 个城市" else "直辖/特别行政区",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                HeartLightSwitch(
                    isLit = isProvinceLit,
                    onClick = onProvinceToggle
                )
                if (canExpand) {
                    Text(
                        text = if (isExpanded) "∧" else "∨",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onToggleExpanded)
                    )
                }
            }
            if (visibleCities.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(Color.White.copy(alpha = 0.52f), RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    visibleCities.forEach { city ->
                        val isCityLit = cityLightMap[cityLightKey(province.name, city.name)]?.isLit == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = city.name,
                                    color = YanYeColors.Ink,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isCityLit) {
                                    LitBadge()
                                }
                            }
                            HeartLightSwitch(
                                isLit = isCityLit,
                                onClick = { onCityToggle(city) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LitBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFFFE4EC)
    ) {
        Text(
            text = "已点亮",
            color = Color(0xFFFF6F98),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun HeartLightSwitch(
    isLit: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(52.dp)
            .height(30.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isLit) Color(0xFFFF6F98) else Color(0xFFF4F4F4),
        shadowElevation = if (isLit) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = if (isLit) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Text(
                text = "♥",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LightBulbToggle(
    isLit: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (isLit) Color(0xFFFFF2B8) else Color.White,
        border = BorderStroke(
            width = 0.8.dp,
            color = if (isLit) Color(0xFFFFC83D) else YanYeColors.Line
        ),
        shadowElevation = if (isLit) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "💡",
                style = MaterialTheme.typography.titleMedium
            )
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
    onBackToChina: () -> Unit,
    onOpenProvinceLightManager: () -> Unit = {},
    onOpenCityMemoryManager: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatBlock(
            label = "点亮省份",
            value = "$litProvinceCount/$provinceCount",
            onClick = if (selectedProvince == null) onOpenProvinceLightManager else null,
            modifier = Modifier.weight(1f)
        )
        StatBlock(
            label = "城市回忆",
            value = memoryCount.toString(),
            onClick = if (selectedProvince == null) onOpenCityMemoryManager else null,
            modifier = Modifier.weight(1f)
        )
        StatBlock(
            label = "完成度",
            value = "${provinceCount.takeIf { it > 0 }?.let { litProvinceCount * 100 / it } ?: 0}%",
            modifier = Modifier.weight(1f)
        )
        if (selectedProvince != null) {
            OutlinedButton(onClick = onBackToChina) {
                Text("全国")
            }
        }
    }
}

@Composable
private fun FootprintTopBar(
    title: String,
    showLightControls: Boolean,
    onBack: () -> Unit,
    onToggleLight: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "‹",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clickable(onClick = onBack)
                    .padding(end = 14.dp)
            )
            Text(
                text = title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            if (showLightControls) {
                LightStatusActions(
                    onToggleLight = onToggleLight,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            } else {
                Spacer(modifier = Modifier.align(Alignment.TopEnd).width(1.dp))
            }
        }
        androidx.compose.material3.HorizontalDivider(
            color = YanYeColors.Line,
            thickness = 0.8.dp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun LightStatusActions(
    onToggleLight: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(38.dp)
                .clickable(onClick = onToggleLight),
            shape = CircleShape,
            color = Color(0xFFFFE3EB),
            border = BorderStroke(0.6.dp, YanYeColors.Line)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "♥",
                    color = Color(0xFFFF7FA1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LightPromptPanel(
    title: String,
    isLit: Boolean,
    onLitChange: (Boolean) -> Unit
) {
    if (isLit) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .width(260.dp)
                .height(62.dp)
                .clickable { onLitChange(true) },
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFFFF7FA1),
            shadowElevation = 8.dp
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "♥",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ImagePlaceholderCard(label: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MapCardBackground),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(YanYeColors.Paper),
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
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .background(MapCardBackground)
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
                        else -> MapUnlitRegion
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
        colors = CardDefaults.cardColors(containerColor = MapCardBackground),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .background(MapCardBackground)
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
                        else -> MapUnlitRegion
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
            border = BorderStroke(0.6.dp, YanYeColors.Line),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .background(YanYeColors.Paper)
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
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
    value: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val blockModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    Surface(
        modifier = blockModifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(0.6.dp, Color(0xFFFFDCE4))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text(
                text = value,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "省份备注",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${province.name} · $litCityCount 个城市点亮",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text("写一点关于这里的备注...") }
            )
            Button(
                onClick = { onSaveProvinceNote(note.trim()) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YanYeColors.Ink,
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "完成",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
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
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
private fun AddCityMemoryCard(
    city: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "添加城市回忆",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$city 已点亮，记录吃过的、玩过的和避坑提示。",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YanYeColors.Ink,
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "＋ 添加",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CityMemoryListPage(
    province: String,
    city: String,
    memories: List<CityMemory>,
    onAdd: () -> Unit,
    onEdit: (CityMemory) -> Unit
) {
    var selectedCoverUri by remember(memories) { mutableStateOf<String?>(null) }
    var showAlbum by remember { mutableStateOf(false) }
    val albumUris = memories.flatMap { it.allImageUris() }.distinct()
    val sections = CityMemoryType.entries
        .map { type -> type to memories.filter { memory -> memory.belongsTo(type) } }
        .filter { (_, items) -> items.isNotEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CityMemoryHeroCard(
            province = province,
            city = city,
            coverUri = selectedCoverUri ?: memories.firstNotNullOfOrNull { it.displayImageUri() },
            onOpenAlbum = { showAlbum = true }
        )
        Text(
            text = "我们的回忆 ${memories.size} 条",
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        sections.forEach { (type, items) ->
            CityMemoryTypeSection(
                type = type,
                memories = items,
                onEdit = onEdit
            )
        }
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF7FA1),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text(
                text = "＋ 添加回忆",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
    if (showAlbum) {
        CityMemoryAlbumDialog(
            imageUris = albumUris,
            onDismiss = { showAlbum = false },
            onAdd = {
                showAlbum = false
                onAdd()
            },
            onSetCover = { uri ->
                selectedCoverUri = uri
                showAlbum = false
            }
        )
    }
}

@Composable
private fun CityMemoryHeroCard(
    province: String,
    city: String,
    coverUri: String?,
    onOpenAlbum: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFFCAD6), Color(0xFFFFF0E5))
                    )
                )
                .padding(16.dp)
        ) {
            ImagePreviewBox(
                imageUri = coverUri,
                contentDescription = "$city 回忆封面",
                modifier = Modifier.fillMaxWidth(),
                minHeight = 150.dp,
                maxHeight = 190.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (coverUri.isNullOrBlank()) 34.dp else 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$province · $city",
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable(onClick = onOpenAlbum),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.72f),
                    border = BorderStroke(0.6.dp, Color(0xFFFFDCE4))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "▣",
                            color = Color(0xFFFF7FA1),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CityMemoryAlbumDialog(
    imageUris: List<String>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onSetCover: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("城市相册") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (imageUris.isEmpty()) {
                    Text(
                        text = "还没有图片，先添加一条带图片的回忆。",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    imageUris.take(6).forEach { uri ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ImagePreviewBox(
                                imageUri = uri,
                                contentDescription = "相册图片",
                                modifier = Modifier.size(72.dp),
                                minHeight = 72.dp,
                                maxHeight = 72.dp
                            )
                            Button(
                                onClick = { onSetCover(uri) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF7FA1),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("设为封面")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAdd) {
                Text("添加图片")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun CityMemoryTypeSection(
    type: CityMemoryType,
    memories: List<CityMemory>,
    onEdit: (CityMemory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${type.icon} ${type.title}",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${memories.size} ›",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        memories.forEach { memory ->
            CityMemoryListCard(
                memory = memory,
                type = type,
                onClick = { onEdit(memory) }
            )
        }
    }
}

@Composable
private fun CityMemoryListCard(
    memory: CityMemory,
    type: CityMemoryType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        border = BorderStroke(0.6.dp, Color(0xFFFFDCE4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(Color(0xFFFFEEF3), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                ImagePreviewBox(
                    imageUri = memory.displayImageUri(),
                    contentDescription = memory.title,
                    modifier = Modifier.fillMaxSize(),
                    minHeight = 92.dp,
                    maxHeight = 92.dp
                )
                if (memory.displayImageUri().isNullOrBlank()) {
                    Text(
                        text = type.icon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = memory.displayTitle(type),
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "⋯",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (memory.rating > 0) {
                    Text(
                        text = "⭐ ${memory.rating}.0",
                        color = Color(0xFFF5A623),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = memory.displaySummary(type),
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = LocalDate.ofEpochDay(memory.dateEpochDay).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private enum class CityMemoryType(
    val code: String,
    val title: String,
    val icon: String,
    val subtitle: String
) {
    Food("FOOD", "吃过的", "💮", "美食记忆"),
    Play("PLAY", "玩过的", "🎁", "游玩体验"),
    Place("PLACE", "去过的", "📍", "地点打卡"),
    Shopping("SHOPPING", "买过的", "🛍", "购物清单"),
    Stay("STAY", "住过的", "⛺", "住宿体验"),
    Moment("MOMENT", "我们的时刻", "💕", "特别瞬间")
}

private fun CityMemoryType.summaryPlaceholder(city: String): String =
    when (this) {
        CityMemoryType.Food -> "写下在 $city 吃到的味道..."
        CityMemoryType.Play -> "记录在 $city 玩过的地方和感受..."
        CityMemoryType.Place -> "记录这次地点打卡..."
        CityMemoryType.Shopping -> "记录买到的东西和小心情..."
        CityMemoryType.Stay -> "记录住过的地方和体验..."
        CityMemoryType.Moment -> "写下这个特别瞬间..."
    }

private fun CityMemoryType.locationLabel(): String =
    when (this) {
        CityMemoryType.Food -> "店名 / 地点"
        CityMemoryType.Play -> "景点 / 项目"
        CityMemoryType.Place -> "地点名称"
        CityMemoryType.Shopping -> "店铺 / 商场"
        CityMemoryType.Stay -> "住宿名称"
        CityMemoryType.Moment -> "发生地点"
    }

private fun CityMemory.belongsTo(type: CityMemoryType): Boolean {
    val normalized = memoryType.uppercase()
    val explicitType = CityMemoryType.entries.firstOrNull { it.code == normalized }
    if (explicitType != null) {
        return explicitType == type
    }

    return when (type) {
        CityMemoryType.Food -> foods.isNotBlank()
        CityMemoryType.Play -> places.isNotBlank()
        CityMemoryType.Place -> locationName.isNotBlank()
        CityMemoryType.Shopping -> priceText.isNotBlank() || expenseCents != null
        CityMemoryType.Stay -> false
        CityMemoryType.Moment -> summary.isNotBlank() || note.isNotBlank() || insideJoke.isNotBlank()
    }
}

private fun CityMemory.displayImageUri(): String? =
    coverImageUri.ifBlank { firstCityPhotoUri(photoUris).orEmpty() }.ifBlank { null }

private fun CityMemory.allImageUris(): List<String> =
    buildList {
        coverImageUri.trim().takeIf { it.isNotBlank() }?.let(::add)
        photoUris.split('\n', ',', ';')
            .map(String::trim)
            .filter { it.isNotBlank() }
            .forEach(::add)
    }

private fun CityMemory.displayTitle(type: CityMemoryType): String =
    title.ifBlank {
        when (type) {
            CityMemoryType.Food -> foods
            CityMemoryType.Play -> places
            CityMemoryType.Place -> locationName
            CityMemoryType.Shopping -> priceText
            CityMemoryType.Stay -> locationName
            CityMemoryType.Moment -> "我们的时刻"
        }.ifBlank { type.title }
    }

private fun CityMemory.displaySummary(type: CityMemoryType): String =
    summary.ifBlank {
        when (type) {
            CityMemoryType.Food -> foods
            CityMemoryType.Play -> places
            CityMemoryType.Place -> locationName
            CityMemoryType.Shopping -> priceText.ifBlank { expenseCents?.let { "${it / 100} 元" }.orEmpty() }
            CityMemoryType.Stay -> note
            CityMemoryType.Moment -> note.ifBlank { insideJoke }
        }
    }.ifBlank { "记录我们在这里的美好时光~" }

@Composable
private fun CityMemoryDetail(
    memory: CityMemory,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ImagePreviewBox(
            imageUri = firstCityPhotoUri(memory.photoUris),
            contentDescription = "城市照片",
            modifier = Modifier.fillMaxWidth(),
            maxHeight = 300.dp
        )
        CityMemorySection(title = "吃过的") {
            CityMemoryInfoCard(
                title = memory.foods.ifBlank { "null" },
                subtitle = "⭐${memory.rating}.0 · ${LocalDate.ofEpochDay(memory.dateEpochDay).format(DateTimeFormatter.ISO_LOCAL_DATE)}"
            )
        }
        CityMemorySection(title = "玩过的") {
            CityMemoryInfoCard(
                title = memory.places.ifBlank { "null" },
                subtitle = memory.insideJoke.ifBlank { "null" }
            )
        }
        CityMemorySection(title = "避坑") {
            CityMemoryInfoCard(
                title = memory.pitfallNotes.ifBlank { "null" },
                subtitle = memory.note.ifBlank { "null" }
            )
        }
        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = YanYeColors.Ink,
                contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "编辑回忆",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text("删除城市回忆", color = YanYeColors.Rose)
        }
    }
}

private fun firstCityPhotoUri(photoUris: String): String? =
    photoUris.split('\n', ',', ';').map(String::trim).firstOrNull { it.isNotBlank() }

@Composable
private fun CityMemorySection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun CityMemoryInfoCard(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
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
    var selectedType by remember(city, existingMemory?.id) {
        mutableStateOf(
            CityMemoryType.entries.firstOrNull { it.code == existingMemory?.memoryType?.uppercase() }
                ?: CityMemoryType.Moment
        )
    }
    var dateText by remember(city, existingMemory?.id) {
        mutableStateOf(
            existingMemory?.let { LocalDate.ofEpochDay(it.dateEpochDay).format(DateTimeFormatter.ISO_LOCAL_DATE) }
                ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }
    var title by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.title.orEmpty()) }
    var coverImageUri by remember(city, existingMemory?.id) {
        mutableStateOf(existingMemory?.coverImageUri?.ifBlank { firstCityPhotoUri(existingMemory.photoUris).orEmpty() }.orEmpty())
    }
    var summary by remember(city, existingMemory?.id) {
        mutableStateOf(existingMemory?.summary?.ifBlank { existingMemory.note }.orEmpty())
    }
    var locationName by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.locationName.orEmpty()) }
    var priceText by remember(city, existingMemory?.id) {
        mutableStateOf(
            existingMemory?.priceText?.ifBlank {
                existingMemory.expenseCents?.let { "${it / 100}" }.orEmpty()
            }.orEmpty()
        )
    }
    var note by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.note.orEmpty()) }
    var rating by remember(city, existingMemory?.id) { mutableStateOf(existingMemory?.rating?.takeIf { it > 0 } ?: 4) }
    var imageUploadState by remember { mutableStateOf(ImageUploadState()) }
    val hasPendingLocalImage = coverImageUri.isNotBlank() && isLocalOnlyImageUri(coverImageUri)
    val imageBlockingMessage = when {
        imageUploadState.isUploading -> "图片上传中，请稍后再保存"
        imageUploadState.errorMessage != null -> "图片上传失败，请重试或移除图片"
        hasPendingLocalImage -> "图片尚未变成云端地址，暂时不能保存"
        else -> null
    }
    val canSave = imageBlockingMessage == null

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CityMemoryEditorHero(city = city, isEditing = existingMemory != null)
        CityMemoryTypePicker(
            selectedType = selectedType,
            onTypeSelected = { selectedType = it }
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            border = BorderStroke(0.6.dp, Color(0xFFFFDCE4)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            )
            {
                Text(
                    text = selectedType.title,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("标题") },
                    placeholder = { Text("${city}${selectedType.title}") }
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it.take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("日期") }
                )
                ImagePickerField(
                    label = "封面照片",
                    imageUri = coverImageUri,
                    onImageUriChange = { coverImageUri = it },
                    height = 260.dp,
                    module = "footprints",
                    onUploadStateChange = { imageUploadState = it }
                )
                imageBlockingMessage?.let { message ->
                    Text(
                        text = message,
                        color = YanYeColors.Rose,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("回忆内容") },
                    placeholder = { Text(selectedType.summaryPlaceholder(city)) }
                )
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(selectedType.locationLabel()) }
                )
                if (selectedType == CityMemoryType.Shopping) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.take(20) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("花费 / 购物备注") }
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text("补充记录") }
                )
                RecordRating(
                    rating = rating,
                    onRatingChange = { rating = it }
                )
                Button(
                    onClick = {
                        val date = runCatching { LocalDate.parse(dateText) }.getOrDefault(LocalDate.now())
                        val cleanTitle = title.trim().ifBlank { "${city}${selectedType.title}" }
                        val cleanSummary = summary.trim()
                        val cleanLocation = locationName.trim()
                        val cleanPrice = priceText.trim()
                        val cleanNote = note.trim()
                        val memory = existingMemory?.copy(
                            title = cleanTitle,
                            memoryType = selectedType.code,
                            dateEpochDay = date.toEpochDay(),
                            coverImageUri = coverImageUri.trim(),
                            summary = cleanSummary,
                            locationName = cleanLocation,
                            priceText = cleanPrice,
                            foods = if (selectedType == CityMemoryType.Food) cleanSummary else existingMemory.foods,
                            places = if (selectedType == CityMemoryType.Play) cleanSummary else existingMemory.places,
                            photoUris = coverImageUri.trim(),
                            expenseCents = cleanPrice.filter(Char::isDigit).toIntOrNull()?.times(100),
                            rating = rating,
                            note = cleanNote
                        ) ?: CityMemory(
                            provinceName = province,
                            cityName = city,
                            title = cleanTitle,
                            memoryType = selectedType.code,
                            dateEpochDay = date.toEpochDay(),
                            coverImageUri = coverImageUri.trim(),
                            summary = cleanSummary,
                            locationName = cleanLocation,
                            priceText = cleanPrice,
                            foods = if (selectedType == CityMemoryType.Food) cleanSummary else "",
                            places = if (selectedType == CityMemoryType.Play) cleanSummary else "",
                            photoUris = coverImageUri.trim(),
                            expenseCents = cleanPrice.filter(Char::isDigit).toIntOrNull()?.times(100),
                            rating = rating,
                            note = cleanNote
                        )
                        onSave(memory)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF7FA1),
                        contentColor = Color.White,
                        disabledContainerColor = YanYeColors.Soft,
                        disabledContentColor = YanYeColors.Muted
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = if (existingMemory == null) "＋ 添加回忆" else "保存回忆",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CityMemoryEditorHero(
    city: String,
    isEditing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFFD3DF), Color(0xFFFFF1E9))
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isEditing) "编辑城市回忆" else "添加城市回忆",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "记录我们在 $city 的美好时光吧~",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CityMemoryTypePicker(
    selectedType: CityMemoryType,
    onTypeSelected: (CityMemoryType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "选择回忆类型",
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        CityMemoryType.entries.chunked(2).forEach { rowTypes ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowTypes.forEach { type ->
                    CityMemoryTypeOption(
                        type = type,
                        selected = type == selectedType,
                        onClick = { onTypeSelected(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowTypes.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CityMemoryTypeOption(
    type: CityMemoryType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(104.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFFFE3EB) else Color.White.copy(alpha = 0.86f)
        ),
        border = BorderStroke(
            width = if (selected) 1.2.dp else 0.6.dp,
            color = if (selected) Color(0xFFFF7FA1) else Color(0xFFFFDCE4)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = type.title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = type.subtitle,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = type.icon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.End)
            )
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
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
