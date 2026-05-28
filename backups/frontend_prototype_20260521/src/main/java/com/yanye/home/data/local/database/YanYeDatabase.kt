package com.yanye.home.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yanye.home.data.local.dao.AnniversaryDao
import com.yanye.home.data.local.dao.FootprintDao
import com.yanye.home.data.local.dao.MemoryDao
import com.yanye.home.data.local.dao.MemoDao
import com.yanye.home.data.local.dao.RestaurantDao
import com.yanye.home.data.local.dao.ScheduleDao
import com.yanye.home.data.local.dao.WishDao
import com.yanye.home.data.local.entity.AnniversaryEntity
import com.yanye.home.data.local.entity.CareCycleEntity
import com.yanye.home.data.local.entity.CityLightEntity
import com.yanye.home.data.local.entity.CityMemoryEntity
import com.yanye.home.data.local.entity.MemoryEntity
import com.yanye.home.data.local.entity.MemoEntity
import com.yanye.home.data.local.entity.ProvinceLightEntity
import com.yanye.home.data.local.entity.RestaurantEntity
import com.yanye.home.data.local.entity.ScheduleEntity
import com.yanye.home.data.local.entity.WishEntity

@Database(
    entities = [
        AnniversaryEntity::class,
        WishEntity::class,
        ScheduleEntity::class,
        MemoryEntity::class,
        RestaurantEntity::class,
        MemoEntity::class,
        CareCycleEntity::class,
        ProvinceLightEntity::class,
        CityMemoryEntity::class,
        CityLightEntity::class
    ],
    version = 16,
    exportSchema = true
)
abstract class YanYeDatabase : RoomDatabase() {
    abstract fun anniversaryDao(): AnniversaryDao
    abstract fun wishDao(): WishDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun memoryDao(): MemoryDao
    abstract fun restaurantDao(): RestaurantDao
    abstract fun memoDao(): MemoDao
    abstract fun footprintDao(): FootprintDao

    companion object {
        const val DATABASE_NAME = "yanye_home.db"
    }
}
