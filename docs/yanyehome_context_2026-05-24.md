# YanYeHome Context Memory

Saved: 2026-05-24 Asia/Shanghai
Project root: `/Users/a86153/Desktop/YanYeHome`
Backup: `/Users/a86153/Desktop/YanYeHome_backup_20260523_2316`

## Current product/dev state

- CloudBase image upload is working.
- Normal memories now have end-to-end cloud sync support:
  - Android service: `CloudBaseMemorySyncService.kt`
  - Cloud function: `cloudfunctions/syncMemories/index.js`
  - zip exists: `cloudfunctions/syncMemories.zip`
- Image save protection is in place for:
  - normal memories
  - wishes
  - footprint city memories
- Rule now is:
  - image uploading: cannot save
  - upload failed: cannot save
  - local `content://` or `file://` must not be persisted into synced business records
- Current upload compression settings in `CloudBaseImageUploadService.kt`:
  - `MAX_IMAGE_DIMENSION = 1080`
  - `JPEG_QUALITY = 72`

## Wish list UI status

- User wanted homepage category area changed, not the editor page category area.
- Wish homepage category filter is now horizontal scroll.
- Wish editor category UI was restored to original style.
- Wish cards and text were resized several times and currently sit between the original oversized version and the overly compressed version.

## Auth and couple space status

- Phase 1 auth skeleton is in place:
  - `SessionSettings`
  - login/register basic screens
  - app startup gated by auth
  - settings page has account/logout section
- Phase 2 CloudBase auth/space flow is in place:
  - `registerUser`
  - `loginUser`
  - `createCoupleSpace`
  - `createInviteCode`
  - `joinCoupleSpaceByInvite`
  - `getCurrentSessionProfile`
- Relevant cloudfunction zip packages already exist under `cloudfunctions/`.
- `YY` marks on the login home page were already removed.

## Important identity logic

- `SyncSettings.identity()` now behaves like:
  - logged in + has `currentSpaceId` -> `coupleId = currentSpaceId`
  - logged in + no current space -> `coupleId = solo_<userId>`
  - no logged-in session -> fallback to old local default behavior
- This means cloud sync identity is already space-aware.

## Critical bug already fixed

- CloudBase nullable fields were sometimes returning the literal string `"null"`.
- That incorrectly made the app think the user already had a space.
- Fixes already landed in:
  - `SessionSettings.kt`
  - `CloudBaseAuthService.kt`

## Current known issue

User reported:

- creating a new space still shows old data
- new accounts appear to enter the previously used shared data view

Root cause identified:

- this is most likely not a failure of space creation
- local Room reads are still not scoped by `currentSpaceId/coupleId`
- many DAOs still read all rows from local tables, so old cached shared data bleeds into newly created spaces

Confirmed unscoped read points:

- `WishDao.observeWishes()`
- `ScheduleDao.observeSchedules()`
- `MemoryDao.observeMemories()`
- `MemoDao.observeMemos()`
- `AnniversaryDao.observeAnniversaries()`

Implication:

- even if login/session/cloud space creation is correct, UI can still show old data from local DB
- therefore seeing old data after creating a new space is currently expected from the app's local display layer

## What to tell the user next

- The new space is probably already created successfully if the session/cloud fields look correct.
- The visible old data is mainly a local cache isolation problem.
- Entering `spaceCode = yanye-home-couple` is not a guaranteed way to see old shared data under the new auth-space model, because old default sync `coupleId` and new `currentSpaceId` are not the same concept.
- The reason old data still appears right now is the missing local filtering, not that the new space failed.

## Recommended next implementation

Do local space isolation for shared modules first:

- wishes
- anniversaries
- schedules
- memories
- memos

Likely acceptable approach:

- add DAO queries filtered by `coupleId`
- make repositories observe current session space only
- optionally clear old shared local cache on account/space switch to avoid stale bleed-through

After that, user can properly test:

1. device A creates a new space
2. device A generates invite code
3. device B joins via invite code
4. both devices see only that space's data

## Build state

- Latest known Gradle build was successful after the auth/space and null handling fixes.
- Build command used successfully:
  - `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug`

## 2026-05-24 03:51 CST continuation update

### Settings/profile UI changes

- “我的”页 was redesigned several times.
- Current intended UI state:
  - Top header title remains `"我的"` using the same `PrimaryPageHeader` sizing/grey-divider style as main pages such as `"关怀"`.
  - Right side of the header has a small avatar button. The avatar size was reduced to avoid pushing the header/divider down.
  - Tapping the avatar opens a separate profile page, not a dialog.
  - Profile page route is `YanYeDestination.Profile` (`"profile"`).
  - Profile page composable is `ProfileEditorScreen` in `SettingsScreen.kt`.
  - Profile page uses app’s existing flat/white-card/secondary-top-bar style.
- Profile fields:
  - avatar
  - username
  - new password, optional; blank means unchanged
  - space code, disabled/read-only
  - space name
  - logout
- Avatar upload:
  - uses existing `ImagePickerField`
  - module is `"avatars"`
  - save is blocked while upload is in progress
  - save is blocked if avatar upload failed or remains local-only `content://`/`file://`
- Profile save:
  - Android service method: `CloudBaseAuthService.updateProfile(...)`
  - ViewModel method: `AuthViewModel.updateProfile(...)`
  - API constant: `CloudBaseConfig.UPDATE_SESSION_PROFILE_URL`
  - Cloud function added: `cloudfunctions/updateSessionProfile/`
  - zip exists: `cloudfunctions/updateSessionProfile.zip`
  - Function must be deployed as HTTP route `/updateSessionProfile`.
  - Online probe showed `/updateSessionProfile` returned `404 INVALID_PATH` before deployment, so profile save failed even though avatar upload worked.
  - Frontend was updated so save errors display on the profile page, save button shows `保存中...`, and successful save returns to `"我的"`.

### Matching card and invite card UI state

- Matching card current intended state:
  - no black card
  - light pink gradient background
  - no `"空间号"` row
  - no top-right `"待配对"` badge
  - no subtitle like `"1 和 等待加入"`
  - left and right large circular avatars with a pink horizontal curve between them
  - if not paired, show small `"等待匹配"` text under the curve
  - if paired, hide `"等待匹配"`
- Invite card current intended state:
  - compact card
  - first line large: `"空间邀请码："` + code
  - only the code/`"未生成"` part is deep pink; `"空间邀请码："` remains normal ink color
  - buttons order: left = generate/regenerate, right = copy invite code

### Home title

- Home title was changed from hard-coded `"妍叶之庭"` to `session.spaceName ?: "妍叶之庭"`.
- `HomeScreen` now receives `UserSession` from `YanYeNavGraph`.

### Partner avatar/session field

- `UserSession` now includes `partnerAvatarUrl`.
- `SessionSettings` persists `partnerAvatarUrl`.
- `CloudBaseAuthService.toUserSession()` parses `partnerAvatarUrl`.
- Some cloud functions were updated to return `partnerAvatarUrl` and zips were rebuilt:
  - `getCurrentSessionProfile.zip`
  - `loginUser.zip`
  - `joinCoupleSpaceByInvite.zip`
  - `createCoupleSpace.zip`

### Space isolation work and serious migration bug

- Entities already had `coupleId`.
- DAO/repository work was added so UI reads are scoped by current `SyncSettings.identity().coupleId`:
  - `WishDao.observeWishesForCouple(coupleId)`
  - `ScheduleDao.observeSchedulesForCouple(coupleId)`
  - `MemoryDao.observeMemoriesForCouple(coupleId)`
  - `MemoDao.observeMemosForCouple(coupleId)`
  - `AnniversaryDao.observeAnniversariesForCouple(coupleId)`
  - Repositories use these scoped observe methods.
- New-data writes were updated to fill missing `coupleId`/`ownerUserId` from `SyncSettings.identity()`.
- Pending sync queues were later tightened to current space only:
  - `WishDao.pendingSyncWishesForCouple(coupleId)`
  - `ScheduleDao.pendingSyncSchedulesForCouple(coupleId)`
  - `MemoryDao.pendingSyncMemoriesForCouple(coupleId)`
  - `MemoDao.pendingSyncMemosForCouple(coupleId)`
  - `AnniversaryDao.pendingSyncAnniversariesForCouple(coupleId)`
  - Repositories now use these scoped pending queries.
- Remote save methods now defensively ignore remote items whose `coupleId` differs from current identity:
  - `WishRepository.saveRemoteWish`
  - `ScheduleRepository.saveRemoteSchedule`
  - `ScheduleRepository.saveRemoteMemory`
  - `MemoRepository.saveRemoteMemo`
  - `AnniversaryRepository.saveRemoteAnniversary`
- Critical mistake:
  - An automatic `LocalSpaceMigrationService` was added to migrate existing local data to current space.
  - The original migration SQL condition was too broad: it rewrote rows with `coupleId IS NULL OR coupleId != currentSpace`.
  - This changed old test records from the original legacy `coupleId` into newly created `space_xxx` ids.
  - Because sync used `set()` with existing `remoteId`, cloud records were also overwritten to the new `space_xxx` `coupleId`.
  - User confirmed database records originally under `coupleId = "yanyehome"` were changed to e.g. `"space_a1c0130793670450"`.
  - This explains why brand-new spaces could still pull old data: the old data had been truly reassigned to those spaces in CloudBase, not merely displayed incorrectly.
- Fix after discovering the bug:
  - `LocalSpaceMigrationService.kt` was deleted.
  - Automatic migration call was removed from `YanYeHomeApp.MainAppShell`.
  - Do **not** re-add automatic data migration on startup.
  - `SyncSettings.DEFAULT_COUPLE_ID` was changed to `"yanyehome"` to match the actual legacy id the user saw.
  - DAO migration SQL was changed to only target `coupleId IS NULL OR coupleId = '' OR coupleId = 'yanyehome'`, but because automatic migration was removed, these methods are currently only dormant/manual helpers.

### Cloud data repair functions

- `clearSpaceBusinessData` was added earlier:
  - folder: `cloudfunctions/clearSpaceBusinessData/`
  - zip: `cloudfunctions/clearSpaceBusinessData.zip`
  - deletes business records for a given `spaceId` from:
    - `wishes`
    - `schedules`
    - `memories`
    - `memos`
    - `anniversaries`
  - This is destructive and should not be used for records that should be restored to `yanyehome`.
- `restoreMovedLegacyData` was added after user noticed records were reassigned:
  - folder: `cloudfunctions/restoreMovedLegacyData/`
  - zip: `cloudfunctions/restoreMovedLegacyData.zip`
  - deploy as HTTP route `/restoreMovedLegacyData`
  - It changes records from a bad `fromCoupleId` back to `targetCoupleId`, intended target: `"yanyehome"`.
  - It supports `dryRun`; default is dry run unless `dryRun: false`.
  - Example dry run body:
    ```json
    {
      "userId": "current space member userId",
      "fromCoupleId": "space_a1c0130793670450",
      "targetCoupleId": "yanyehome",
      "dryRun": true
    }
    ```
  - Then run with `"dryRun": false` only if counts are reasonable.
  - Reason for dryRun: batch cloud `coupleId` repair affects multiple collections and can corrupt data if parameters are wrong.
  - Function verifies `userId` is a member of `fromCoupleId` if `fromCoupleId` starts with `"space_"`.

### Current recommended next steps

1. Deploy latest Android build with automatic migration removed and scoped pending sync queries.
2. Deploy `restoreMovedLegacyData` if repairing already corrupted CloudBase data.
3. For each mistakenly polluted `space_xxx`, call `restoreMovedLegacyData` dryRun first:
   - `fromCoupleId = polluted space id`
   - `targetCoupleId = "yanyehome"`
4. If dryRun counts are expected, call again with `dryRun: false`.
5. Clear/reinstall app data on test devices if local Room has already been polluted with wrong `coupleId` values.
6. Create a brand-new space after installing the fixed app; it should not receive old data.

### Important caution for future work

- Do not implement broad automatic local migration from old `coupleId` to the currently logged-in space.
- If preserving old legacy data is required, make migration explicit and user-confirmed, ideally selecting source and target `coupleId`.
- Never update all rows with `coupleId != currentSpace`; that reassigns legitimate data from other spaces.
- Any repair function that mutates cloud records should support dryRun first.

## 2026-05-24 continuation update: remaining local isolation hardening

- After reading this memory, the codebase was checked under `/Users/a86153/Desktop/YanYeHome`.
- The project directory is not a git repository, so changes were tracked by file inspection and build verification.
- Additional local space isolation was added beyond the earlier main shared modules:
  - restaurants
  - footprint province lights
  - footprint city lights
  - footprint city memories
  - care cycles
- DAO additions:
  - `RestaurantDao.observeRestaurantsForCouple(coupleId)`
  - `RestaurantDao.pendingSyncRestaurantsForCouple(coupleId)`
  - `FootprintDao.observeProvinceLightsForCouple(coupleId)`
  - `FootprintDao.observeCityLightsForCouple(coupleId)`
  - `FootprintDao.observeCityMemoriesForCouple(coupleId)`
  - `FootprintDao.pendingSyncProvinceLightsForCouple(coupleId)`
  - `FootprintDao.pendingSyncCityLightsForCouple(coupleId)`
  - `FootprintDao.pendingSyncCityMemoriesForCouple(coupleId)`
  - `MemoDao.observeCareCyclesForCouple(coupleId)`
  - `MemoDao.pendingSyncCareCyclesForCouple(coupleId)`
- Repository changes:
  - `RestaurantRepository` now receives `SyncSettings`, observes/pushes only current `coupleId`, fills missing `coupleId`/`ownerUserId` on local saves, and rejects remote restaurants from other spaces.
  - `FootprintRepository` now receives `SyncSettings`, observes/pushes only current `coupleId`, fills missing identity fields on local saves, and rejects remote footprint data from other spaces.
  - `MemoRepository` now scopes care cycles the same way memos are scoped.
- Database/schema changes:
  - Room database version bumped from `21` to `22`.
  - Added migration `MIGRATION_21_22`.
  - Footprint unique indexes were changed from globally unique location keys to per-space unique keys:
    - `province_lights`: `(coupleId, provinceName)`
    - `city_lights`: `(coupleId, provinceName, cityName)`
- Verification:
  - `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug`
  - Build succeeded.

## 2026-05-24 continuation update: full isolation audit and overwrite guard

- User explicitly asked to re-check end to end that new spaces do not show old data and that old `coupleId` values cannot be overwritten by a new `coupleId`.
- Audit scope:
  - local DAO reads
  - pending-sync upload queries
  - local save defaults
  - remote download-to-local merge
  - cloud sync writes
  - repair/clear cloud functions
  - automatic migration/bulk `coupleId` updates
- Serious risk found and fixed:
  - `SyncRules.remoteId(prefix, localId)` generated IDs such as `wish_1`, `schedule_1`, etc.
  - Footprint/care-cycle deterministic IDs also omitted `coupleId`.
  - Because CloudBase sync functions write with `collection.doc(remoteId).set(payload)`, two spaces could write the same document id and overwrite its `coupleId`.
  - Fixed by changing generated remote IDs to include `identity.coupleId` and `identity.localUserId`:
    - `SyncRules.remoteId(prefix, identity, localId)`
    - updated wishes, schedules, memories, anniversaries, restaurants, memos, care cycles, and footprints.
- Cloud overwrite guard added:
  - All HTTP sync cloud functions now call `assertRemoteIdWritable(remoteId, coupleId)` before `set()`.
  - If an existing cloud document has a different `coupleId`, the function throws `REMOTE_ID_COUPLE_CONFLICT:<remoteId>` and refuses to overwrite it.
  - Updated functions:
    - `syncWishes`
    - `syncSchedules`
    - `syncMemories`
    - `syncAnniversaries`
    - `syncRestaurants`
    - `syncMemos`
    - `syncCareCycles`
    - `syncFootprints`
- Local remote merge guard added:
  - Remote download save paths now find existing local rows by `remoteId + current coupleId`, not `remoteId` alone.
  - This prevents a remote item in one space from updating a hidden local row cached under another space.
- Removed dormant migration danger:
  - Repository `migrateActiveDataToCurrentCouple()` methods were removed.
  - DAO `migrateActiveDataToCouple(...)` SQL methods were removed.
  - This further reduces the chance of accidentally reintroducing automatic broad local `coupleId` rewrites.
- Additional isolation fixes:
  - Default relationship anniversary check is now per current `coupleId`.
  - Care cycle “clear all” now clears only current `coupleId`.
- Repair/clear functions broadened:
  - `restoreMovedLegacyData` and `clearSpaceBusinessData` now include:
    - `restaurants`
    - `footprints`
    - `care_cycles`
  - Both zips were rebuilt.
- Verification:
  - Android build succeeded:
    - `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug`
  - `node --check` passed for all changed sync/repair/clear cloud functions.
  - Rebuilt zips:
    - `syncWishes.zip`
    - `syncSchedules.zip`
    - `syncMemories.zip`
    - `syncAnniversaries.zip`
    - `syncRestaurants.zip`
    - `syncMemos.zip`
    - `syncCareCycles.zip`
    - `syncFootprints.zip`
    - `restoreMovedLegacyData.zip`
    - `clearSpaceBusinessData.zip`

## 2026-05-24 continuation update: debug install, repair run, and UI work

### Android debug package

- User installed the latest debug APK successfully with:
  ```bash
  adb install -r /Users/a86153/Desktop/YanYeHome/app/build/outputs/apk/debug/app-debug.apk
  ```
- Install output was `Success`.
- Reinstall command remains:
  ```bash
  adb install -r /Users/a86153/Desktop/YanYeHome/app/build/outputs/apk/debug/app-debug.apk
  ```

### Cloud repair run

- User called deployed HTTP function `/restoreMovedLegacyData`.
- Correct `userId` was:
  - `user_3910e5e27a3dff05`
- Polluted source space:
  - `space_a1c0130793670450`
- Legacy target:
  - `yanyehome`
- Dry run with correct user succeeded and reported:
  - wishes scanned 5, updated 0
  - schedules scanned 7, updated 0
  - memories scanned 12, updated 0
  - memos scanned 2, updated 0
  - anniversaries scanned 4, updated 0
  - restaurants/footprints/care_cycles scanned 0
- Actual run with `dryRun: false` succeeded and updated:
  - wishes 5
  - schedules 7
  - memories 12
  - memos 2
  - anniversaries 4
  - restaurants/footprints/care_cycles 0
- This restored records from `space_a1c0130793670450` back to `yanyehome`.
- User understood that after this, re-entering `space_a1c0130793670450` should be isolated/new, and additional created spaces should also be isolated from each other.
- User decided not to keep trying to view old `yanyehome` data through the new space-code flow.

### Space id / space code discussion

- User observed that user-entered `spaceId`/space code is not the real sync isolation key.
- Real data isolation key is the generated `currentSpaceId`, e.g. `space_a1c0130793670450`.
- Space code / display space id is mainly user-facing invite/lookup metadata, not the `coupleId` used by synced business collections.
- We did not remove the user-facing field.
- Copy changes requested:
  - Replace visible labels such as “情侣空间 ID” and “空间号” with “空间id”.

### Home / Today UI changes

- Bottom navigation label “首页” was changed to “今天”.
- Home/Today page was heavily redesigned to match user prototype while preserving:
  - top title sizing/proportion
  - bottom navigation bar
  - app font family
- User repeatedly tuned proportions:
  - cards were initially too large, then too small, finally adjusted between those states
  - line heights and row spacing were increased
  - Today card borders/lines were made lighter
- “她的状态” card:
  - compact prototype-like card
  - height later increased by about 10%
  - text “还有一天来姨妈哦” removed
- Anniversary card:
  - “在一起 x 天” size reduced after being too large
  - anniversary rows changed so all rows use the same style/layout as the relationship day row
  - removed separate grey “已经 x 天” style
  - icon moved to the front
- Schedule card:
  - removed repeated “今日日程” label inside each schedule card/entry
  - title and time separated into two lines where needed
  - card text reduced so title is smaller than the page title
  - if schedule title wraps, time aligns with the first line
  - dot bullet added in home schedule rows
  - dot radius increased after user noted it was too small
  - schedule content no longer bold
- Date/time editor UI:
  - schedule editing should use time picker UI instead of manual time text input
  - if other screens have time picking, prefer the memo editor time picker style.
- Quick action row:
  - eating/food text size checked against schedule content size
  - quick action icons later replaced with custom PNG icons.
- Memo and Today Memory:
  - memo content font increased
  - “那年今天” title font increased
  - Today Memory layout changed to icon first, then “那年今天” / next line memory title; detail content starts on its own line and does not share the icon column.
- Budget/location display rule:
  - if both budget and location are missing/null, hide the whole row
  - if only one exists, show only that one
  - if both exist, show both
  - literal `null` fields should not be displayed.

### Settings / My page UI changes

- “我的” page was redesigned to match user prototype while preserving top title, bottom nav, and font.
- Matching card:
  - height reduced by about 30%
  - removed the small diamond/point above `test66`
  - invite-code font reduced by two sizes
- Feature card groups:
  - “关系维护”, “冲突修复”, “承诺墙”, “小宇宙”, “冒险挑战”, “梗百科”, “时光信箱” text size changed to match “生成邀请码”
  - these cards were made equal height and about 20% shorter
  - gap between the large groups was reduced.

### Profile page behavior

- Personal profile page was redesigned to match user prototype.
- Top and bottom chrome and font are preserved.
- Avatar area:
  - removed heart badge at avatar bottom-right
  - nickname below avatar font reduced by two sizes
  - when editing, avatar gets a grey overlay with camera icon
  - tapping avatar in edit mode lets the user pick/crop/save a new avatar image.
- Profile fields under avatar now only keep, in order:
  - nickname
  - space name
  - space id
  - bound partner
  - password
- Removed field-leading emoji/icons.
- Field row height was reduced by about 30%.
- Password is not shown in plaintext; represent with a placeholder/symbol.
- Bound partner logic:
  - if no partner, display `还没有绑定哦～`.
- Profile fields are read-only by default and do not show right-side arrows.
- Tapping top-right edit does not navigate to a new page; it toggles inline editing and fields become input boxes.
- `spaceId` cannot be edited.
- After save, user remains on the profile page, not returned to “我的”.
- User later complained input boxes were too large; profile edit input boxes should stay compact.

### Space invite UI

- If already bound to a partner, the space invite card should not remain as a large top card.
- Instead, create a separate group/category titled “空间邀请码”, placed near the bottom under “时光信箱”.
- Inside that group there is a one-row card similar to “冒险挑战”/“梗百科”.
- Tapping the row opens the existing invite-code UI in a dialog/overlay.
- Dialog requirements:
  - no extra outer purple wrapper/container
  - only the inner white invite card should remain
  - add a close `×` in the card’s top-right corner
  - background behind the dialog should be fully transparent.

### Font changes

- User disliked the previous font and asked for Android font recommendations.
- Whole app font was changed to MiSans.
- MiSans files are under:
  - `app/src/main/res/font/misans_regular.otf`
  - `app/src/main/res/font/misans_medium.otf`
  - `app/src/main/res/font/misans_semibold.otf`
  - `app/src/main/res/font/misans_bold.otf`
- Theme maps font weights so all `MiSans Bold` usages were changed to use `MiSans SemiBold`.
- User asked what Today layout uses:
  - page title keeps existing title style
  - most card headings use MiSans Medium, small muted text
  - important card content uses MiSans SemiBold after the Bold-to-SemiBold change
  - normal body rows use MiSans Regular/Medium depending on local Compose `fontWeight`.

### Today page custom icons

- User generated 8 transparent-background icon images and placed them in:
  - `app/src/main/res/drawable-nodpi`
- Original image files had invalid Android resource names like `ChatGPT Image ... (1).png`; they were renamed and mapped as:
  - `home_icon_anniversary.png` -> 纪念日
  - `home_icon_schedule.png` -> 日程
  - `home_icon_food.png` -> 吃什么
  - `home_icon_new_schedule.png` -> 新日程
  - `home_icon_memory.png` -> 记回忆
  - `home_icon_care.png` -> 冷静
  - `home_icon_memo.png` -> 备忘录
  - `home_icon_today_memory.png` -> 那年今天
- The provided files were actually `RGB` with light background, not alpha-transparent.
- They were processed into real `RGBA` transparent PNGs, resized to 512 x 512, and saved back under the clean `home_icon_*` names.
- Original uploaded files were moved to:
  - `/tmp/yanye_uploaded_today_icons_original`
- `.DS_Store` was removed from `drawable-nodpi`.
- `HomeScreen.kt` was updated so Today page uses these PNGs.
- Old vector tint/color-block styling was removed for those icons:
  - `LeadingIcon` now renders the PNG directly, without tint and without colored rounded background
  - quick action icons render the PNG directly as well.
- The changed project built successfully afterward.

### Standalone generated icon

- User asked for one transparent-background icon, theme “那年今天”, saved to Desktop.
- A local script drew a 1024 x 1024 `RGBA` transparent PNG:
  - pink jelly calendar
  - heart clock motif
  - sparkles
- Saved path:
  - `/Users/a86153/Desktop/那年今天_icon.png`
- User then asked to save as `memory`.
- File was copied to:
  - `/Users/a86153/Desktop/memory.png`
- Both files were preserved.

### Build verification

- After icon replacement, debug build succeeded:
  ```bash
  JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
  ```
- APK path:
  - `/Users/a86153/Desktop/YanYeHome/app/build/outputs/apk/debug/app-debug.apk`

### User working preferences reinforced

- User is very sensitive to UI proportions and alignment.
- For prototype-based UI changes, preserve top title, bottom navigation, and font unless user explicitly says otherwise.
- Do not make cards or fonts too large at first; user prefers compact, prototype-like proportions.
- Avoid visible `null` text anywhere in UI.
- For data isolation, never introduce automatic broad `coupleId` rewrites or migration-on-startup again.
