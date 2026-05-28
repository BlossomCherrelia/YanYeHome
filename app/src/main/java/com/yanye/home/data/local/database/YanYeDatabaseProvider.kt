package com.yanye.home.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yanye.home.data.repository.AnniversaryRepository
import com.yanye.home.data.repository.FootprintRepository
import com.yanye.home.data.repository.MemoRepository
import com.yanye.home.data.repository.RestaurantRepository
import com.yanye.home.data.repository.ScheduleRepository
import com.yanye.home.data.repository.WishRepository
import com.yanye.home.data.sync.SyncSettings

object YanYeDatabaseProvider {
    @Volatile
    private var database: YanYeDatabase? = null

    fun database(context: Context): YanYeDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                YanYeDatabase::class.java,
                YanYeDatabase.DATABASE_NAME
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23
                )
                .build()
                .also { database = it }
        }

    fun anniversaryRepository(context: Context): AnniversaryRepository =
        AnniversaryRepository(
            anniversaryDao = database(context).anniversaryDao(),
            syncSettings = SyncSettings(context)
        )

    fun wishRepository(context: Context): WishRepository =
        WishRepository(
            wishDao = database(context).wishDao(),
            syncSettings = SyncSettings(context)
        )

    fun scheduleRepository(context: Context): ScheduleRepository {
        val database = database(context)
        return ScheduleRepository(
            scheduleDao = database.scheduleDao(),
            wishDao = database.wishDao(),
            memoryDao = database.memoryDao(),
            syncSettings = SyncSettings(context)
        )
    }

    fun restaurantRepository(context: Context): RestaurantRepository =
        RestaurantRepository(
            restaurantDao = database(context).restaurantDao(),
            syncSettings = SyncSettings(context)
        )

    fun memoRepository(context: Context): MemoRepository =
        MemoRepository(
            memoDao = database(context).memoDao(),
            syncSettings = SyncSettings(context)
        )

    fun footprintRepository(context: Context): FootprintRepository =
        FootprintRepository(
            footprintDao = database(context).footprintDao(),
            syncSettings = SyncSettings(context)
        )

    private val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE city_memories ADD COLUMN memoryType TEXT NOT NULL DEFAULT 'MOMENT'")
            db.execSQL("ALTER TABLE city_memories ADD COLUMN coverImageUri TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE city_memories ADD COLUMN summary TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE city_memories ADD COLUMN locationName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE city_memories ADD COLUMN priceText TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE city_memories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_memories_memoryType ON city_memories (memoryType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_memories_sortOrder ON city_memories (sortOrder)")
        }
    }

    private val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_province_lights_provinceName")
            db.execSQL("DROP INDEX IF EXISTS index_city_lights_provinceName_cityName")
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_province_lights_coupleId_provinceName
                ON province_lights (coupleId, provinceName)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_city_lights_coupleId_provinceName_cityName
                ON city_lights (coupleId, provinceName, cityName)
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE memories ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE memories ADD COLUMN coupleId TEXT")
            db.execSQL("ALTER TABLE memories ADD COLUMN ownerUserId TEXT")
            db.execSQL("ALTER TABLE memories ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE memories ADD COLUMN remoteUpdatedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_remoteId ON memories (remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_syncStatus ON memories (syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_coupleId ON memories (coupleId)")
        }
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE anniversaries ADD COLUMN displayMode TEXT NOT NULL DEFAULT 'ANNIVERSARY'")
            db.execSQL("ALTER TABLE anniversaries ADD COLUMN coverImageUri TEXT")
            db.execSQL("ALTER TABLE anniversaries ADD COLUMN giftWishLinkEnabled INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE anniversaries ADD COLUMN celebrationArchiveEnabled INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS wishes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    visibility TEXT NOT NULL,
                    revealAfterEpochDay INTEGER,
                    budgetCents INTEGER,
                    locationName TEXT NOT NULL,
                    priority TEXT NOT NULL,
                    targetDateEpochDay INTEGER,
                    note TEXT NOT NULL,
                    preparationItems TEXT NOT NULL,
                    coverImageUri TEXT,
                    isCompleted INTEGER NOT NULL,
                    scheduleReady INTEGER NOT NULL,
                    linkedScheduleId INTEGER,
                    giftCandidateForAnniversaryId INTEGER,
                    createdBy TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    sharedWithPartner INTEGER NOT NULL,
                    isDeleted INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_wishes_category ON wishes (category)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_wishes_visibility ON wishes (visibility)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_wishes_targetDateEpochDay ON wishes (targetDateEpochDay)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_wishes_isCompleted ON wishes (isCompleted)")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS schedules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    startEpochDay INTEGER NOT NULL,
                    startMinuteOfDay INTEGER NOT NULL,
                    endMinuteOfDay INTEGER,
                    locationName TEXT NOT NULL,
                    reminderMinutesBefore INTEGER NOT NULL,
                    budgetCents INTEGER,
                    participants TEXT NOT NULL,
                    linkedWishId INTEGER,
                    isGuideMode INTEGER NOT NULL,
                    guideRestaurants TEXT NOT NULL,
                    guideActivities TEXT NOT NULL,
                    guideRoute TEXT NOT NULL,
                    backupPlan TEXT NOT NULL,
                    note TEXT NOT NULL,
                    isCompleted INTEGER NOT NULL,
                    memoryId INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    visibility TEXT NOT NULL,
                    sharedWithPartner INTEGER NOT NULL,
                    isDeleted INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_schedules_startEpochDay ON schedules (startEpochDay)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_schedules_linkedWishId ON schedules (linkedWishId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_schedules_isCompleted ON schedules (isCompleted)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS memories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    dateEpochDay INTEGER NOT NULL,
                    scheduleId INTEGER,
                    linkedWishId INTEGER,
                    locationName TEXT NOT NULL,
                    photoUris TEXT NOT NULL,
                    foodNotes TEXT NOT NULL,
                    expenseCents INTEGER,
                    mood TEXT NOT NULL,
                    note TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    isDeleted INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_dateEpochDay ON memories (dateEpochDay)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_scheduleId ON memories (scheduleId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_linkedWishId ON memories (linkedWishId)")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS restaurants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    cuisine TEXT NOT NULL,
                    tags TEXT NOT NULL,
                    avoidNotes TEXT NOT NULL,
                    priceLevel TEXT NOT NULL,
                    address TEXT NOT NULL,
                    cityName TEXT NOT NULL,
                    canTakeout INTEGER NOT NULL,
                    canDineIn INTEGER NOT NULL,
                    isPitfall INTEGER NOT NULL,
                    rating INTEGER NOT NULL,
                    note TEXT NOT NULL,
                    lastPickedAt INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    isDeleted INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_restaurants_cuisine ON restaurants (cuisine)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_restaurants_cityName ON restaurants (cityName)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_restaurants_isPitfall ON restaurants (isPitfall)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_restaurants_lastPickedAt ON restaurants (lastPickedAt)")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS memos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    category TEXT NOT NULL,
                    checklistItems TEXT NOT NULL,
                    imageUris TEXT NOT NULL,
                    reminderAtMillis INTEGER,
                    reminderEnabled INTEGER NOT NULL,
                    notificationChannelKey TEXT NOT NULL,
                    linkedScheduleId INTEGER,
                    visibility TEXT NOT NULL,
                    sharedWithPartner INTEGER NOT NULL,
                    isCompleted INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    isDeleted INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memos_category ON memos (category)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memos_visibility ON memos (visibility)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memos_reminderAtMillis ON memos (reminderAtMillis)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memos_isCompleted ON memos (isCompleted)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memos_linkedScheduleId ON memos (linkedScheduleId)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS care_cycles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    startEpochDay INTEGER NOT NULL,
                    endEpochDay INTEGER,
                    cycleLengthDays INTEGER NOT NULL,
                    painLevel TEXT NOT NULL,
                    mood TEXT NOT NULL,
                    avoidNotes TEXT NOT NULL,
                    carePreference TEXT NOT NULL,
                    shareReminderWithPartner INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    isDeleted INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_care_cycles_startEpochDay ON care_cycles (startEpochDay)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_care_cycles_shareReminderWithPartner ON care_cycles (shareReminderWithPartner)")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS province_lights (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    provinceName TEXT NOT NULL,
                    isLit INTEGER NOT NULL,
                    fillColorArgb INTEGER NOT NULL DEFAULT -34150,
                    note TEXT NOT NULL,
                    linkedScheduleId INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    isDeleted INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_province_lights_provinceName ON province_lights (provinceName)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_province_lights_isLit ON province_lights (isLit)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS city_memories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    provinceName TEXT NOT NULL,
                    cityName TEXT NOT NULL,
                    title TEXT NOT NULL,
                    dateEpochDay INTEGER NOT NULL,
                    foods TEXT NOT NULL,
                    places TEXT NOT NULL,
                    photoUris TEXT NOT NULL,
                    linkedScheduleId INTEGER,
                    insideJoke TEXT NOT NULL,
                    expenseCents INTEGER,
                    pitfallNotes TEXT NOT NULL,
                    rating INTEGER NOT NULL,
                    note TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    isDeleted INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_memories_provinceName ON city_memories (provinceName)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_memories_cityName ON city_memories (cityName)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_memories_dateEpochDay ON city_memories (dateEpochDay)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_memories_linkedScheduleId ON city_memories (linkedScheduleId)")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE province_lights ADD COLUMN fillColorArgb INTEGER NOT NULL DEFAULT -34150")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS city_lights (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    provinceName TEXT NOT NULL,
                    cityName TEXT NOT NULL,
                    isLit INTEGER NOT NULL,
                    fillColorArgb INTEGER NOT NULL,
                    note TEXT NOT NULL,
                    linkedScheduleId INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    isDeleted INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_lights_provinceName ON city_lights (provinceName)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_lights_cityName ON city_lights (cityName)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_city_lights_provinceName_cityName ON city_lights (provinceName, cityName)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_lights_isLit ON city_lights (isLit)")
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM province_lights")
            db.execSQL("DELETE FROM city_lights")
            db.execSQL("DELETE FROM city_memories")
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE anniversaries ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE anniversaries ADD COLUMN coupleId TEXT")
            db.execSQL("ALTER TABLE anniversaries ADD COLUMN ownerUserId TEXT")
            db.execSQL("ALTER TABLE anniversaries ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE anniversaries ADD COLUMN remoteUpdatedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_anniversaries_remoteId ON anniversaries (remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_anniversaries_syncStatus ON anniversaries (syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_anniversaries_coupleId ON anniversaries (coupleId)")
        }
    }

    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE wishes ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE wishes ADD COLUMN coupleId TEXT")
            db.execSQL("ALTER TABLE wishes ADD COLUMN ownerUserId TEXT")
            db.execSQL("ALTER TABLE wishes ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE wishes ADD COLUMN remoteUpdatedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_wishes_remoteId ON wishes (remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_wishes_syncStatus ON wishes (syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_wishes_coupleId ON wishes (coupleId)")
        }
    }

    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE schedules ADD COLUMN linkedWishRemoteId TEXT")
            db.execSQL("ALTER TABLE schedules ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE schedules ADD COLUMN coupleId TEXT")
            db.execSQL("ALTER TABLE schedules ADD COLUMN ownerUserId TEXT")
            db.execSQL("ALTER TABLE schedules ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE schedules ADD COLUMN remoteUpdatedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_schedules_linkedWishRemoteId ON schedules (linkedWishRemoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_schedules_remoteId ON schedules (remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_schedules_syncStatus ON schedules (syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_schedules_coupleId ON schedules (coupleId)")
        }
    }

    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE restaurants ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE restaurants ADD COLUMN coupleId TEXT")
            db.execSQL("ALTER TABLE restaurants ADD COLUMN ownerUserId TEXT")
            db.execSQL("ALTER TABLE restaurants ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE restaurants ADD COLUMN remoteUpdatedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_restaurants_remoteId ON restaurants (remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_restaurants_syncStatus ON restaurants (syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_restaurants_coupleId ON restaurants (coupleId)")
        }
    }

    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE province_lights ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE province_lights ADD COLUMN coupleId TEXT")
            db.execSQL("ALTER TABLE province_lights ADD COLUMN ownerUserId TEXT")
            db.execSQL("ALTER TABLE province_lights ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE province_lights ADD COLUMN remoteUpdatedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_province_lights_remoteId ON province_lights (remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_province_lights_syncStatus ON province_lights (syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_province_lights_coupleId ON province_lights (coupleId)")

            db.execSQL("ALTER TABLE city_lights ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE city_lights ADD COLUMN coupleId TEXT")
            db.execSQL("ALTER TABLE city_lights ADD COLUMN ownerUserId TEXT")
            db.execSQL("ALTER TABLE city_lights ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE city_lights ADD COLUMN remoteUpdatedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_lights_remoteId ON city_lights (remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_lights_syncStatus ON city_lights (syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_lights_coupleId ON city_lights (coupleId)")

            db.execSQL("ALTER TABLE city_memories ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE city_memories ADD COLUMN coupleId TEXT")
            db.execSQL("ALTER TABLE city_memories ADD COLUMN ownerUserId TEXT")
            db.execSQL("ALTER TABLE city_memories ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE city_memories ADD COLUMN remoteUpdatedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_memories_remoteId ON city_memories (remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_memories_syncStatus ON city_memories (syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_city_memories_coupleId ON city_memories (coupleId)")
        }
    }

    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE care_cycles ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE care_cycles ADD COLUMN coupleId TEXT")
            db.execSQL("ALTER TABLE care_cycles ADD COLUMN ownerUserId TEXT")
            db.execSQL("ALTER TABLE care_cycles ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE care_cycles ADD COLUMN remoteUpdatedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_care_cycles_remoteId ON care_cycles (remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_care_cycles_syncStatus ON care_cycles (syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_care_cycles_coupleId ON care_cycles (coupleId)")
        }
    }

    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE anniversaries ADD COLUMN showOnHome INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE anniversaries ADD COLUMN homeSortOrder INTEGER NOT NULL DEFAULT 100")
            db.execSQL(
                """
                UPDATE anniversaries
                SET showOnHome = 1,
                    homeSortOrder = 0
                WHERE isDeleted = 0
                  AND type = 'RELATIONSHIP'
                  AND displayMode = 'COUNT_UP'
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE memos ADD COLUMN showOnHome INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE memos ADD COLUMN homeSortOrder INTEGER NOT NULL DEFAULT 100")
        }
    }

    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_memos_showOnHome")
            db.execSQL("ALTER TABLE memos ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE memos ADD COLUMN coupleId TEXT")
            db.execSQL("ALTER TABLE memos ADD COLUMN ownerUserId TEXT")
            db.execSQL("ALTER TABLE memos ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE memos ADD COLUMN remoteUpdatedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memos_remoteId ON memos (remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memos_syncStatus ON memos (syncStatus)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_memos_coupleId ON memos (coupleId)")
        }
    }

    private val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE memos ADD COLUMN dueLabel TEXT NOT NULL DEFAULT ''")
        }
    }
}
