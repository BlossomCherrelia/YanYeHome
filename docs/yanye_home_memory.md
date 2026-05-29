# YanYeHome Development Memory

## Project Context

- Project directory: `/Users/a86153/Desktop/YanYeHome`
- App name: YanYeHome
- Former product name in PRD: Zero Distance
- Tech stack: Android, Kotlin, Jetpack Compose, Material 3, Room, KSP.
- Main build command:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

## Product Direction

YanYeHome is a private relationship app for two people. It emphasizes shared memories, lightweight care, planning, wishes, food choices, anniversary tracking, and future sync between partners.

Important product principle from the user:

- This is a practical app, not a tutorial or marketing page.
- Avoid explanatory filler text in UI.
- Prefer direct usable screens.
- Shared modules should support couple-level sync.
- Private/interactive modules need per-user separation.

## Completed Core Architecture

Basic app architecture has been created:

- Room database
- Repository layer
- ViewModel layer
- Jetpack Compose UI
- Bottom navigation
- Theme and basic pages

Current major modules include:

- Home
- Anniversary
- Wish
- Schedule
- Food decision wheel
- Care / menstrual tracking
- Footprint map
- Settings

## Database

Current Room database has been progressively migrated through multiple versions.

Important latest migrations:

- Map data was cleared during version 10 migration:
  - `province_lights`
  - `city_lights`
  - `city_memories`
- Anniversary sync fields were added in version 11:
  - `remoteId`
  - `coupleId`
  - `ownerUserId`
  - `syncStatus`
  - `remoteUpdatedAt`

## Anniversary Module

Implemented:

- Anniversary entity, DAO, repository, UI.
- Add/edit/delete anniversary.
- Fields include name, date, type, display mode, reminder, note, cover image URI, gift wish link placeholder, celebration archive placeholder.
- Sync fields are now included for future cloud sync.

Current sync behavior:

- Saving an anniversary marks it as pending sync.
- Existing local anniversaries without `remoteId` are considered pending sync.
- The anniversary page has a manual sync button.
- Saving also attempts auto sync.

## Wish Module

Implemented:

- Wish entity, DAO, repository, UI.
- Add/edit wishes.
- Visibility ideas are represented:
  - Shared
  - Private
  - Reveal after date
- Schedule-link placeholder exists for future one-click schedule creation.

Still to do:

- Cloud sync.
- Real partner-level permission enforcement.

## Schedule Module

Implemented:

- Schedule entity, DAO, repository, UI.
- Calendar-style schedule list.
- Add schedule.
- Supports common event fields:
  - title
  - time
  - location
  - reminder
  - budget
  - participants
  - linked wish
- Guide/travel mode placeholder:
  - restaurants
  - activities
  - route
  - backup plan
- Completion memory/archive hooks exist.

Still to do:

- Cloud sync.
- Travel schedule to footprint map linkage.

## Food Decision Module

Implemented:

- Restaurant entity, DAO, repository.
- Food decision wheel UI.
- Modes:
  - Blind restaurant
  - Cuisine wheel
  - Rules wheel
- Wheel labels are drawn directly on wheel segments.
- Result is bound to pointer position, not fake random result.
- Cuisine mode uses de-duplicated cuisines so each cuisine has equal probability regardless of how many restaurants share that cuisine.
- User explicitly said not to change this equal-probability behavior.
- On mode switch, pointer state resets instantly without rotating back.
- Rule mode resets to a single default rule `1` when leaving/re-entering.
- Add restaurant fields were simplified to:
  - restaurant name
  - cuisine

Important user preference:

- Do not add tutorial-like copy.
- Keep wheel centered and practical.

## Care / Menstrual Tracking Module

Original memo/care center was removed from UI. The module now focuses on menstrual tracking only.

Implemented:

- Period calendar inspired by common menstrual apps.
- Month header with jump-to-month and cycle-length settings.
- Swipe month switching.
- Period start/end logic:
  - Tap a date to mark period started.
  - Start day is solid red.
  - Following editable prediction window is translucent red.
  - Within 10 days from start, any date can be selected as period end, even after an end date was already set.
  - Tapping the start day again cancels the entire period record.
- Ovulation logic:
  - Luteal phase removed.
  - Ovulation window uses purple text.
  - Ovulation day has a star marker.
  - Historical ovulation is calculated from all recorded period starts.
- Predicted period logic:
  - Only latest period record generates the next predicted period.
  - Older predicted periods are removed once a later real period is recorded.
- Settings include clearing menstrual records.

## Footprint Map Module

Implemented:

- Province-level China map.
- City-level map after province is lit.
- City lighting as third-level menu.
- Real province GeoJSON is in local assets.
- City GeoJSON assets were downloaded for most provinces.
- Taiwan city GeoJSON source was invalid/tiny and uses fallback behavior.
- Province/city lights are stored separately:
  - `province_lights`
  - `city_lights`
- City memories are stored in:
  - `city_memories`

Latest interaction rules:

- Level 1: national map.
- Level 2: province map.
- Level 3: city lighting.
- National map top shows:
  - lit provinces `x/34`
  - city memories count
  - completion percentage
- Province menu:
  - If not lit, only centered `点亮xx` switch is shown.
  - No province placeholder image.
  - Only top-right `全国` button remains.
  - If lit, switch moves to top and palette icon appears.
  - Palette opens a separate dialog with five colors.
  - Lit province shows city-level map and province note below.
- City menu:
  - Same centered switch behavior.
  - Top-right returns to province menu, not national map.
  - City light count increments when city is lit, not when memory is saved.
  - City memory is a single editable record per city, not append-only.

National map issue fixed:

- Hong Kong/Macau are hard to tap because they are tiny.
- Added nearest-center fallback hit detection for small regions.
- Max zoom increased.
- Text size smaller at 100% and scales with map.

## Firebase Attempt

Firebase was configured first:

- Firebase Auth
- Firebase Firestore
- `google-services.json`
- Conditional Google Services plugin

Build worked with Firebase:

- `:app:processDebugGoogleServices` ran successfully.

But runtime sync failed in simulator because Firebase/Google services were inaccessible from the current network/simulator environment in China.

User decided to switch away from Firebase.

## CloudBase Direction

Chosen replacement: Tencent CloudBase.

User created CloudBase environment:

```text
yanyehome-d9grtwqrlc809509f
```

CloudBase environment name:

```text
yanyehome
```

User created `anniversaries` collection with permission:

```text
读取全部数据，修改本人数据
```

Code currently added:

- Cloud function folder:
  - `cloudfunctions/syncAnniversaries/index.js`
  - `cloudfunctions/syncAnniversaries/package.json`
- Android config:
  - `CloudBaseConfig.kt`
- Android sync service:
  - `CloudBaseAnniversarySyncService.kt`
- Anniversary ViewModel switched from Firebase sync service to CloudBase sync service.

Current CloudBase config:

```kotlin
ENV_ID = "yanyehome-d9grtwqrlc809509f"
SYNC_ANNIVERSARIES_URL = ""
```

Still needed:

- Deploy `syncAnniversaries` cloud function.
- Create HTTP access route.
- Fill returned HTTP URL into `CloudBaseConfig.SYNC_ANNIVERSARIES_URL`.
- Rebuild and test anniversary sync.

Cloud function creation notes:

- Function name: `syncAnniversaries`
- Runtime: Node.js 18.15
- Upload folder:

```text
/Users/a86153/Desktop/YanYeHome/cloudfunctions/syncAnniversaries
```

- Auto install dependencies: on
- Memory: 256MB
- Timeout: 10 seconds recommended
- Handler should be:

```text
index.main
```

Reason:

- File is `index.js`.
- Export is `exports.main`.

## Sync Design

Long-term sync model:

- Room remains local cache.
- CloudBase is cloud source.
- Each user has local user ID.
- Both users share a couple ID.
- Shared data syncs to couple space.
- Private/interactive data must include ownership and visibility fields.

Generic sync fields:

- `remoteId`
- `coupleId`
- `ownerUserId`
- `syncStatus`
- `remoteUpdatedAt`

Shared sync utilities:

- `CloudBaseConfig`: env id and CloudBase HTTP endpoints.
- `SyncSettings`: stable local user id and couple id.
- `CloudBaseHttpClient`: common JSON POST, timeout, HTTP error handling, and CloudBase `{ statusCode, body }` response unwrapping.
- `SyncRules`: stable remote id generation, owner fallback, shared visibility filtering, wish reveal/private rules.

Visibility model:

- Shared
- Private
- Reveal after date

Interactive events such as apology bubbles should be separate event records:

- `fromUserId`
- `toUserId`
- `type`
- `status`
- `message`
- `createdAt`
- `respondedAt`

## User Preferences

- Answer in Chinese unless code or identifiers require English.
- Be practical and direct.
- User prefers implementing real usable app screens over placeholder/tutorial text.
- Avoid unnecessary explanatory UI text inside the app.
- Preserve equal-probability cuisine wheel behavior.
- For map interactions, follow the strict three-level model:
  - national map
  - province map
  - city lighting

## Current Sync State

Anniversary CloudBase sync is working through:

- Collection: `anniversaries`
- Function: `syncAnniversaries`
- Route: `/syncAnniversaries`

Wish CloudBase sync has been implemented locally and needs console setup:

- Collection: `wishes`
- Function zip: `/Users/a86153/Desktop/YanYeHome/cloudfunctions/syncWishes.zip`
- Function name: `syncWishes`
- Handler: `index.main`
- HTTP route: `/syncWishes`

Wish sync rules:

- Shared wishes sync immediately.
- Private wishes stay local.
- Reveal-after-date wishes sync only after the reveal date.
- Deleted synced wishes upload a tombstone so they do not reappear.

Schedule CloudBase sync has been implemented locally and needs console setup:

- Collection: `schedules`
- Function zip: `/Users/a86153/Desktop/YanYeHome/cloudfunctions/syncSchedules.zip`
- Function name: `syncSchedules`
- Handler: `index.main`
- HTTP route: `/syncSchedules`

Schedule sync rules:

- Shared schedules sync immediately.
- Deleted synced schedules upload a tombstone.
- Schedule-to-wish relation uses `linkedWishRemoteId` in cloud data because local Room ids differ across devices.
- Memory archive details are still local; schedule completion state syncs, memory-card sync can be added later.

Restaurant/Food CloudBase sync has been implemented locally and needs console setup:

- Collection: `restaurants`
- Function zip: `/Users/a86153/Desktop/YanYeHome/cloudfunctions/syncRestaurants.zip`
- Function name: `syncRestaurants`
- Handler: `index.main`
- HTTP route: `/syncRestaurants`

Food sync rules:

- Sync only the restaurant pool: restaurant name and cuisine, plus existing lightweight metadata.
- Cuisine wheel remains derived locally from distinct restaurant cuisines.
- Do not sync rule wheel options.
- Do not sync spin results or `lastPickedAt`.

Footprint/map CloudBase sync has been implemented locally and needs console setup:

- Collection: `footprints`
- Function zip: `/Users/a86153/Desktop/YanYeHome/cloudfunctions/syncFootprints.zip`
- Function name: `syncFootprints`
- Handler: `index.main`
- HTTP route: `/syncFootprints`

Footprint sync rules:

- One collection stores all map records and uses `type` to distinguish `PROVINCE_LIGHT`, `CITY_LIGHT`, and `CITY_MEMORY`.
- Province lighting syncs `provinceName`, lit state, fill color, province note, delete/toggle state, and schedule link placeholder.
- City lighting syncs `provinceName`, `cityName`, lit state, fill color, city note, delete/toggle state, and schedule link placeholder.
- City memory syncs title, date, foods, places, photo URI text, linked schedule placeholder, inside joke, expense, pitfall notes, rating, note, and delete state.
- Remote ids are deterministic from map natural keys, so two devices editing the same province/city merge into the same cloud document instead of creating duplicates.

Memory CloudBase sync has been implemented locally and needs console setup:

- Collection: `memories`
- Function zip: `/Users/a86153/Desktop/YanYeHome/cloudfunctions/syncMemories.zip`
- Function name: `syncMemories`
- Handler: `index.main`
- HTTP route: `/syncMemories`

Memory sync rules:

- Syncs ordinary timeline memories so the record itself, not only uploaded images, appears on the partner device.
- Synced fields include title, date, location, photo URL text, food notes, expense, mood, note, deleted state, and sync metadata.
- Image binaries still upload through the existing CloudBase image upload flow; memory sync only transports the resulting URL strings.
- Local `scheduleId` and `linkedWishId` are not used as cross-device identifiers, so pulled memories stay readable even when Room ids differ across devices.

Care cycle CloudBase sync has been implemented locally and needs console setup:

- Collection: `care_cycles`
- Function zip: `/Users/a86153/Desktop/YanYeHome/cloudfunctions/syncCareCycles.zip`
- Function name: `syncCareCycles`
- Handler: `index.main`
- HTTP route: `/syncCareCycles`

Care sync rules:

- Syncs menstrual/care cycle records only, not the removed memo center.
- Fields synced: start date, end date, cycle length, pain level, mood, avoid notes, care preference, share reminder flag, deleted state.
- Remote ids are deterministic from period start day, so editing the same recorded cycle updates the same cloud document.

Automatic sync UX:

- CloudBase-backed modules now use a local-first debounced sync controller.
- User edits save to Room immediately and show a pending message; cloud upload runs after a 3-second debounce.
- Manual sync buttons are still visible for testing and call immediate sync.
- Entering a synced page triggers immediate cloud sync/pull.
- Leaving a synced page or sending the app to background triggers `flushSync`, which cancels any pending debounce delay and attempts upload immediately.
- If upload fails, records keep their `PENDING_*` sync status and are retried on the next automatic/manual sync.

## Development Memory Snapshot - 2026-05-23

This section records the practical development history and current product state so future work can continue without rediscovering decisions.

### Overall Development Path

The project started from the documents in `docs/`:

- `zero_distance_prd.html`: product requirements. `Zero Distance` is the former name; current app name is `YanYeHome`.
- `yanye_home_android_tech_guide.html`: Android technical direction and implementation guide.
- `yanye_home_development_steps.html`: phased development steps.

The build order so far has been:

1. Basic Android app shell: package structure, theme, bottom navigation, and core empty pages.
2. Room local database and repositories.
3. Anniversary module.
4. Wish module.
5. Schedule module.
6. Food decision wheel.
7. Care/menstrual calendar.
8. Footprint map with real GeoJSON map rendering.
9. Firebase experiment, then replacement with Tencent CloudBase because Google services were unreliable in the user's China-based simulator/network.
10. CloudBase sync for shared modules.
11. Automatic debounced sync mechanism.
12. Large frontend restyling based on the user's prototype HTML and screenshots.
13. Todo/memo feature.
14. Memory/"那年今天" feature.

### Current Product Positioning

YanYeHome is a private relationship app for two people. It should feel practical, quiet, intimate, and useful. The app is not a tutorial, not a landing page, and not a marketing product.

Core product values:

- Shared life records should be simple to add and easy to revisit.
- Private and shared visibility must be respected.
- Most screens should be directly usable, not explanatory.
- The app should feel minimal, clean, and stable rather than decorative.
- UI copy such as `null`, tutorial text, and oversized explanations should generally be removed.

### Current Tech Stack

- Android native app.
- Kotlin.
- Jetpack Compose.
- Material 3.
- Room.
- KSP.
- Repository + ViewModel architecture.
- Tencent CloudBase via HTTP cloud functions.

Main build command:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

Use this after code changes. The latest known build after the Space/My cleanup was successful.

### Important Paths

- Project root: `/Users/a86153/Desktop/YanYeHome`
- Main app code: `app/src/main/java/com/yanye/home`
- Cloud functions: `cloudfunctions/`
- Docs: `docs/`
- GeoJSON assets:
  - `app/src/main/assets/china_provinces.geojson`
  - `app/src/main/assets/province_city_geojson/`

Important Compose screens:

- Home: `ui/home/HomeScreen.kt`
- Calendar/Schedule/Anniversary: `ui/schedule/ScheduleScreen.kt`
- Wish: `ui/wish/WishScreen.kt`
- Space: `ui/space/SpaceScreen.kt`
- Footprint map: `ui/footprint/FootprintScreen.kt`
- Care: `ui/care/` or related care screen files, depending on current package structure.
- Memo/Todo: `ui/memo/MemoScreen.kt`
- Memory: `ui/memory/MemoryScreen.kt`
- Settings/My: `ui/settings/SettingsScreen.kt`
- Navigation: `navigation/YanYeDestination.kt`, `navigation/YanYeNavGraph.kt`

### CloudBase Environment

Tencent CloudBase is the chosen backend.

Environment:

```text
yanyehome-d9grtwqrlc809509f
```

Base HTTP domain used by the user:

```text
yanyehome-d9grtwqrlc809509f-1434875599.ap-shanghai.app.tcloudbase.com
```

Cloud function convention:

- Runtime: Node.js 18.15.
- Handler: `index.main`.
- Upload zip or folder from `cloudfunctions/<functionName>`.
- HTTP route usually matches the function name, such as `/syncWishes`.

Known CloudBase functions/collections:

- `anniversaries` + `syncAnniversaries`
- `wishes` + `syncWishes`
- `schedules` + `syncSchedules`
- `restaurants` + `syncRestaurants`
- `footprints` + `syncFootprints`
- `care_cycles` + `syncCareCycles`
- `memos` + `syncMemos`

CloudBase permissions used during testing were generally equivalent to:

```text
读取全部数据，修改本人数据
```

### Sync Architecture

Current sync design is local-first:

1. User edits data.
2. Room database updates immediately.
3. UI updates from local Room state.
4. Sync status marks the record pending.
5. CloudBase upload/pull runs through a debounced sync controller.
6. If upload fails, pending state remains and is retried later.

Generic sync concepts:

- `CloudBaseConfig`: stores endpoints and environment configuration.
- `SyncSettings`: stores stable `coupleId` and `localUserId`.
- `CloudBaseHttpClient`: common HTTP POST and CloudBase response parsing.
- `SyncRules`: common remote id, owner id, visibility, and merge rules.

Common sync fields:

- `remoteId`
- `coupleId`
- `ownerUserId`
- `createdBy`
- `visibility`
- `syncStatus`
- `remoteUpdatedAt`
- `isDeleted`

Debounced sync behavior:

- Edits save locally immediately.
- Cloud upload waits a short delay so repeated input does not freeze UI.
- During testing, delay was temporarily increased to 10 seconds, then intended to return to a shorter delay later.
- Page leave and app background should call `flushSync`.
- Manual sync buttons were kept during testing, but final product goal is to remove sync buttons and rely on automatic sync.

Important sync rule:

- Do not assume no debounce means no failures. It only reduces delay; network/backend failures can still happen.
- Pending sync records must remain retryable.

### Anniversary Module Current State

Implemented:

- Room entity/DAO/repository.
- Add/edit/delete.
- Countdown/count-up/anniversary display.
- CloudBase sync.
- Default relationship anniversary.
- Home display settings.
- Homepage anniversary card now uses the special `在一起` anniversary.

Important current rules:

- There must always be a built-in relationship anniversary named `在一起`.
- If a user installs the app for the first time, the app should ensure this default exists, using a placeholder date such as January 1 if needed.
- Homepage anniversary section uses this `在一起` record.
- The old standalone anniversary page should no longer be opened from home. Home anniversary should open Calendar with the Anniversary tab selected.
- Anniversary display settings allow choosing which anniversaries appear on the homepage and ordering them.

UI decisions:

- Anniversary list cards should be minimal: remaining/passed days, title, date.
- No circular type icons like `恋`, `生`, `旅`.
- Detail page follows prototype:
  - large day count
  - title-like text below
  - start date
  - optional sections
- `历年庆祝` and `礼物灵感` were asked to be commented/hidden for now, not permanently deleted from all code if easy to preserve.
- Do not show `null` for empty note or optional fields.
- New anniversary should not force a default name.
- Form rows must align value columns precisely.

### Wish Module Current State

Implemented:

- Room entity/DAO/repository.
- Wish list.
- Add/edit/detail pages.
- Shared/private/reveal-after-date visibility.
- CloudBase sync.
- Deleted synced wishes upload tombstones.
- Shared wishes sync; private wishes stay local; reveal-after-date syncs after reveal date.

Important logic decisions:

- Wish-to-schedule linkage should use remote ids in cloud, because local Room ids differ across devices.
- The old "安排时间 -> 生成日程" button duplicated the linked schedule concept and was removed.
- Linked schedule section should:
  - If no linked schedule: allow going to calendar/schedule arrangement.
  - If linked schedule exists: open schedule detail.

UI decisions:

- Wish list follows prototype:
  - title
  - category/budget
  - status chip
- Category chips are small.
- No sync button in final UI.
- Add/edit wish uses the same unified form style as schedule/anniversary.
- `可转日程` switch size/spacing should match schedule's `攻略模式` switch if it remains.

### Schedule Module Current State

Implemented:

- Room entity/DAO/repository.
- Calendar page.
- Add/edit/detail pages.
- Linked wish support.
- Guide/travel mode fields.
- Archive/completion behavior.
- CloudBase sync.

Important logic:

- Home "今日日程" should read real schedules, not hardcoded text.
- If today has no schedule: show `今天还没有日程哦，请新建日程`.
- If today has schedules: show only `time title`, one per schedule.
- Home schedule card is elastic and grows with item count.
- Linked wish selector in schedule edit is expandable:
  - Start collapsed with `关联愿望`.
  - Expand to a two-column chooser.
  - Left side: wish categories.
  - Right side: wishes in selected category.
  - If no selection, schedule remains unlinked.
- Changing linked wish must update cloud `linkedWishRemoteId`.
- Removing linked wish must clear cloud relation.
- A wish already linked to a schedule should not remain incorrectly selectable as if unused.

UI decisions:

- Schedule detail should be a clean detail page, not inline expanded actions.
- Add/edit schedule should be page-based, not a dialog.
- Detail fields are inside one clean card with small gray labels and bold values.
- `攻略模式` switch should be smaller, aligned with text, and not too close.
- Guide mode text fields should start one line high and grow with content.

### Food Decision Module Current State

Implemented:

- Restaurant pool.
- Cuisine pool derived from restaurant cuisines.
- CloudBase sync for restaurant/cuisine pool.
- Modes:
  - Blind restaurant.
  - Cuisine wheel.
  - Existing rules wheel.
- Wheel segment labels reflect actual current options.
- Pointer result is bound to final physical wheel position.
- Mode switch resets pointer to center/up without rotating back.

Important probability rule:

- Cuisine wheel must de-duplicate cuisines.
- If there are 10 Japanese restaurants and 1 barbecue restaurant, Japanese cuisine and barbecue cuisine should still each have equal probability in cuisine mode.
- Do not change this to weighted probability.

Important wheel visual rule:

- After each spin, only pointer position resets as required.
- Restaurant/cuisine/rule segment order should not shuffle after a spin.
- Re-entering page resets pointer only, not option order.

### Care / Menstrual Tracking Current State

Original "care memo center" was removed. Current care module focuses on menstrual tracking.

Implemented:

- Calendar-style period page.
- Period start/end recording.
- Prediction window.
- Ovulation period/day marker.
- Cycle length settings.
- CloudBase sync for care cycles.

Important logic:

- Tap period start: mark actual start.
- Start day is solid deep pink.
- Following period window is translucent/predicted.
- Within 10 days after start, any date can become period end, even if a previous end was selected.
- Tapping the start day again cancels the whole period record.
- If a mistaken period is removed, related predicted period and ovulation markers should recalculate.
- Ovulation period/day should also calculate historical periods, not only future.
- Predicted menstrual period only comes from the latest real record.
- Ovulation can still be calculated from all actual records.

UI decisions:

- Calendar should use the same clean calendar style as schedule calendar, while preserving colored date states.
- No luteal phase block.
- Ovulation window uses purple text.
- Ovulation day uses a small star.

### Footprint Map Current State

Implemented:

- Real China province GeoJSON map.
- Province-level national map.
- City-level maps through province GeoJSON assets.
- Province/city lighting.
- Fill color choice.
- City memory cards.
- CloudBase sync for all footprint data.

Important levels:

1. Level 1: national map.
2. Level 2: province map.
3. Level 3: city lighting.

Important interaction:

- National map supports zoom and pan.
- Small areas like Hong Kong/Macau use nearest-center fallback hit detection.
- Province screen:
  - If not lit, show a large round `点亮xx` button centered.
  - If lit, top title row shows palette and lightbulb icons.
  - Lightbulb toggles lit/unlit.
  - Palette opens color selection.
  - Province note was removed.
- City screen:
  - Same lightbulb/palette logic.
  - City note was removed.
  - If lit and no memory, show add-city-memory card.
  - If memory exists, show memory detail directly and allow editing.
- Returning should always return one level up:
  - city -> province
  - province -> national
  - national -> space

Navigation rule:

- Tapping bottom nav `空间` must return to Space homepage, not keep user trapped inside map/wish/memory internal pages.

### Todo/Memo Current State

User requested a practical memo/todo module after initially removing old memo center.

Implemented:

- New memo/todo database model.
- Shared/private todo.
- CloudBase sync through `memos` and `syncMemos`.
- Todo list with tabs:
  - 全部待办
  - 共享待办
  - 个人待办
- Pending and completed sections.
- Add/edit pages follow global form style.
- Homepage memo card.
- Homepage display settings and ordering.
- Swipe delete in memo list.
- Completion animation on homepage checkbox.

Important todo terminology:

- Use `备忘录` for the homepage entry.
- Use `待完成日期` and `待完成时间`, not `提醒日期` and `提醒时间`.
- `待完成时间` is optional.
- If no pending todo: homepage shows `还没有待办哦，快去加入备忘录吧！`.
- If todos exist: homepage shows selected todos.
- Default homepage display is urgent/top 3.
- User can manually enable more than 3 and reorder via a display settings page similar to anniversary homepage display settings.
- Category was removed because it was not useful.

Important todo display rules:

- Homepage todo rows should conserve space:
  - Show title and due date/time.
  - Do not show location/note on homepage.
  - If date/time/note are empty, do not show placeholder text.
- In todo list/detail:
  - If note is long, card height grows and note is fully visible.
  - If note exists, format is:
    - title
    - note
    - date/time
  - If note does not exist, only title and optional date/time.
- Date display:
  - If date is today/tomorrow, show `今天`/`明天` even when no specific time is set.

Swipe delete rule:

- Right swipe around 30% reveals red delete action.
- Deletion requires clicking the red delete action after reveal.
- This swipe-delete pattern may be reused later.

### Memory / "那年今天" Current State

Implemented:

- Memory list page.
- Memory detail page.
- Add/edit memory page.
- Homepage "那年今天" card.
- Navigation from Space and Home.

Current behavior:

- Space "回忆" opens the memory list.
- Home "那年今天" can open the corresponding memory detail.
- If there is no memory from a previous year on this month/day:
  - First line only shows `那年今天`.
  - Content shows `那年的今天没有记录回忆，不过一定也是美好的一天～`.
  - Do not show `日期 null`.
- If memory has a photo, home card can display image preview and detail can show photo.

Memory list UI:

- Title: `回忆`.
- Top-right is `+`, not `筛选`.
- Year row shows something like `2026年` with a small arrow.
- Clicking year arrow opens jump-time picker.
- No category chips such as `全部、本月、北京、旅行`.
- Timeline style:
  - red dot and vertical line on left.
  - month marker such as `05` appears near the first memory of each month.
  - month marker does not need sticky behavior.
  - year header is sticky-like: when scrolling through 2026 records, `2026年` stays at top until 2025 replaces it.
- Date row should be like `5/23 · location`, without year; year is handled separately.
- Remove unclear source text such as `手动`.
- Notes/remarks should show fully and card height should grow.
- Photo block appears inside the card below source/note if photo exists.

Memory detail UI:

- Similar to prototype:
  - photo area at top.
  - title.
  - date/location/mood row.
  - note content card.
  - linked schedule card if available.

Jump-time picker:

- The first version was too ugly.
- User prefers a wheel-like picker style inspired by iOS/time picker visuals.
- It should still match YanYeHome's minimal style.

Sync status:

- Memory data currently comes from local schedule memory/archive logic.
- CloudBase sync for memories has not yet been fully described as done in this memory file.
- If implemented later, add a `memories` collection/cloud function and use the same local-first sync pattern.

### Home UI Current State

Home page is based on the user's prototype style.

Important home sections:

- Header: `妍叶之庭`, bold, compact.
- Status card: `她的状态`.
- Anniversary card.
- Today's schedule card.
- Quick actions:
  - 吃什么
  - 新日程
  - 记回忆
  - 冷静
- Todo/memo card.
- "那年今天" card.

Current home UI rules:

- `她的状态`, `纪念日`, `今日日程`, and `那年今天` label font sizes were slightly increased.
- Main content text like `想贴贴`, `在一起 x 天`, schedule content, todo title, and memory title should use consistent size.
- Cards with the same number of text rows should have the same height.
- Cards should be elastic if content grows.
- The previous separate `关怀提示` card was removed; care prediction can later be shown inside the status card right side.
- Status card should not be too tall; vertical padding should stay compact.
- Pink arrows in the homepage anniversary/schedule cards were removed where requested.
- The homepage should not route to old pages.

Potential future status design:

- Main status left: user-set status like `想贴贴`.
- Time such as `10分钟前` can sit on the right of the status line.
- Care hint such as `还有一天来姨妈哦` can appear on the right side of status card without the label `关怀提示`.

### Space UI Current State

Space page follows prototype card layout:

- Title: `我们的空间`.
- Big full-width card: 点亮地图.
- Half cards: 愿望清单, 回忆.
- Placeholder modules:
  - 冒险挑战
  - 时光信箱

Latest cleanup:

- Removed pink arrow badges from:
  - 点亮地图
  - 愿望清单
  - 回忆
- Removed `null` from placeholder modules.
- `功能预留` changed to `1.1版本上线`.
- `点亮地图` card should occupy full width, not just half row.

### My / Settings Current State

Settings/My page has placeholder features:

- 冲突修复
- 承诺墙
- 冒险挑战
- 梗百科
- 时光信箱
- 通知设置
- 隐私设置
- 数据备份

Latest cleanup:

- Removed small descriptive subtitles under these placeholders.
- Right-side pink `null` changed to `1.1版本上线`.

### Global UI Style Rules

The current design direction is strongly prototype-driven:

- Clean, flat, minimal, practical.
- Thin light gray borders.
- Avoid dark heavy borders.
- Avoid big explanatory blocks.
- Use generous but not wasteful spacing.
- Main page headers should share exact top spacing, font size, and divider behavior.
- Secondary pages:
  - Back arrow left.
  - Centered title.
  - Right action such as `保存`, `编辑`, `+`.
  - Same header height/divider style everywhere.
- Add/edit pages:
  - Use unified form cards.
  - Labels in gray.
  - Values/input fields aligned to a consistent left column.
  - Do not show `null`; omit empty optional rows or show an intentional placeholder only when useful.
- Cards:
  - Light border.
  - Rounded corners.
  - Elastic height.
  - Same row count should look same height.
- Do not use circular text icons like `恋`, `生`, `图`, `愿`.
- Avoid pink arrows except where explicitly preserved.

### Navigation Rules

Important navigation fix:

- Bottom nav should reset top-level pages.
- If user is inside:
  - map city detail
  - wish detail
  - memory detail
  - any nested Space page

  then tapping bottom nav `空间` should return to Space homepage.

Likewise:

- Home anniversary card opens Calendar with Anniversary tab selected.
- Home schedule card opens Calendar with Schedule tab selected.
- Home memory card opens Memory detail if a matching memory exists.
- Internal back buttons should return one level up, not jump unexpectedly to top-level unless intended.

### Known User Preferences To Preserve

- Respond in Chinese.
- Implement directly when the request is clear.
- Keep changes scoped.
- Preserve existing backend/database logic unless the user explicitly asks to modify it.
- When UI is being adjusted, avoid breaking sync and repositories.
- Use existing project patterns.
- Run Gradle build after code changes.
- Be especially careful with:
  - Room migrations.
  - CloudBase remote ids.
  - private/shared visibility.
  - deleted tombstones.
  - avoiding duplicate records after sync.

### Important Bugs Fixed During Development

- Firebase sync hung/failed due to Google service/network limitations.
- CloudBase `_id` update failure fixed by removing direct `_id` mutation in payload.
- Anniversary duplicates and deleted records reappearing after sync fixed through deterministic remote ids/tombstones/merge logic.
- Wish shared-to-private update duplicate bug fixed.
- Schedule linked wish relation not updating fixed by syncing `linkedWishRemoteId`.
- Food wheel fake result/pointer mismatch fixed by binding pointer position to actual selected segment.
- Cuisine probability weighted-by-restaurant bug avoided by de-duplicating cuisines.
- Map click mismatch after zoom fixed by transforming pointer coordinates correctly.
- Small-region map hit detection added for Hong Kong/Macau.
- Menstrual record edit/cancel bugs fixed by making the 10-day end window mutable and clearing related period state correctly.
- Todo crash after adding memo feature was debugged and fixed.

### Things Not Yet Fully Implemented Or Intentionally Deferred

- Music sharing across QQ Music and NetEase Cloud Music was discussed but deferred to v1 later.
- Gift linkage for anniversaries remains mostly placeholder/hidden.
- Celebration archive for anniversaries is placeholder/hidden.
- Real apology bubble/conflict repair module is not implemented.
- Adventure challenge, time capsule, promise wall, meme encyclopedia, privacy/backup details are placeholders for `1.1版本上线`.
- Memory CloudBase sync may still need to be added if not already implemented after this snapshot.
- Push notifications/reminders are not fully implemented; date/time fields prepare for it.

### Image Upload And Sharing - 2026-05-23

Image sharing was added after local image URI support.

Important decision:

- Do not create a normal database collection just for images.
- Actual image files should be stored in CloudBase Cloud Storage.
- Existing database fields continue to store image references, but after upload they should store cloud URLs rather than local `content://` URIs.

Current image fields:

- Wish cover image: `Wish.coverImageUri`
- Footprint city memory photos: `CityMemory.photoUris`
- Memory photos: `Memory.photoUris`

Implemented local Android code:

- Shared upload service:
  - `app/src/main/java/com/yanye/home/data/sync/CloudBaseImageUploadService.kt`
- Shared picker/preview component:
  - `app/src/main/java/com/yanye/home/ui/common/ImagePickerField.kt`
- CloudBase endpoint:
  - `CloudBaseConfig.UPLOAD_IMAGE_URL = "$BASE_URL/uploadImage"`

Current app-side behavior:

1. User taps choose/change image.
2. Android opens the system document/image picker.
3. App temporarily saves and previews the local URI.
4. App compresses the image to JPEG, max dimension about 1600px.
5. App posts base64 image data to the CloudBase `uploadImage` HTTP function.
6. Cloud function uploads the file to CloudBase Cloud Storage.
7. Cloud function returns a cloud URL or fileID.
8. App replaces the local URI field with the returned cloud URL.
9. Existing module sync then syncs that cloud URL to the other device.

Important limitation:

- Wish and footprint images can be shared once their existing records sync.
- Memory photos upload to CloudBase, but the memory record itself still needs Memory CloudBase sync if the other phone should see the memory item automatically.

Implemented cloud function:

- Folder:
  - `cloudfunctions/uploadImage/`
- Zip:
  - `cloudfunctions/uploadImage.zip`
- Function name:
  - `uploadImage`
- Handler:
  - `index.main`
- Runtime:
  - Node.js 18.15
- HTTP route:
  - `/uploadImage`

Tencent CloudBase console setup needed:

1. Enter environment `yanyehome-d9grtwqrlc809509f`.
2. Enable or confirm Cloud Storage is available.
3. Create cloud function `uploadImage`.
4. Upload `/Users/a86153/Desktop/YanYeHome/cloudfunctions/uploadImage.zip`.
5. Enable auto install dependencies.
6. Configure HTTP access route `/uploadImage`.

Current image display rules:

- If a valid image exists, show the image itself at full available width and preserve image aspect ratio.
- Do not show gray placeholder blocks when there is no image.
- Do not show gray placeholder blocks when image URL/URI is invalid or unreadable.
- This rule applies to:
  - Memory detail
  - Memory list
  - Home "那年今天"
  - Footprint/city memory
  - shared image preview component

Recent memory UI changes:

- Memory detail no longer shows `null` for empty note/content.
- Empty content is hidden.
- The whole linked schedule section was hidden/removed from memory detail.
- Memory detail/edit labels changed from `备注` to `内容`.
- Memory edit removed `吃了什么` and `花费` fields from the main memory editor UI.
- Memory mood enum added `Unwell("UNWELL")`, displayed as `😣 难受`.

### Homepage And Main Tab Wallpaper - 2026-05-26

Wallpaper assets were added from the user's Desktop:

- `/Users/a86153/Desktop/主页壁纸.jpg`
- `/Users/a86153/Desktop/简洁壁纸.jpg`

They are stored in:

- `app/src/main/res/drawable-nodpi/home_wallpaper.jpg`
- `app/src/main/res/drawable-nodpi/simple_wallpaper.jpg`

Implemented shared component:

- `app/src/main/java/com/yanye/home/ui/common/WallpaperBackground.kt`

Current correct behavior:

- `今天` page uses `home_wallpaper`.
- `日历`, `空间`, `关怀`, and `我的` use `simple_wallpaper`.
- `WallpaperBackground` should stay simple:
  - `ContentScale.Crop`
  - `Modifier.fillMaxSize()`
  - no `scale`
  - no `offset`
- Do not manually tune wallpaper size/position in `WallpaperBackground`; this previously made the image crop/align unpredictably.

Important status bar rule:

- The user wanted the wallpaper to extend behind the top system status bar / battery area.
- The correct solution is:
  - `MainActivity.kt` uses `WindowCompat.setDecorFitsSystemWindows(window, false)`.
  - `MainActivity.kt` sets `window.statusBarColor = Color.TRANSPARENT`.
  - `themes.xml` sets `android:statusBarColor` to `@android:color/transparent`.
  - `YanYeHomeApp.kt` wraps the app shell in an outer `WallpaperBackground` that follows the selected top-level tab:
    - Home -> `home_wallpaper`
    - other main tabs -> `simple_wallpaper`
- Do not set `Scaffold(contentWindowInsets = WindowInsets(0.dp))` for this feature. That made the top page layout/navigation spacing wrong.
- Keep `Scaffold` default insets so page content positions remain stable, while the outer wallpaper still shows behind the transparent status bar.

### Today And Calendar UI Polish - 2026-05-26

Today/home page current decisions:

- Top title remains the space name.
- The old `她的状态` block was removed.
- Home top relationship area is borderless and shows:
  - `我们在一起`
  - large day count
  - `从 yyyy.MM.dd 开始`
- The pink separator line between the day count and start date was removed.
- Home anniversary glass card:
  - no title row `纪念日`
  - no forced `在一起` item
  - only shows anniversaries with `showOnHome = true`
  - if zero anniversaries are enabled for home, the whole anniversary card is hidden
  - does not fill missing rows with `未设置`
- Home schedule and memo cards use the original left-icon/right-title layout.
- Home schedule content uses `FontWeight.Medium`.
- Home memo content was aligned to the same weight/size direction.
- Home `那年今天` title uses the same size/weight as `备忘录`, but keeps its original rose color.
- Empty `那年今天` text uses the same 13sp content size as schedule/memo content and `FontWeight.Medium`.

Calendar page current decisions:

- Schedule/anniversary segmented tab background is very light pink.
- Selected calendar day background is deep pink.
- Schedule indicator under calendar dates is deep pink.
- Floating add button is light pink.
- Schedule list card layout:
  - date header inside the card, formatted like `05.26 周二`, bold
  - each schedule row is pink dot + time + title
  - title and metadata are in the same right-side column so location/budget aligns with title
  - long titles can wrap and increase card height
- Date picker in schedule/anniversary editors is custom, not default Material DatePicker:
  - black/white rounded style matching app
  - clicking year switches to year grid
  - clicking month switches to month grid
  - selecting year proceeds to month selection; selecting month returns to date grid

Anniversary list page current decisions:

- `首页展示设置` is a simple text row, not a card.
- Do not show `已展示 x 个纪念日` in that row.
- `在一起` can be enabled/disabled for homepage display like any normal anniversary; it is not locked.
- Anniversary list cards are compact.
- Left side shows only:
  - anniversary name
  - unified date format `yyyy.MM.dd`
- Do not show a pink status chip above the anniversary name.
- Right side shows full rose text such as:
  - `已经 510 天`
  - `还有 2 天`
  - `已经 1 周年`
- Bottom of anniversary list shows static hint:
  - `— 长按可拖动排序 —`
- Drag sorting UI/back-end is intentionally not implemented yet.

### How To Continue Safely

When continuing:

1. Read this file first.
2. Check current git status before changing code.
3. Inspect the relevant screen/repository before editing.
4. Prefer small UI-only changes when the user asks for polish.
5. If adding sync to a new module:
   - add Room sync fields/migration
   - update DAO/repository pending logic
   - add CloudBase endpoint in config
   - add sync service
   - add cloud function under `cloudfunctions/`
   - create collection/function/HTTP route in Tencent console
   - test create/update/delete/private/shared cases
6. Build with:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

7. Summarize only the meaningful changes to the user.

## Space / Footprint / City Memory Updates - 2026-05-27

Space page entry cards:

- `点亮地图`, `愿望清单`, and `回忆` are now three full-width stacked cards.
- All three use image backgrounds from `drawable-nodpi`:
  - `space_map_card.png`
  - `space_wish_card.png`
  - `space_memory_card.png`
- All three main entry cards use a fixed `5:2` aspect ratio.
- The numeric values on these cards use the same text style scale as the card titles, not oversized display text.
- The cards have rounded outer corners.

Footprint/map page current decisions:

- The footprint feature page uses `simple_wallpaper` as page background via `WallpaperBackground`.
- The top title `点亮地图` should align vertically with top-level page titles such as `我们的空间`.
- `点亮地图` uses the same typography as `我们的空间`:
  - `MaterialTheme.typography.titleLarge`
  - `FontWeight.Bold`
- The previous lower alignment was caused by `TextButton` default height in the back button row; the back arrow is now plain clickable text to avoid pushing the title down.
- Province and city boundary map cards use a pink theme:
  - area outside the map: very pale pink
  - unlit regions: a different light pink
  - lit regions still use the saved fill color logic and remain editable.
- Province and city light prompts use a pink pill button with a white heart and text such as `点亮西藏`, replacing the old black circular button.

City memory redesign direction:

- Target UI is a city memory list page similar to the provided prototype:
  - top city hero card
  - city title and region
  - stats such as memory count, average rating / 默契值, and check-in days
  - grouped memory sections like `吃过的`, `玩过的`, etc.
  - each memory item is a card with cover image, title, rating, summary, date, and menu.
- The current UI still needs to be rebuilt. Data layer step 1 is complete; UI redesign is not complete yet.
- Final direction is no longer one city = one mixed memory. It should support one city with multiple categorized memories.

City memory data structure update completed:

- `CityMemory` domain model now includes:
  - `memoryType: String = "MOMENT"`
  - `coverImageUri: String = ""`
  - `summary: String = ""`
  - `locationName: String = ""`
  - `priceText: String = ""`
  - `sortOrder: Int = 0`
- `CityMemoryEntity` / `city_memories` includes the same fields.
- Existing old fields remain for compatibility:
  - `foods`
  - `places`
  - `photoUris`
  - `linkedScheduleId`
  - `insideJoke`
  - `expenseCents`
  - `pitfallNotes`
  - `rating`
  - `note`
- Room database version was bumped from `22` to `23`.
- Added `MIGRATION_22_23`:
  - adds `memoryType`, `coverImageUri`, `summary`, `locationName`, `priceText`, `sortOrder`
  - adds indices for `memoryType` and `sortOrder`
- Repository entity/domain mappings were updated for the new fields.
- CloudBase footprint sync JSON was updated to include the new city memory fields.
- New city memory remote IDs now include local `cityMemory.id` in addition to couple/user/province/city, so multiple memories in the same city do not overwrite each other.
- Remote city memory save logic now primarily matches by `remoteId`; city-level fallback only remains for legacy remote data with no timestamps.
- Build passed after this data-layer update:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

CloudBase function note:

- Local `cloudfunctions/syncFootprints/index.js` uses a spread payload when writing:
  - `const payload = { ...item, remoteId, type, coupleId, ownerUserId, remoteUpdatedAt, updatedAt }`
- Because of this, the cloud function should transparently persist the new city memory fields without code changes.
- Re-uploading `syncFootprints` is not required if the deployed online function matches the local version.
- Re-upload `syncFootprints.zip` only if the Tencent CloudBase console has an older or manually modified version that does not use passthrough payload writing.

## Git / Memory Multi-Image / CloudBase Status - 2026-05-29

Git baseline:

- `/Users/a86153/Desktop/YanYeHome` is now a local Git repository.
- Initial commit:
  - `abd2971 Initial YanYeHome project`
- Remote repository:
  - `git@github.com:BlossomCherrelia/YanYeHome.git`
- `main` tracks `origin/main`.
- `.gitignore` excludes build/local/generated-heavy files including:
  - `.gradle/`
  - `**/build/`
  - `local.properties`
  - `.DS_Store`
  - `.Rhistory`
  - `*.hprof`
- Important: `java_pid73717.hprof` was intentionally not committed.

Memory image update:

- Regular `Memory` records now support multiple images without a database migration.
- Existing `photoUris` string is reused as newline/comma/semicolon separated URI storage.
- New/edit memory uses `MultiImagePickerField`.
- Up to 9 images are supported.
- Existing single-image memories remain compatible.
- Edit/new image picker layout:
  - square `1:1` thumbnails
  - 3 columns per row
  - all 9 images can display in the editor
  - `删除` toggles delete mode
  - delete mode shows a small `×` on each image
  - tapping `×` deletes only that single image
  - `继续添加图片 x/9` appends images instead of replacing existing ones
- Memory list/detail outside display:
  - 1 image: single cover image
  - 2 images: two square images in one row
  - 3 images: three square images in one row
  - more than 3: first three display, third image shows a small arrow hint
  - tapping any image opens a dialog preview
  - dialog preview uses horizontal swiping for up to 9 images
- Memory detail page no longer shows the `内容` heading or a content card; note text displays directly on the wallpaper background.

CloudBase / image upload status:

- Local app code calls these CloudBase endpoints through `CloudBaseConfig`:
  - `syncAnniversaries`
  - `syncWishes`
  - `syncSchedules`
  - `syncRestaurants`
  - `syncFootprints`
  - `syncCareCycles`
  - `syncMemos`
  - `syncMemories`
  - `uploadImage`
  - `registerUser`
  - `loginUser`
  - `createCoupleSpace`
  - `createInviteCode`
  - `joinCoupleSpaceByInvite`
  - `getCurrentSessionProfile`
  - `updateSessionProfile`
- Local `cloudfunctions/` contains matching function directories and zip files for all endpoints above.
- On 2026-05-29, empty POST requests were sent to the online CloudBase HTTP URLs for all endpoints above.
- All endpoints responded with function-specific validation errors such as:
  - `coupleId is required`
  - `userId is required`
  - `username is required`
- This confirms the online functions exist and are routed; they are not missing/404 placeholders.
- `uploadImage` is implemented locally and online:
  - app uploads compressed JPEG bytes to `uploadImage`
  - cloud function uses `app.uploadFile`
  - cloud function returns both `fileID` and `url`
  - app currently stores `url` first, falling back to `fileID`
- Current risk:
  - returned `url` is produced via `getTempFileURL`
  - if CloudBase storage is not configured as public readable, old stored URLs can expire and uploaded images may stop opening
- Short-term workaround:
  - configure Tencent CloudBase storage read permission as `所有用户可读`
- Safer long-term direction:
  - store permanent `fileID` in app data
  - resolve it to a fresh temp URL when displaying images
  - this avoids expired image URLs without making storage broadly public.

Test APK:

- A debug test APK was built on 2026-05-29.
- Desktop copy:
  - `/Users/a86153/Desktop/YanYeHome-test-debug.apk`
- Original build output:
  - `/Users/a86153/Desktop/YanYeHome/app/build/outputs/apk/debug/app-debug.apk`
- Approx size:
  - `48MB`
