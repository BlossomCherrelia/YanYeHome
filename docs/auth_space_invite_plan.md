# YanYeHome 账号 / 情侣空间 / 邀请码方案

## 目标

把当前基于本地 `SyncSettings` 默认 `coupleId` 的临时同步方案，升级为更正式的产品结构：

1. 用户先注册 / 登录自己的账号。
2. 登录后再决定：
   - 创建情侣空间
   - 绑定已有情侣空间
3. 绑定已有空间时，不再手动共享 `coupleId`，改为使用邀请码。
4. App 中所有“共享同步”功能，都基于：
   - 当前登录用户
   - 当前绑定的情侣空间

这个方案的重点不是一次性把所有产品都做满，而是先把“账号、空间、邀请码、同步身份来源”这四件核心事理顺。

## 为什么要改

当前实现虽然能测试双机同步，但有明显问题：

- `localUserId` 是本地随机生成的，不是正式账号。
- `coupleId` 当前有默认固定值，不能作为正式产品机制。
- 没有“谁是谁”“谁属于哪个空间”的可靠关系模型。
- 没有邀请、绑定、退出空间等正式操作。
- “我的”页当前是技术设置入口，不是产品级关系入口。

所以后续如果继续做共享能力，必须先补账号和空间层。

## 产品流程

### 1. 启动流程

首次打开 App 时进入欢迎入口：

- `登录`
- `注册`

如果本地已有有效登录态，则直接进入主 App。

### 2. 注册流程

第一步：注册账号

- 用户名
- 密码
- 点击 `立即注册`

规则：

- 用户名允许中文。
- 用户名不要求全局唯一，是否唯一可后续决定；最小版本建议唯一，避免歧义。
- 密码最小长度建议 6 位。

注册成功后，此时：

- 已有用户账号
- 还没有情侣空间
- 进入“空间选择页”

### 3. 空间选择页

这个页面承接“注册完之后下一步做什么”。

分成两块：

#### A. 已有情侣空间？去绑定

- 输入邀请码
- 点击 `加入情侣空间`

#### B. 创建情侣空间

字段：

- `空间名`
  - 对应首页顶部标题，例如“妍叶之庭”
  - 对应“我的”页顶部关系卡中的空间标题
  - 可中文

- `空间标识 spaceCode`
  - 对应你之前说的“情侣空间id”
  - 长期存在
  - 用于唯一识别空间
  - 必须唯一
  - 只允许字母、数字、下划线

点击 `创建情侣空间` 后：

- 当前用户成为空间创建者
- 当前用户默认落在左侧位置
- 进入主 App

### 4. 登录后“我的”页顶部关系卡

不再显示现在的“情侣连接设置卡片”。

改成关系卡：

- 左侧圆形头像：当前空间左侧用户
- 右侧圆形头像：当前空间右侧用户
- 中间用曲线 / 连线表达“配对关系”

状态分四种：

#### 状态 A：未创建空间

- 提示去创建或绑定空间

#### 状态 B：已创建空间，未邀请

- 左边显示当前用户头像
- 右边显示白色空圆
- 白色空圆中显示 `去邀请`

#### 状态 C：已发邀请码，等待对方加入

- 左边显示当前用户头像
- 右边显示白色空圆
- 右边显示 `等待加入`
- 可再次生成邀请码

#### 状态 D：已完成配对

- 左右都显示头像
- 中间保持配对连接视觉

## 邀请码机制

邀请码不建议直接等于 `spaceCode`。

应该拆成两个概念：

### spaceCode

- 空间长期标识
- 创建空间时输入
- 唯一
- 主要用于系统内部和长期引用

### inviteCode

- 邀请别人加入时生成
- 短码
- 给对方手动输入或复制使用
- 建议可过期、可失效、可重新生成

这样比“直接把空间 ID 当邀请码”更符合产品逻辑，也更安全。

## 最小数据模型

### users

用户集合

字段建议：

- `userId`
- `username`
- `passwordHash`
- `avatarUrl`
- `currentSpaceId`
- `createdAt`
- `updatedAt`

说明：

- `avatarUrl` 可以为空
- 默认头像可以取用户名第一个字
- `currentSpaceId` 允许为空，表示该用户还没创建或加入空间

### couple_spaces

情侣空间集合

字段建议：

- `spaceId`
- `spaceName`
- `spaceCode`
- `ownerUserId`
- `leftUserId`
- `rightUserId`
- `status`
- `createdAt`
- `updatedAt`

说明：

- `spaceCode` 唯一
- `leftUserId` 默认创建者
- `rightUserId` 初期可为空
- `status` 可取：
  - `WAITING_PARTNER`
  - `PAIRED`

### space_invites

邀请码集合

字段建议：

- `inviteId`
- `inviteCode`
- `spaceId`
- `createdByUserId`
- `status`
- `expiresAt`
- `usedByUserId`
- `createdAt`

说明：

- `inviteCode` 唯一
- `status` 可取：
  - `ACTIVE`
  - `USED`
  - `EXPIRED`
  - `CANCELLED`

## 最小云函数

建议新增以下 CloudBase HTTP 云函数。

### 1. registerUser

职责：

- 注册账号
- 写入 `users`

输入：

- `username`
- `password`

输出：

- `userId`
- `username`
- 会话信息

### 2. loginUser

职责：

- 校验用户名密码
- 返回会话信息

输入：

- `username`
- `password`

输出：

- `userId`
- `username`
- `currentSpaceId`

### 3. createCoupleSpace

职责：

- 创建空间
- 校验 `spaceCode` 唯一
- 把创建者写入 `leftUserId`
- 更新该用户 `currentSpaceId`

输入：

- `userId`
- `spaceName`
- `spaceCode`

输出：

- `spaceId`
- `spaceName`
- `spaceCode`

### 4. createInviteCode

职责：

- 为当前空间生成邀请码
- 返回邀请码

输入：

- `userId`
- `spaceId`

输出：

- `inviteCode`
- `expiresAt`

### 5. joinCoupleSpaceByInvite

职责：

- 校验邀请码
- 校验该空间 `rightUserId` 是否为空
- 把当前用户写入 `rightUserId`
- 把邀请码标记为已使用
- 更新当前用户 `currentSpaceId`

输入：

- `userId`
- `inviteCode`

输出：

- `spaceId`
- `spaceName`
- `spaceCode`

### 6. getCurrentSessionProfile

职责：

- 拉取当前用户资料
- 拉取当前空间状态
- 拉取左/右两侧用户信息

输入：

- `userId`

输出：

- 用户资料
- 当前空间信息
- 配对状态

## Android 端本地模型建议

最小本地会话模型：

- `userId`
- `username`
- `avatarUrl`
- `currentSpaceId`
- `spaceName`
- `spaceCode`
- `isLoggedIn`

建议新建本地会话存储：

- `SessionSettings`

它会逐步替代当前 `SyncSettings` 中“身份来源”的职责。

## 同步身份迁移方案

当前同步服务拿身份的方式是：

- `localUserId`
- `coupleId`

后续要迁移成：

- `userId`
- `spaceId` 或 `spaceCode`

建议迁移顺序：

### 阶段 1

先保留 `SyncSettings`，但把来源改成“如果已登录则优先使用会话身份”。

也就是：

- `localUserId` -> 用真实 `userId`
- `coupleId` -> 用真实 `spaceId` 或 `spaceCode`

### 阶段 2

所有同步服务都不再依赖随机生成身份，只依赖登录会话。

### 阶段 3

移除默认固定 `coupleId = yanye-home-couple` 的逻辑。

## 页面结构建议

### 新增页面

- `AuthWelcomeScreen`
- `RegisterScreen`
- `LoginScreen`
- `SpaceSetupScreen`
- `JoinSpaceScreen`

### 修改页面

- `SettingsScreen`
  - 顶部改成关系卡
  - 加入邀请入口
  - 去掉当前临时“情侣连接设置卡”

- `HomeScreen`
  - 标题不再写死“妍叶之庭”
  - 从当前空间读取 `spaceName`

## 字段约束建议

### username

- 允许中文
- 建议长度 1-20

### password

- 建议长度 6-32

### spaceName

- 允许中文
- 建议长度 1-20

### spaceCode

- 只允许：字母 / 数字 / 下划线
- 建议长度：4-24
- 必须唯一

### inviteCode

- 系统自动生成
- 建议 6-8 位字母数字

## 最小 UI 优先级

第一阶段只做能跑通的版本，不追求最终视觉完整度。

### P1

- 注册
- 登录
- 创建空间
- 输入邀请码加入空间
- “我的”页关系卡显示
- 同步身份来源切换

### P2

- 头像上传
- 邀请码复制
- 邀请码过期提示
- 空间名编辑

### P3

- 更精致的配对动效
- 退出空间
- 更换另一半
- 邀请记录

## 推荐开发顺序

### Step 1

新增会话层：

- `SessionSettings`
- 本地登录态模型

### Step 2

新增云函数和集合：

- `users`
- `couple_spaces`
- `space_invites`

### Step 3

做注册 / 登录 / 空间选择页

### Step 4

把同步身份从随机本地值迁移成真实用户 + 空间

### Step 5

改“我的”页顶部关系卡和邀请入口

### Step 6

首页标题等位置接入动态 `spaceName`

## 本阶段建议

这次实现不要一次性把所有视觉都做满，先完成最小闭环：

1. 注册账号
2. 登录账号
3. 创建空间
4. 生成邀请码
5. 输入邀请码加入空间
6. 同步身份改为真实空间

只要这条线跑通，后面的配对视觉、头像上传、邀请码美化都可以继续叠加。
