package com.yanye.home.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yanye.home.data.local.entity.CityLightEntity
import com.yanye.home.data.local.entity.CityMemoryEntity
import com.yanye.home.data.local.entity.ProvinceLightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FootprintDao {
    @Query(
        """
        SELECT * FROM province_lights
        WHERE isDeleted = 0
        ORDER BY updatedAt DESC
        """
    )
    fun observeProvinceLights(): Flow<List<ProvinceLightEntity>>

    @Query(
        """
        SELECT * FROM province_lights
        WHERE isDeleted = 0
          AND coupleId = :coupleId
        ORDER BY updatedAt DESC
        """
    )
    fun observeProvinceLightsForCouple(coupleId: String): Flow<List<ProvinceLightEntity>>

    @Query(
        """
        SELECT * FROM city_memories
        WHERE isDeleted = 0
        ORDER BY dateEpochDay DESC, updatedAt DESC
        """
    )
    fun observeCityMemories(): Flow<List<CityMemoryEntity>>

    @Query(
        """
        SELECT * FROM city_memories
        WHERE isDeleted = 0
          AND coupleId = :coupleId
        ORDER BY dateEpochDay DESC, updatedAt DESC
        """
    )
    fun observeCityMemoriesForCouple(coupleId: String): Flow<List<CityMemoryEntity>>

    @Query(
        """
        SELECT * FROM city_lights
        WHERE isDeleted = 0
        ORDER BY updatedAt DESC
        """
    )
    fun observeCityLights(): Flow<List<CityLightEntity>>

    @Query(
        """
        SELECT * FROM city_lights
        WHERE isDeleted = 0
          AND coupleId = :coupleId
        ORDER BY updatedAt DESC
        """
    )
    fun observeCityLightsForCouple(coupleId: String): Flow<List<CityLightEntity>>

    @Query(
        """
        SELECT * FROM province_lights
        WHERE syncStatus != 'SYNCED'
           OR remoteId IS NULL
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncProvinceLights(): List<ProvinceLightEntity>

    @Query(
        """
        SELECT * FROM province_lights
        WHERE coupleId = :coupleId
          AND (syncStatus != 'SYNCED' OR remoteId IS NULL)
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncProvinceLightsForCouple(coupleId: String): List<ProvinceLightEntity>

    @Query(
        """
        SELECT * FROM city_lights
        WHERE syncStatus != 'SYNCED'
           OR remoteId IS NULL
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncCityLights(): List<CityLightEntity>

    @Query(
        """
        SELECT * FROM city_lights
        WHERE coupleId = :coupleId
          AND (syncStatus != 'SYNCED' OR remoteId IS NULL)
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncCityLightsForCouple(coupleId: String): List<CityLightEntity>

    @Query(
        """
        SELECT * FROM city_memories
        WHERE syncStatus != 'SYNCED'
           OR remoteId IS NULL
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncCityMemories(): List<CityMemoryEntity>

    @Query(
        """
        SELECT * FROM city_memories
        WHERE coupleId = :coupleId
          AND (syncStatus != 'SYNCED' OR remoteId IS NULL)
        ORDER BY updatedAt ASC
        """
    )
    suspend fun pendingSyncCityMemoriesForCouple(coupleId: String): List<CityMemoryEntity>

    @Query("SELECT * FROM province_lights WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findProvinceLightByRemoteId(remoteId: String): ProvinceLightEntity?

    @Query(
        """
        SELECT * FROM province_lights
        WHERE remoteId = :remoteId AND coupleId = :coupleId
        LIMIT 1
        """
    )
    suspend fun findProvinceLightByRemoteIdForCouple(remoteId: String, coupleId: String): ProvinceLightEntity?

    @Query(
        """
        SELECT * FROM province_lights
        WHERE provinceName = :provinceName AND coupleId = :coupleId
        LIMIT 1
        """
    )
    suspend fun findProvinceLightByName(
        provinceName: String,
        coupleId: String
    ): ProvinceLightEntity?

    @Query("SELECT * FROM city_lights WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findCityLightByRemoteId(remoteId: String): CityLightEntity?

    @Query(
        """
        SELECT * FROM city_lights
        WHERE remoteId = :remoteId AND coupleId = :coupleId
        LIMIT 1
        """
    )
    suspend fun findCityLightByRemoteIdForCouple(remoteId: String, coupleId: String): CityLightEntity?

    @Query(
        """
        SELECT * FROM city_lights
        WHERE provinceName = :provinceName AND cityName = :cityName AND coupleId = :coupleId
        LIMIT 1
        """
    )
    suspend fun findCityLightByName(
        provinceName: String,
        cityName: String,
        coupleId: String
    ): CityLightEntity?

    @Query("SELECT * FROM city_memories WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findCityMemoryByRemoteId(remoteId: String): CityMemoryEntity?

    @Query(
        """
        SELECT * FROM city_memories
        WHERE remoteId = :remoteId AND coupleId = :coupleId
        LIMIT 1
        """
    )
    suspend fun findCityMemoryByRemoteIdForCouple(remoteId: String, coupleId: String): CityMemoryEntity?

    @Query(
        """
        SELECT * FROM city_memories
        WHERE provinceName = :provinceName AND cityName = :cityName AND coupleId = :coupleId
        ORDER BY updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun findCityMemoryByCity(
        provinceName: String,
        cityName: String,
        coupleId: String
    ): CityMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvinceLight(provinceLight: ProvinceLightEntity): Long

    @Update
    suspend fun updateProvinceLight(provinceLight: ProvinceLightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCityMemory(cityMemory: CityMemoryEntity): Long

    @Update
    suspend fun updateCityMemory(cityMemory: CityMemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCityLight(cityLight: CityLightEntity): Long

    @Update
    suspend fun updateCityLight(cityLight: CityLightEntity)

    @Query(
        """
        UPDATE province_lights
        SET isDeleted = 1,
            syncStatus = 'PENDING_DELETE',
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDeleteProvinceLight(id: Long, updatedAt: Long)

    @Query(
        """
        UPDATE city_memories
        SET isDeleted = 1,
            syncStatus = 'PENDING_DELETE',
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDeleteCityMemory(id: Long, updatedAt: Long)

    @Query(
        """
        UPDATE city_lights
        SET isDeleted = 1,
            syncStatus = 'PENDING_DELETE',
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDeleteCityLight(id: Long, updatedAt: Long)

    @Query(
        """
        UPDATE province_lights
        SET remoteId = :remoteId,
            coupleId = :coupleId,
            ownerUserId = :ownerUserId,
            syncStatus = 'SYNCED',
            remoteUpdatedAt = :remoteUpdatedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markProvinceLightSynced(
        id: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE city_lights
        SET remoteId = :remoteId,
            coupleId = :coupleId,
            ownerUserId = :ownerUserId,
            syncStatus = 'SYNCED',
            remoteUpdatedAt = :remoteUpdatedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markCityLightSynced(
        id: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE city_memories
        SET remoteId = :remoteId,
            coupleId = :coupleId,
            ownerUserId = :ownerUserId,
            syncStatus = 'SYNCED',
            remoteUpdatedAt = :remoteUpdatedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markCityMemorySynced(
        id: Long,
        remoteId: String,
        coupleId: String,
        ownerUserId: String,
        remoteUpdatedAt: Long,
        updatedAt: Long
    )
}
