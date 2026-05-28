package com.yanye.home.ui.footprint

data class MapProvince(
    val name: String,
    val x: Float,
    val y: Float,
    val cities: List<MapCity>
)

data class MapCity(
    val name: String,
    val x: Float,
    val y: Float
)

val ChinaProvinceMap: List<MapProvince> = listOf(
    MapProvince("新疆", 0.16f, 0.28f, cities("乌鲁木齐", "喀什", "伊犁", "吐鲁番")),
    MapProvince("西藏", 0.20f, 0.58f, cities("拉萨", "林芝", "日喀则")),
    MapProvince("青海", 0.35f, 0.47f, cities("西宁", "海西", "玉树")),
    MapProvince("甘肃", 0.43f, 0.38f, cities("兰州", "敦煌", "张掖", "天水")),
    MapProvince("宁夏", 0.52f, 0.39f, cities("银川", "中卫")),
    MapProvince("内蒙古", 0.56f, 0.24f, cities("呼和浩特", "包头", "呼伦贝尔", "赤峰")),
    MapProvince("黑龙江", 0.78f, 0.13f, cities("哈尔滨", "齐齐哈尔", "牡丹江")),
    MapProvince("吉林", 0.78f, 0.23f, cities("长春", "延边", "吉林")),
    MapProvince("辽宁", 0.74f, 0.31f, cities("沈阳", "大连", "丹东")),
    MapProvince("北京", 0.66f, 0.34f, cities("北京")),
    MapProvince("天津", 0.69f, 0.37f, cities("天津")),
    MapProvince("河北", 0.64f, 0.41f, cities("石家庄", "秦皇岛", "承德", "保定")),
    MapProvince("山西", 0.58f, 0.43f, cities("太原", "大同", "平遥")),
    MapProvince("陕西", 0.52f, 0.50f, cities("西安", "延安", "汉中")),
    MapProvince("山东", 0.68f, 0.47f, cities("济南", "青岛", "烟台", "威海")),
    MapProvince("河南", 0.60f, 0.53f, cities("郑州", "洛阳", "开封")),
    MapProvince("江苏", 0.72f, 0.57f, cities("南京", "苏州", "无锡", "扬州")),
    MapProvince("上海", 0.78f, 0.62f, cities("上海")),
    MapProvince("安徽", 0.67f, 0.61f, cities("合肥", "黄山", "芜湖")),
    MapProvince("湖北", 0.59f, 0.63f, cities("武汉", "宜昌", "襄阳")),
    MapProvince("重庆", 0.49f, 0.65f, cities("重庆")),
    MapProvince("四川", 0.41f, 0.65f, cities("成都", "乐山", "阿坝", "都江堰")),
    MapProvince("云南", 0.38f, 0.82f, cities("昆明", "大理", "丽江", "西双版纳")),
    MapProvince("贵州", 0.50f, 0.77f, cities("贵阳", "遵义", "黔东南")),
    MapProvince("湖南", 0.59f, 0.73f, cities("长沙", "张家界", "岳阳")),
    MapProvince("江西", 0.67f, 0.72f, cities("南昌", "景德镇", "九江")),
    MapProvince("浙江", 0.76f, 0.69f, cities("杭州", "宁波", "舟山")),
    MapProvince("福建", 0.72f, 0.81f, cities("福州", "厦门", "泉州")),
    MapProvince("广东", 0.60f, 0.88f, cities("广州", "深圳", "珠海", "汕头")),
    MapProvince("广西", 0.50f, 0.88f, cities("南宁", "桂林", "北海")),
    MapProvince("海南", 0.56f, 0.97f, cities("海口", "三亚")),
    MapProvince("香港", 0.65f, 0.93f, cities("香港")),
    MapProvince("澳门", 0.62f, 0.94f, cities("澳门")),
    MapProvince("台湾", 0.82f, 0.84f, cities("台北", "高雄", "花莲"))
)

private fun cities(vararg names: String): List<MapCity> =
    names.mapIndexed { index, name ->
        val column = index % 3
        val row = index / 3
        MapCity(
            name = name,
            x = 0.25f + column * 0.25f,
            y = 0.28f + row * 0.22f
        )
    }
