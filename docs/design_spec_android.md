# 妍叶之庭 Android Jetpack Compose 设计规范

本文从 `docs/prototype.html` 高保真原型提取，用作后续 Android 前端实现的统一设计标准。除非产品或新原型明确更新，Compose 页面、组件、颜色、字号、间距应以本文为准。

## 1. 色板定义

### 1.1 品牌主色

| 用途 | Compose 命名建议 | 色值 | 使用场景 |
| --- | --- | --- | --- |
| 主色 | `Primary` | `#E8889A` | 选中态、主标签、日历选中、开关开启、重要图标 |
| 深主色 | `PrimaryDeep` | `#D4567C` | FAB、重要数字、主按钮渐变终点、强调标题 |
| 柔主色 | `PrimarySoft` | `#F2B5C0` | 渐变起伏、次级装饰、弱选中态、头像/快捷入口背景 |
| 浅主色 | `PrimaryLight` | `#FDE8EC` | 标签背景、分段控件未选中背景、浅粉按钮、状态胶囊背景 |
| 珊瑚强调 | `AccentCoral` | `#E76F7A` | 警示但不严重的强调、承诺/冲突修复类点缀 |
| 暖橙强调 | `AccentWarm` | `#F4A261` | 娱乐/食物/评分等暖色标签或辅助强调 |

### 1.2 背景与容器

| 用途 | Compose 命名建议 | 色值 | 使用场景 |
| --- | --- | --- | --- |
| 页面背景 | `Background` | `#FFF5F7` | 无壁纸页面的默认背景 |
| 浅输入背景 | `InputBackground` | `#FFF8F9` | 表单输入框、选择框 |
| 卡片背景 | `CardBackground` | `#FFFFFF` | 所有信息卡、列表卡、弹窗主体 |
| 页面渐变起点 | `BackgroundGradientStart` | `#FFF8FA` | 原型外层浅背景参考 |
| 页面渐变中点 | `BackgroundGradientMid` | `#FFF0F3` | 原型外层浅背景参考 |
| 页面渐变终点 | `BackgroundGradientEnd` | `#FFFBFC` | 原型外层浅背景参考 |
| 次级灰白背景 | `SurfaceSubtle` | `#FAFAFA` | 底部标签栏、弱容器 |
| 中性浅灰粉 | `NeutralSoft` | `#F5F0F1` | 日常标签、弱分类背景 |

### 1.3 文字颜色

| 用途 | Compose 命名建议 | 色值 | 使用场景 |
| --- | --- | --- | --- |
| 主要文字 | `TextMain` | `#3D2C2E` | 标题、正文、表单值 |
| 次级文字 | `TextSecondary` | `#7A5F63` | 标签名、辅助说明、次要正文 |
| 弱文字 | `TextMuted` | `#B89A9F` | 日期、空状态、导航未选中、右箭头 |
| 输入提示 | `TextHint` | `#D4B8BD` | placeholder、未填写值 |
| 反白文字 | `TextInverse` | `#FFFFFF` | 主按钮、深色遮罩上的文字 |

### 1.4 边框、分割线与遮罩

| 用途 | Compose 命名建议 | 色值 | 使用场景 |
| --- | --- | --- | --- |
| 主色边框 | `BorderPrimary` | `rgba(232,136,154,0.12)` | 输入框、粉色系卡片边界 |
| 轻分割线 | `DividerLight` | `rgba(0,0,0,0.04)` | 列表分割线、底部导航顶线 |
| 常规分割线 | `Divider` | `rgba(0,0,0,0.06)` | 登录分割线等更可见场景 |
| 遮罩 | `Overlay` | `rgba(61,44,46,0.5)` | 图片预览、弹窗背景 |
| 深遮罩 | `OverlayDark` | `rgba(0,0,0,0.6)` | 头像裁剪、沉浸式裁剪背景 |

### 1.5 状态色

| 用途 | Compose 命名建议 | 色值 | 使用场景 |
| --- | --- | --- | --- |
| 成功绿 | `Success` | `#4CAF50` | 已完成、健康状态 |
| 成功浅绿 | `SuccessLight` | `#E8F5E9` | 成功标签背景 |
| 较亮成功绿 | `SuccessBright` | `#66BB6A` | 承诺/完成类辅助图标 |
| 紫色 | `Purple` | `#9B59B6` | 排卵期、特殊状态 |
| 警告浅橙 | `WarningLight` | `#FFF3E8` | 娱乐/食物标签背景 |
| 暖浅橙 | `WarmLight` | `#FFF3E0` | 轻提示背景 |
| 开关关闭 | `SwitchOff` | `#E0D8D9` | 关闭态 Switch 轨道 |
| 禁用灰 | `Disabled` | `#E0E0E0` | 禁用组件 |
| 中性灰 | `NeutralGray` | `#757575` | 弱图标、备用文字 |

### 1.6 渐变定义

| Compose 命名建议 | 色值 | 使用场景 |
| --- | --- | --- |
| `PrimaryGradient` | `#E8889A` 到 `#D4567C`，135° | 主按钮、重要确认按钮、主头像 |
| `SoftPinkGradient` | `#FDE8EC` 到 `#F2B5C0`，135° | 快捷入口圆形图标、情侣空间入口图标 |
| `AvatarGradient` | `#F2B5C0` 到 `#E8889A`，135° | 默认头像 |
| `BrandGradient` | `#FDE8EC` 到 `#F2B5C0` 到 `#E8889A`，135° | 登录品牌图标、仪式感但轻量的品牌元素 |
| `WarmGradient` | `#F4A261` 到 `#E76F7A`，135° | 食物/娱乐/评分高亮 |
| `CareGradient` | `#F2B5C0` 到 `#E8889A` 到 `#D4567C`，135° | 关怀、经期、头像裁剪示意 |

### 1.7 使用原则

- 主界面以 `Background` / 壁纸 + `CardBackground` 为底，粉色只做状态、选中、按钮和小面积强调。
- 正文一律优先使用 `TextMain`，说明和日期使用 `TextMuted`，不要用纯黑。
- 卡片边框只在需要区分层级时使用 `BorderPrimary` 或 `DividerLight`，不要使用深边框。
- 暗色模式暂不定义；当前所有 token 均为亮色模式。

## 2. 字体系统

### 2.1 字体族

| 平台 | 字体族建议 |
| --- | --- |
| Android 默认 | 系统 sans-serif |
| 中文优先 | `Noto Sans SC` / 系统中文字体 |
| 设计参考 | `PingFang SC`, `SF Pro Display` |

Compose 实现时使用系统默认字体即可，保持中文在 Android 上清晰稳定。不要引入装饰字体。

### 2.2 字号档位

| Token | 字号 | 字重 | 建议行高 | 使用场景 |
| --- | --- | --- | --- | --- |
| `DisplayNumber` | 36sp | 700 | 40sp | 首页在一起天数、纪念日大数字 |
| `TitleLarge` | 20sp | 600 | 28sp | 一级页面标题、登录页品牌名 |
| `TopBarTitle` | 18sp | 600 | 24sp | 二级页面居中标题、首页标题 |
| `TopBarIcon` | 20sp | 400 | 24sp | 返回箭头、简单文本图标 |
| `SectionTitle` | 16sp | 600 | 22sp | 卡片内较强标题、详情主标题 |
| `ButtonLarge` | 16sp | 500 | 22sp | 登录/注册主按钮 |
| `Body` | 14sp | 400 | 22sp | 正文、列表主文本、表单值 |
| `BodyMedium` | 14sp | 500 | 22sp | 列表标题、日程标题、可点击正文 |
| `BodySemibold` | 14sp | 600 | 22sp | 卡片标题、小模块标题 |
| `Label` | 12sp | 400 | 18sp | 表单 label、日期、说明 |
| `LabelMedium` | 12sp | 500 | 18sp | 状态栏、弱操作、较重要小字 |
| `ChipText` | 11sp | 500 | 16sp | 分类标签、状态 chip、底部导航文字 |
| `Caption` | 11sp | 400 | 16sp | 底部导航未选中文字、极弱辅助信息 |
| `Tiny` | 10sp | 500 | 14sp | 勾选符号、小徽标 |

### 2.3 字体使用规则

- 页面标题：顶级页优先 `TitleLarge`，二级页 TopBar 使用 `TopBarTitle`。
- 卡片标题：统一使用 `BodySemibold`，不要随意放大。
- 列表主文本：使用 `BodyMedium`；列表副文本使用 `Label` + `TextMuted`。
- 表单 label 可用 12sp 或 14sp：独立表单页倾向 12sp，详情字段倾向 14sp。
- 底部导航文字固定 11sp，选中态 500，未选中态 400。
- 行高默认取字号的约 1.45 到 1.6 倍；多行备注统一 `Body` + 22sp 行高。

## 3. 间距系统

### 3.1 基础间距档位

| Token | 值 | 使用场景 |
| --- | --- | --- |
| `Space2` | 2dp | Switch 圆点偏移、小状态点 |
| `Space3` | 3dp | 标签内竖向 padding、日历小圆点到底部距离 |
| `Space4` | 4dp | 日期与副文本距离、状态胶囊细间距 |
| `Space6` | 6dp | 表单 label 到输入框、快捷入口图标到文字 |
| `Space8` | 8dp | chip 间距、列表行内部间距、卡片标题到内容 |
| `Space10` | 10dp | 详情字段行上下 padding、列表次级间距 |
| `Space12` | 12dp | 卡片之间常用距离、状态行间距 |
| `Space14` | 14dp | 输入框水平 padding、卡片中等 padding |
| `Space16` | 16dp | 页面主要水平边距、卡片标准内边距 |
| `Space18` | 18dp | 表单项之间较宽间距 |
| `Space20` | 20dp | 顶部标题水平边距、底部导航 FAB 右边距 |
| `Space24` | 24dp | 登录页分组间距、头像下方间距 |
| `Space28` | 28dp | 登录/注册页左右边距 |
| `Space32` | 32dp | 大块内容顶部间距 |

### 3.2 页面边距

| 场景 | 水平边距 | 垂直规则 |
| --- | --- | --- |
| 顶级页面标题区 | 20dp | 标题上方跟随系统 inset，标题下方 12dp |
| 常规内容区 | 16dp | 卡片间距 10dp 到 12dp |
| 登录/注册表单 | 28dp | 表单项间距 14dp 到 16dp |
| 二级页面 TopBar | 16dp 或 20dp | 高度固定 44dp |
| 底部导航 | 8dp | 高度 60dp |
| FAB | 右 20dp，底部距导航 20dp 左右 | 尺寸 52dp |

### 3.3 卡片内边距

| 卡片类型 | Padding |
| --- | --- |
| 标准信息卡 | 16dp |
| 紧凑列表卡 | 14dp 水平，14dp 垂直 |
| 日历卡 | 12dp 垂直，8dp 水平 |
| 设置列表分组 | 外层 4dp 垂直，16dp 水平；单行内部 14dp 到 16dp |
| 大入口卡 | 18dp 垂直，16dp 水平 |
| 备注/多行内容卡 | 16dp；正文行高 22dp |

### 3.4 组件尺寸与列表项高度

| 组件 | 尺寸 |
| --- | --- |
| 二级页面 TopBar | 44dp 高 |
| 主按钮 | 44dp 高 |
| 输入框/选择框 | 44dp 高 |
| 多行输入框 | 最小 80dp 高 |
| 底部导航 | 60dp 高 |
| FAB | 52dp × 52dp |
| 快捷入口圆形图标 | 48dp × 48dp |
| 常规头像 | 28dp / 44dp / 56dp / 72dp / 80dp 按场景 |
| 日历日期格 | 36dp × 36dp |
| Switch | 44dp × 24dp，圆点 20dp |
| Checkbox | 16dp × 16dp，圆角 4dp |
| 设置列表单行 | 48dp 到 52dp 高 |
| 详情字段行 | 40dp 到 44dp 高，内部上下 10dp |
| 普通列表卡 | 最小 64dp，可随内容增长 |
| 主页模块卡 | 内容决定高度，最小约 92dp |

## 4. 形状系统

| 类型 | Compose 命名建议 | 圆角 | 使用场景 |
| --- | --- | --- | --- |
| 小圆角 | `RadiusSmall` | 8dp | 输入框、普通 chip、标签 |
| 中圆角 | `RadiusMedium` | 12dp | 主按钮、小图标容器、胶囊按钮 |
| 卡片圆角 | `RadiusCard` | 14dp | 标准卡片、日历卡、列表卡 |
| 大圆角 | `RadiusLarge` | 20dp | 分段控件、较大胶囊、特殊卡片 |
| 超大圆角 | `RadiusXLarge` | 28dp | 大弹窗或强柔和容器 |
| 圆形 | `CircleShape` | 50% | FAB、头像、快捷入口、日历选中日期 |

### 4.1 组件形状规则

- 卡片统一 14dp 圆角，除非是图片入口卡或特殊沉浸卡。
- 主按钮 12dp 圆角，高度 44dp。
- 输入框 8dp 圆角，高度 44dp，多行输入框保持 8dp。
- 标签 chip 使用 8dp、12dp 或 20dp：小分类 8dp，状态胶囊 12dp，分段控件 20dp。
- FAB 固定圆形，52dp。
- Checkbox 使用 4dp 小圆角，不做完全圆形。

## 5. 阴影 / Elevation

原型主要使用轻阴影，视觉应保持平、浅、干净。

| Token | CSS 参考 | Compose 建议 | 使用场景 |
| --- | --- | --- | --- |
| `ElevationNone` | 无 | 0dp | 页面背景、纯文本行 |
| `ElevationCard` | `0 2px 8px rgba(212,86,124,0.06)` | 1dp 到 2dp | 标准卡片、列表卡 |
| `ElevationCardMedium` | `0 4px 16px rgba(212,86,124,0.08)` | 3dp 到 4dp | FAB、重要卡、悬浮元素 |
| `ElevationDialog` | `0 8px 32px rgba(212,86,124,0.12)` | 8dp | 弹窗、图片预览容器 |
| `ElevationStrong` | `0 4px 16px rgba(212,86,124,0.20)` | 4dp 到 6dp | 登录主按钮、需要更强反馈的确认按钮 |

### 5.1 阴影规则

- 标准卡片默认 `ElevationCard`，不要叠加厚重边框。
- FAB 使用 `ElevationCardMedium`。
- 弹窗使用 `ElevationDialog`，背景加 `Overlay`。
- 主按钮可以使用渐变 + 轻阴影，但普通按钮不要都加阴影。

## 6. 通用组件清单

### 6.1 页面与导航组件

| 组件 | 职责 | 参数接口设想 |
| --- | --- | --- |
| `YanYeScreenScaffold` | 统一页面背景、系统栏、安全区、底部导航空间。 | `selectedTab`, `useWallpaper`, `showBottomBar`, `floatingAction`, `content` |
| `YanYeTopBar` | 二级页顶部栏，左返回、中标题、右操作。 | `title`, `onBack`, `rightText`, `rightIcon`, `onRightClick` |
| `YanYePageHeader` | 顶级页左上标题区。 | `title`, `subtitle`, `actions`, `contentPadding` |
| `YanYeBottomNavBar` | 五个主 tab 底部导航。 | `items`, `selected`, `onSelect` |
| `YanYeFloatingActionButton` | 统一圆形 FAB。 | `icon`, `contentDescription`, `onClick`, `enabled` |

### 6.2 容器与卡片

| 组件 | 职责 | 参数接口设想 |
| --- | --- | --- |
| `YanYeCard` | 标准白底圆角轻阴影卡片。 | `modifier`, `padding`, `onClick`, `border`, `content` |
| `YanYeListCard` | 列表项卡片，支持标题、副标题、尾部标签或箭头。 | `title`, `subtitle`, `leading`, `trailing`, `onClick` |
| `YanYeSectionCard` | 带小标题的模块卡片。 | `title`, `titleIcon`, `action`, `content` |
| `YanYeEntryCard` | 空间/功能入口卡，支持背景图、标题、数值、副标题。 | `title`, `subtitle`, `value`, `background`, `onClick` |
| `YanYeDetailCard` | 详情页字段集合卡。 | `rows`, `showDividers`, `content` |
| `YanYeEmptyStateCard` | 轻量空状态容器。 | `text`, `icon`, `actionText`, `onAction` |

### 6.3 文本、标签与状态

| 组件 | 职责 | 参数接口设想 |
| --- | --- | --- |
| `YanYeChip` | 分类、状态、可见范围等小标签。 | `text`, `selected`, `colors`, `size`, `onClick` |
| `YanYeSegmentedTabs` | 日程/纪念日、待办分类等分段切换。 | `items`, `selectedIndex`, `onSelect` |
| `YanYeStatusPill` | 天气、心情、状态等首页小胶囊。 | `text`, `icon`, `colorStyle`, `onClick` |
| `YanYeDateText` | 统一日期格式和弱文字样式。 | `date`, `pattern`, `relativeMode` |
| `YanYeBadge` | 小数量、完成、更多图片提示。 | `text`, `colorStyle` |

### 6.4 表单组件

| 组件 | 职责 | 参数接口设想 |
| --- | --- | --- |
| `YanYeTextField` | 单行输入框，统一背景、边框、字号。 | `label`, `value`, `placeholder`, `onValueChange`, `trailingIcon`, `enabled` |
| `YanYeMultilineTextField` | 多行内容输入，最小 80dp。 | `label`, `value`, `placeholder`, `minLines`, `maxLines`, `onValueChange` |
| `YanYeSelectField` | 点击选择日期、时间、分类、性别等。 | `label`, `value`, `placeholder`, `trailingIcon`, `onClick` |
| `YanYeSwitchRow` | 设置页和表单开关行。 | `title`, `subtitle`, `checked`, `onCheckedChange` |
| `YanYeFormSection` | 表单项分组，统一外边距和项间距。 | `title`, `content` |
| `YanYePrimaryButton` | 主操作按钮，粉色渐变。 | `text`, `icon`, `enabled`, `loading`, `onClick` |
| `YanYeSecondaryButton` | 次级浅粉按钮。 | `text`, `icon`, `enabled`, `onClick` |

### 6.5 列表与设置组件

| 组件 | 职责 | 参数接口设想 |
| --- | --- | --- |
| `YanYeSettingsGroup` | 设置页白底分组卡片。 | `items`, `content` |
| `YanYeSettingsRow` | 设置单行，支持副值、箭头、开关。 | `title`, `subtitle`, `value`, `leading`, `trailing`, `onClick` |
| `YanYeSwipeDeleteRow` | 备忘录等列表的右滑删除交互。 | `content`, `deleteText`, `onDelete` |
| `YanYeChecklistRow` | 待办勾选行。 | `text`, `subtitle`, `checked`, `onCheckedChange`, `onClick` |
| `YanYeTimelineItem` | 回忆列表时间线条目。 | `date`, `title`, `location`, `note`, `photos`, `onClick` |

### 6.6 日历与日期组件

| 组件 | 职责 | 参数接口设想 |
| --- | --- | --- |
| `YanYeCalendarMonth` | 月历网格，统一 36dp 日期格和状态点。 | `month`, `selectedDate`, `markers`, `onDateClick` |
| `YanYeCalendarDayCell` | 单个日期格，支持选中、今天、弱化、事件点。 | `date`, `state`, `hasMarker`, `onClick` |
| `YanYeDatePickerSheet` | 自定义日期选择器，支持年/月/日切换。 | `initialDate`, `onConfirm`, `onDismiss` |
| `YanYeWheelPicker` | iOS 风格滚轮选择器。 | `items`, `selected`, `onSelect`, `visibleCount` |

### 6.7 图片与媒体组件

| 组件 | 职责 | 参数接口设想 |
| --- | --- | --- |
| `YanYeImagePickerField` | 单图选择、预览、删除。 | `label`, `imageUri`, `onPick`, `onRemove` |
| `YanYeMultiImagePickerField` | 最多 9 张图的网格选择和删除模式。 | `imageUris`, `maxCount`, `onAdd`, `onRemove`, `onPreview` |
| `YanYePhotoGrid` | 回忆列表/详情多图展示。 | `imageUris`, `onImageClick`, `maxPreviewCount` |
| `YanYeImagePreviewDialog` | 图片预览弹窗，支持横向滑动。 | `imageUris`, `initialIndex`, `onDismiss` |
| `YanYeAvatar` | 头像展示，支持默认渐变、图片、编辑角标。 | `imageUri`, `name`, `size`, `editable`, `onClick` |

### 6.8 业务通用组件

| 组件 | 职责 | 参数接口设想 |
| --- | --- | --- |
| `YanYeQuickActionGrid` | 首页快捷入口横排。 | `actions`, `columns`, `onActionClick` |
| `YanYeScheduleCard` | 日程列表/首页日程展示。 | `title`, `time`, `location`, `category`, `onClick` |
| `YanYeAnniversaryCard` | 纪念日卡片，统一日期和天数文案。 | `name`, `date`, `dayText`, `onClick` |
| `YanYeWishCard` | 愿望列表卡片，显示标题、预算/分类、状态。 | `title`, `category`, `budget`, `status`, `onClick` |
| `YanYeMemoryCard` | 回忆列表卡片，显示时间线信息、内容和图片。 | `memory`, `onClick`, `onImageClick` |
| `YanYeTodoCard` | 备忘录/首页待办模块。 | `items`, `onItemClick`, `onCheckedChange` |
| `YanYePeriodCalendar` | 关怀页经期日历，显示真实/预测/排卵状态。 | `month`, `records`, `selectedDate`, `onDateClick` |
| `YanYeFoodWheel` | 吃什么转盘主体。 | `items`, `mode`, `rotation`, `onSpin`, `onResult` |
| `YanYeMapCard` | 点亮地图容器，统一地图卡片边距和圆角。 | `mapState`, `onRegionClick`, `controls` |

## 7. 落地约束

- 新页面必须优先复用以上组件，不应在单个页面里重新定义一套卡片、按钮、输入框样式。
- 所有新增颜色必须先补充到本文色板；页面内不得随意硬编码相近粉色。
- 表单、详情、列表三类页面必须遵守 44dp 控件高度、14dp 卡片圆角、16dp 内容边距。
- UI 文案保持直接可用，避免教程式说明和占位解释。
- 卡片高度允许随内容增长，不为对齐强行截断正文；但列表主信息应保持清晰、紧凑。
- 原型中的 emoji 可作为视觉参考，Android 实现优先使用项目既有图标体系或 lucide/Material 图标，避免不同系统 emoji 风格造成不一致。
